package cleaner;

import logging.Log;
import models.Bag;
import models.Item;
import models.MailItem;
import models.VMangosDB;

import java.sql.*;
import java.util.*;

public class ItemGuidSortTask {

    private static final String REMAP_TABLE = "_item_guid_remap";
    private static final int    BATCH_SIZE  = 1000;

    /**
     * Loads all required data from the DB once, computes both the
     * "shift to above highestItemGuid" mapping and the "compact to 1" mapping
     * in RAM, then applies them in two sequential bulk operations.
     */
    public void run(Connection connection, int lowestCharacterGuid, int highestCharacterGuid, int highestItemGuid) throws SQLException {
        Log.print("--> ItemGuidSortTask starting");

        // Single DB read — shared by both passes
        Map<Integer, List<Item>>     allItemsByChar    = loadAllItemsByCharacter(connection, lowestCharacterGuid, highestCharacterGuid);
        // Filter mail items to the same character range to avoid touching items belonging to other characters
        Map<Integer, List<MailItem>> mailItemsByChar   = loadMailItemsByCharacter(connection, lowestCharacterGuid, highestCharacterGuid);
        Map<Integer, List<Bag>>      bagsByChar        = deriveBags(allItemsByChar);
        Map<Integer, List<Item>>     nonBagItemsByChar = deriveNonBagItems(allItemsByChar, bagsByChar);
        List<Integer>                charGuids         = getExistingCharacterGuids(connection, lowestCharacterGuid, highestCharacterGuid);

        Log.printLine();
        Log.print("Actual characters in database: " + charGuids.size());
        Log.print("Total items: " + allItemsByChar.values().stream().mapToInt(List::size).sum());
        Log.print("Mail items: " + mailItemsByChar.values().stream().mapToInt(List::size).sum());
        Log.print("Bags: " + bagsByChar.values().stream().mapToInt(List::size).sum());
        Log.print("Items without bags: " + nonBagItemsByChar.values().stream().mapToInt(List::size).sum());
        Log.printLine();

        // Pass 1: shift all existing GUIDs above highestItemGuid to avoid collisions during pass 2
        Log.print("Pass 1: computing shift mapping (start guid: " + (highestItemGuid + 1) + ")...");
        Map<Integer, Integer> shiftMapping = buildMapping(charGuids, bagsByChar, nonBagItemsByChar, mailItemsByChar, highestItemGuid + 1);
        applyGuidRemapping(connection, shiftMapping);

        Log.printLine();
        Log.print("Removing all items which belong to no character...");
        deleteOrphanedItems(connection, highestItemGuid);
        Log.print("Done.");
        Log.printLine();

        // Pass 2: compact all GUIDs down to start from 1
        Log.print("Pass 2: computing compact mapping (start guid: 1)...");
        Map<Integer, Integer> compactMapping = buildMapping(charGuids, bagsByChar, nonBagItemsByChar, mailItemsByChar, 1);
        // Remap from the shifted GUIDs (values of shiftMapping) to the compact GUIDs
        Map<Integer, Integer> shiftToCompact = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer> entry : shiftMapping.entrySet()) {
            Integer compactGuid = compactMapping.get(entry.getKey());
            if (compactGuid != null)
                shiftToCompact.put(entry.getValue(), compactGuid);
        }
        applyGuidRemapping(connection, shiftToCompact);

