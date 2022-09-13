package cleaner;

import logging.Log;
import models.Bag;
import models.Item;
import models.MailItem;
import models.VMangosDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemGuidSortTask {

    public void run(Connection connection, int lowestCharacterGuid, int highestCharacterGuid, int newItemStartGuid) throws SQLException {
        Log.print(" ### new sort task ### start from guid: " + newItemStartGuid + " ###");

        List<Item> allItems = getAllItems(connection, lowestCharacterGuid, highestCharacterGuid);
        List<MailItem> mailItems = getMailItems(connection);
        // Filter fetched results
        List<Bag> bags = getBags(allItems);
        List<Item> itemsWithoutBags = getItemsWithoutBags(allItems, bags);

        Log.print("Total items: " + allItems.size());
        Log.print("Mail items: " + mailItems.size());
        Log.print("Bags: " + bags.size());
        Log.print("Items without bags: " + itemsWithoutBags.size());
        Log.print("Starting to sort items...");

        int currentGuid = newItemStartGuid;

        for (int charGuid = lowestCharacterGuid; charGuid <= highestCharacterGuid; charGuid++) {
            currentGuid = sortBagsForCharacter(connection, charGuid, bags, currentGuid);
            currentGuid = sortItemsForCharacter(connection, charGuid, itemsWithoutBags, currentGuid);
            currentGuid = sortMailItemsForCharacter(connection, charGuid, mailItems, currentGuid);
        }
        Log.print("Sorting successfully finished. Last item guid in database: " + (currentGuid - 1));
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
        for (Item item : items) {
            // if item is in a bag -> bag_guid ist not 0
            if (item.getBagGuid() != 0) {
                // before we add the bag, check if we already have it (since many items can be within the same bag)
                boolean alreadyAdded = false;
                for (Bag bag : bags) {
                    if (bag.getItemGuid() == item.getBagGuid()) {
                        alreadyAdded = true;
                        break;
                    }
                }
                if (!alreadyAdded) {
                    // create new bag object
                    bags.add(new Bag(item.getBagGuid(), item.getCharacterGuid()));
                }
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
        List<Item> filteredItems = new ArrayList<>();
        for (Item item : allItems) {
            boolean isBag = false;
            for (Bag bag : bags) {
                if (bag.getItemGuid() == item.getItemGuid()) {
                    isBag = true;
                    break;
                }
            }
            if (!isBag) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
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
