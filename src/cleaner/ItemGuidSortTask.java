package cleaner;

import logging.Log;
import models.Bag;
import models.Item;
import models.MailItem;
import models.VMangosDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ItemGuidSortTask {

    public void run(Connection connection, int lowestCharacterGuid, int highestCharacterGuid, int newItemStartGuid, boolean printStats) throws SQLException {
        Log.print("--> New ItemGuidSortTask - start guid: " + newItemStartGuid);

        List<Item> allItems = getAllItems(connection, lowestCharacterGuid, highestCharacterGuid);
        List<MailItem> mailItems = getMailItems(connection);
        // Filter fetched results
        List<Bag> bags = getBags(allItems);
        List<Item> itemsWithoutBags = getItemsWithoutBags(allItems, bags);

        int totalCharacters = getActualCharacterCount(connection, lowestCharacterGuid, highestCharacterGuid);

        if (printStats) // Only on first iteration
        {
            Log.printLine();
            Log.print("Actual characters in database: " + totalCharacters);
            Log.print("Total items: " + allItems.size());
            Log.print("Mail items: " + mailItems.size());
            Log.print("Bags: " + bags.size());
            Log.print("Items without bags: " + itemsWithoutBags.size());
            Log.printLine();
        }

        int currentGuid = newItemStartGuid;
        int processedCharacters = 0;
        int lastLoggedPercent = -1;

        // Get list of existing character GUIDs instead of iterating through all possible GUIDs
        List<Integer> existingCharacterGuids = getExistingCharacterGuids(connection, lowestCharacterGuid, highestCharacterGuid);
        for (int charGuid : existingCharacterGuids) {
            currentGuid = sortBagsForCharacter(connection, charGuid, bags, currentGuid);
            currentGuid = sortItemsForCharacter(connection, charGuid, itemsWithoutBags, currentGuid);
            currentGuid = sortMailItemsForCharacter(connection, charGuid, mailItems, currentGuid);

            processedCharacters++;

            // Calculate percentage and log only when it changes
            int currentPercent = (processedCharacters * 100) / totalCharacters;
            if (currentPercent > lastLoggedPercent) {
                Log.print("Progress: " + currentPercent + "% (" + processedCharacters + "/" + totalCharacters + " characters)");
                lastLoggedPercent = currentPercent;
            }
        }

        Log.print("Done. New highest item guid in database: " + (currentGuid - 1));
    }

    /**
     * Get the actual count of characters in the specified GUID range
     * @param connection database connection
     * @param lowestCharacterGuid min character guid
     * @param highestCharacterGuid max character guid
     * @return actual number of existing characters in the range
     */
    private int getActualCharacterCount(Connection connection, int lowestCharacterGuid, int highestCharacterGuid) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT COUNT(*) FROM `" + VMangosDB.characters.TABLE_NAME + "` WHERE `" + VMangosDB.characters.GUID + "` >= ? AND `" + VMangosDB.characters.GUID + "` <= ?");
        preparedStatement.setInt(1, lowestCharacterGuid);
        preparedStatement.setInt(2, highestCharacterGuid);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            return resultSet.getInt(1);
        } else {
            return 0;
        }
    }

    /**
     * Get list of existing character GUIDs in the specified range
     * @param connection database connection
     * @param lowestCharacterGuid min character guid
     * @param highestCharacterGuid max character guid
     * @return list of existing character GUIDs
     */
    private List<Integer> getExistingCharacterGuids(Connection connection, int lowestCharacterGuid, int highestCharacterGuid) throws SQLException {
        List<Integer> characterGuids = new ArrayList<>();
        PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT `" + VMangosDB.characters.GUID + "` FROM `" + VMangosDB.characters.TABLE_NAME + "` WHERE `" + VMangosDB.characters.GUID + "` >= ? AND `" + VMangosDB.characters.GUID + "` <= ? ORDER BY `" + VMangosDB.characters.GUID + "`");
        preparedStatement.setInt(1, lowestCharacterGuid);
        preparedStatement.setInt(2, highestCharacterGuid);
        ResultSet resultSet = preparedStatement.executeQuery();

        while (resultSet.next()) {
            characterGuids.add(resultSet.getInt(1));
        }
        return characterGuids;
    }

    /**
     * @param lowestCharacterGuid  min character guid to fetch data
     * @param highestCharacterGuid max character guid to fetch data
     * @return list containing all items from character_inventory within desired character guid range
     */
    private List<Item> getAllItems(Connection connection, int lowestCharacterGuid, int highestCharacterGuid) throws SQLException {
        List<Item> items = new ArrayList<>();
        // create prepared statement
        PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT * FROM `" + VMangosDB.character_inventory.TABLE_NAME + "` WHERE `" + VMangosDB.character_inventory.GUID + "` >= ? AND `" + VMangosDB.character_inventory.GUID + "` <= ?");
        preparedStatement.setInt(1, lowestCharacterGuid);
        preparedStatement.setInt(2, highestCharacterGuid);
        ResultSet resultSet = preparedStatement.executeQuery();
        // process the result set
        while (resultSet.next()) {
            int charGuid = resultSet.getInt(VMangosDB.character_inventory.GUID);
            int bagGuid = resultSet.getInt(VMangosDB.character_inventory.BAG);
            int itemGuid = resultSet.getInt(VMangosDB.character_inventory.ITEM_GUID);
            Item item = new Item(charGuid, bagGuid, itemGuid);
            items.add(item);
        }
        return items;
    }

    /**
     * @return all entries from table mail_items as list
     */
    private List<MailItem> getMailItems(Connection connection) throws SQLException {
        List<MailItem> mailItems = new ArrayList<>();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM `" + VMangosDB.mail_items.TABLE_NAME + "`;");
        while (resultSet.next()) {
            int itemGuid = resultSet.getInt(VMangosDB.mail_items.ITEM_GUID);
            int owner = resultSet.getInt(VMangosDB.mail_items.RECEIVER_GUID);
            mailItems.add(new MailItem(itemGuid, owner));
        }
        return mailItems;
    }

    /**
     * Filters out bags from Items.
     * A bag can be linked to a character (character guid) and has a unique id (bag guid)
     *
     * @param items list which contains all character items
     * @return filtered list which contains only bags
     */
    private List<Bag> getBags(List<Item> items) {
        List<Bag> bags = new ArrayList<>();
        Set<Integer> addedBagGuids = new HashSet<>();

        for (Item item : items) {
            // if item is in a bag -> bag_guid ist not 0
            if (item.getBagGuid() != 0 && !addedBagGuids.contains(item.getBagGuid())) {
                // create new bag object and mark as added
                bags.add(new Bag(item.getBagGuid(), item.getCharacterGuid()));
                addedBagGuids.add(item.getBagGuid());
            }
        }
        return bags;
    }

    /**
     * @param allItems entries from character_inventory (items and bags)
     * @param bags list of bags
     * @return filtered item list which does no longer contain bags
     */
    private List<Item> getItemsWithoutBags(List<Item> allItems, List<Bag> bags) {
        Set<Integer> bagItemGuids = bags.stream()
                .map(Bag::getItemGuid)
                .collect(Collectors.toSet());

        return allItems.stream()
                .filter(item -> !bagItemGuids.contains(item.getItemGuid()))
                .collect(Collectors.toList());
    }

    public int sortBagsForCharacter(Connection connection, int characterGuid, List<Bag> bags, int currentItemGuid) throws SQLException {
        PreparedStatement update_item_instance_item_guid_query = connection.prepareStatement(
                "UPDATE `" + VMangosDB.item_instance.TABLE_NAME + "` SET `" + VMangosDB.item_instance.GUID + "` = ? WHERE `" + VMangosDB.item_instance.GUID + "` = ?");
        PreparedStatement update_character_inventory_item_guid_query = connection.prepareStatement(
                "UPDATE `" + VMangosDB.character_inventory.TABLE_NAME + "` SET `" + VMangosDB.character_inventory.ITEM_GUID + "` = ? WHERE `" + VMangosDB.character_inventory.ITEM_GUID + "` = ?");
        PreparedStatement update_character_inventory_bag_guid_query = connection.prepareStatement(
                "UPDATE `"+ VMangosDB.character_inventory.TABLE_NAME +"` SET `"+ VMangosDB.character_inventory.BAG +"` = ? WHERE `"+ VMangosDB.character_inventory.BAG +"` = ?");
        for (Bag bag : bags) {
            if (bag.getCharacterGuid() == characterGuid) {
                update_item_instance_item_guid_query.setInt(1, currentItemGuid);
                update_item_instance_item_guid_query.setInt(2, bag.getItemGuid());
                update_item_instance_item_guid_query.executeUpdate();
                // update character_inventory item
                update_character_inventory_item_guid_query.setInt(1, currentItemGuid);
                update_character_inventory_item_guid_query.setInt(2, bag.getItemGuid());
                update_character_inventory_item_guid_query.executeUpdate();
                // update character_inventory bag
                update_character_inventory_bag_guid_query.setInt(1, currentItemGuid);
                update_character_inventory_bag_guid_query.setInt(2, bag.getItemGuid());
                update_character_inventory_bag_guid_query.executeUpdate();
                // increase guid counter
                currentItemGuid++;
            }
        }
        return currentItemGuid;
    }

    public int sortItemsForCharacter(Connection connection, int characterGuid, List<Item> items, int currentItemGuid) throws SQLException {
        PreparedStatement update_item_instance_item_guid_query = connection.prepareStatement(
                "UPDATE `" + VMangosDB.item_instance.TABLE_NAME + "` SET `" + VMangosDB.item_instance.GUID + "` = ? WHERE `" + VMangosDB.item_instance.GUID + "` = ?");
        PreparedStatement update_character_inventory_item_guid_query = connection.prepareStatement(
                "UPDATE `" + VMangosDB.character_inventory.TABLE_NAME + "` SET `" + VMangosDB.character_inventory.ITEM_GUID + "` = ? WHERE `" + VMangosDB.character_inventory.ITEM_GUID + "` = ?");
        for (Item item : items) {
            if (item.getCharacterGuid() == characterGuid) {
                // update item at character_inventory
                update_character_inventory_item_guid_query.setInt(1, currentItemGuid);
                update_character_inventory_item_guid_query.setInt(2, item.getItemGuid());
                update_character_inventory_item_guid_query.executeUpdate();
                // update item at item_instance
                update_item_instance_item_guid_query.setInt(1, currentItemGuid);
                update_item_instance_item_guid_query.setInt(2, item.getItemGuid());
                update_item_instance_item_guid_query.executeUpdate();
                // increase guid counter
                currentItemGuid++;
            }
        }
        return currentItemGuid;
    }

    public int sortMailItemsForCharacter(Connection connection, int characterGuid, List<MailItem> mailItems, int currentItemGuid) throws SQLException {
        PreparedStatement update_item_instance_item_guid_query = connection.prepareStatement(
                "UPDATE `" + VMangosDB.item_instance.TABLE_NAME + "` SET `" + VMangosDB.item_instance.GUID + "` = ? WHERE `" + VMangosDB.item_instance.GUID + "` = ?");
        PreparedStatement update_mail_items_guid_query = connection.prepareStatement(
                "UPDATE `" + VMangosDB.mail_items.TABLE_NAME + "` SET `" + VMangosDB.mail_items.ITEM_GUID + "` = ? WHERE `" + VMangosDB.mail_items.ITEM_GUID + "` = ?");
        for (MailItem mailItem : mailItems) {
            if (mailItem.getOwnerGuid() == characterGuid) {
                // update item at mail_items
                update_mail_items_guid_query.setInt(1, currentItemGuid);
                update_mail_items_guid_query.setInt(2, mailItem.getItemGuid());
                update_mail_items_guid_query.executeUpdate();
                // update item at item_instance
                update_item_instance_item_guid_query.setInt(1, currentItemGuid);
                update_item_instance_item_guid_query.setInt(2, mailItem.getItemGuid());
                update_item_instance_item_guid_query.executeUpdate();
                // increase guid counter
                currentItemGuid++;
            }
        }
        return currentItemGuid;
    }
}