        int lastGuid = shiftToCompact.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        Log.print("Done. New highest item guid in database: " + lastGuid);
    }

    /**
     * Builds an old→new guid mapping by iterating characters in order.
     * Bags first, then regular items, then mail items — same order the server expects.
     */
    private Map<Integer, Integer> buildMapping(
            List<Integer> charGuids,
            Map<Integer, List<Bag>> bagsByChar,
            Map<Integer, List<Item>> nonBagItemsByChar,
            Map<Integer, List<MailItem>> mailItemsByChar,
            int startGuid) {
        Map<Integer, Integer> mapping = new LinkedHashMap<>();
        int currentGuid = startGuid;
        for (int charGuid : charGuids) {
            for (Bag bag : bagsByChar.getOrDefault(charGuid, Collections.emptyList()))
                mapping.put(bag.getItemGuid(), currentGuid++);
            for (Item item : nonBagItemsByChar.getOrDefault(charGuid, Collections.emptyList()))
                mapping.put(item.getItemGuid(), currentGuid++);
            for (MailItem mail : mailItemsByChar.getOrDefault(charGuid, Collections.emptyList()))
                mapping.put(mail.getItemGuid(), currentGuid++);
        }
        return mapping;
    }

    /**
     * Bulk-applies the old→new guid mapping via a temp table + 4 JOIN UPDATEs.
     * Uses DELETE FROM instead of TRUNCATE to avoid implicit commits in MySQL 5.x.
     */
    private void applyGuidRemapping(Connection connection, Map<Integer, Integer> guidMapping) throws SQLException {
        if (guidMapping.isEmpty()) {
            Log.print("No items to remap.");
            return;
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                    "CREATE TEMPORARY TABLE IF NOT EXISTS `" + REMAP_TABLE + "` (" +
                    "  `old_guid` INT UNSIGNED NOT NULL," +
                    "  `new_guid` INT UNSIGNED NOT NULL," +
                    "  PRIMARY KEY (`old_guid`)" +
                    ") ENGINE=InnoDB");
            // DELETE FROM instead of TRUNCATE — TRUNCATE causes an implicit commit in MySQL 5.x
            stmt.executeUpdate("DELETE FROM `" + REMAP_TABLE + "`");

            Log.print("Inserting " + guidMapping.size() + " mapping rows into temp table...");
            try (PreparedStatement insertMapping = connection.prepareStatement(
                    "INSERT INTO `" + REMAP_TABLE + "` (`old_guid`, `new_guid`) VALUES (?, ?)")) {
                int batchCount = 0;
                for (Map.Entry<Integer, Integer> entry : guidMapping.entrySet()) {
                    insertMapping.setInt(1, entry.getKey());
                    insertMapping.setInt(2, entry.getValue());
                    insertMapping.addBatch();
                    if (++batchCount % BATCH_SIZE == 0)
                        insertMapping.executeBatch();
                }
                insertMapping.executeBatch();
            }

            Log.print("Updating item_instance...");
            stmt.executeUpdate(
                    "UPDATE `" + VMangosDB.item_instance.TABLE_NAME + "` i" +
                    " JOIN `" + REMAP_TABLE + "` r ON i.`" + VMangosDB.item_instance.GUID + "` = r.`old_guid`" +
                    " SET i.`" + VMangosDB.item_instance.GUID + "` = r.`new_guid`");

            Log.print("Updating character_inventory (item_guid)...");
            stmt.executeUpdate(
                    "UPDATE `" + VMangosDB.character_inventory.TABLE_NAME + "` ci" +
                    " JOIN `" + REMAP_TABLE + "` r ON ci.`" + VMangosDB.character_inventory.ITEM_GUID + "` = r.`old_guid`" +
                    " SET ci.`" + VMangosDB.character_inventory.ITEM_GUID + "` = r.`new_guid`");

            Log.print("Updating character_inventory (bag)...");
            stmt.executeUpdate(
                    "UPDATE `" + VMangosDB.character_inventory.TABLE_NAME + "` ci" +
                    " JOIN `" + REMAP_TABLE + "` r ON ci.`" + VMangosDB.character_inventory.BAG + "` = r.`old_guid`" +
                    " SET ci.`" + VMangosDB.character_inventory.BAG + "` = r.`new_guid`");

            Log.print("Updating mail_items...");
            stmt.executeUpdate(
                    "UPDATE `" + VMangosDB.mail_items.TABLE_NAME + "` mi" +
                    " JOIN `" + REMAP_TABLE + "` r ON mi.`" + VMangosDB.mail_items.ITEM_GUID + "` = r.`old_guid`" +
                    " SET mi.`" + VMangosDB.mail_items.ITEM_GUID + "` = r.`new_guid`");

            Log.print("Updating auction...");
            stmt.executeUpdate(
                    "UPDATE `" + VMangosDB.auction.TABLE_NAME + "` a" +
                    " JOIN `" + REMAP_TABLE + "` r ON a.`" + VMangosDB.auction.ITEM_GUID + "` = r.`old_guid`" +
                    " SET a.`" + VMangosDB.auction.ITEM_GUID + "` = r.`new_guid`");

            Log.print("Updating character_gifts...");
            stmt.executeUpdate(
                    "UPDATE `" + VMangosDB.character_gifts.TABLE_NAME + "` cg" +
                    " JOIN `" + REMAP_TABLE + "` r ON cg.`" + VMangosDB.character_gifts.ITEM_GUID + "` = r.`old_guid`" +
                    " SET cg.`" + VMangosDB.character_gifts.ITEM_GUID + "` = r.`new_guid`");

            stmt.executeUpdate("DROP TEMPORARY TABLE IF EXISTS `" + REMAP_TABLE + "`");
        }
    }

    private void deleteOrphanedItems(Connection connection, int highestItemGuid) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                    "DELETE FROM `" + VMangosDB.item_instance.TABLE_NAME + "`" +
                    " WHERE `" + VMangosDB.item_instance.GUID + "` <= " + highestItemGuid);
        }
    }

    private List<Integer> getExistingCharacterGuids(Connection connection, int lowestCharacterGuid, int highestCharacterGuid) throws SQLException {
        List<Integer> guids = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT `" + VMangosDB.characters.GUID + "` FROM `" + VMangosDB.characters.TABLE_NAME + "`" +
                " WHERE `" + VMangosDB.characters.GUID + "` >= ? AND `" + VMangosDB.characters.GUID + "` <= ?" +
                " ORDER BY `" + VMangosDB.characters.GUID + "`")) {
            ps.setInt(1, lowestCharacterGuid);
            ps.setInt(2, highestCharacterGuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    guids.add(rs.getInt(1));
            }
        }
        return guids;
    }

    private Map<Integer, List<Item>> loadAllItemsByCharacter(Connection connection, int lowestCharacterGuid, int highestCharacterGuid) throws SQLException {
        Map<Integer, List<Item>> map = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT `" + VMangosDB.character_inventory.GUID + "`," +
                "       `" + VMangosDB.character_inventory.BAG + "`," +
                "       `" + VMangosDB.character_inventory.ITEM_GUID + "`" +
                " FROM `" + VMangosDB.character_inventory.TABLE_NAME + "`" +
                " WHERE `" + VMangosDB.character_inventory.GUID + "` >= ? AND `" + VMangosDB.character_inventory.GUID + "` <= ?")) {
            ps.setInt(1, lowestCharacterGuid);
            ps.setInt(2, highestCharacterGuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int charGuid = rs.getInt(1);
                    int bagGuid  = rs.getInt(2);
                    int itemGuid = rs.getInt(3);
                    map.computeIfAbsent(charGuid, k -> new ArrayList<>()).add(new Item(charGuid, bagGuid, itemGuid));
                }
            }
        }
        return map;
    }

    private Map<Integer, List<MailItem>> loadMailItemsByCharacter(Connection connection, int lowestCharacterGuid, int highestCharacterGuid) throws SQLException {
        Map<Integer, List<MailItem>> map = new HashMap<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT `" + VMangosDB.mail_items.ITEM_GUID + "`, `" + VMangosDB.mail_items.RECEIVER_GUID + "`" +
                " FROM `" + VMangosDB.mail_items.TABLE_NAME + "`" +
                " WHERE `" + VMangosDB.mail_items.RECEIVER_GUID + "` >= ? AND `" + VMangosDB.mail_items.RECEIVER_GUID + "` <= ?")) {
            ps.setInt(1, lowestCharacterGuid);
            ps.setInt(2, highestCharacterGuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int itemGuid = rs.getInt(1);
                    int owner    = rs.getInt(2);
                    map.computeIfAbsent(owner, k -> new ArrayList<>()).add(new MailItem(itemGuid, owner));
                }
            }
        }
        return map;
    }

    private Map<Integer, List<Bag>> deriveBags(Map<Integer, List<Item>> allItemsByChar) {
        Map<Integer, List<Bag>> bags = new HashMap<>();
        Set<Integer> seen = new HashSet<>();
        for (Map.Entry<Integer, List<Item>> entry : allItemsByChar.entrySet()) {
            int charGuid = entry.getKey();
            for (Item item : entry.getValue()) {
                if (item.getBagGuid() != 0 && seen.add(item.getBagGuid()))
                    bags.computeIfAbsent(charGuid, k -> new ArrayList<>()).add(new Bag(item.getBagGuid(), charGuid));
            }
        }
        return bags;
    }

    private Map<Integer, List<Item>> deriveNonBagItems(Map<Integer, List<Item>> allItemsByChar, Map<Integer, List<Bag>> bagsByChar) {
        Set<Integer> bagItemGuids = new HashSet<>();
        for (List<Bag> list : bagsByChar.values())
            for (Bag bag : list)
                bagItemGuids.add(bag.getItemGuid());

        Map<Integer, List<Item>> nonBag = new HashMap<>();
        for (Map.Entry<Integer, List<Item>> entry : allItemsByChar.entrySet()) {
            int charGuid = entry.getKey();
            for (Item item : entry.getValue()) {
                if (!bagItemGuids.contains(item.getItemGuid()))
                    nonBag.computeIfAbsent(charGuid, k -> new ArrayList<>()).add(item);
            }
        }
        return nonBag;
    }
}
