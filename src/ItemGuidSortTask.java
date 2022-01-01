import database_models.Bag;
import database_models.Item;
import database_models.MailItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemGuidSortTask {

    private void printLog(String message) {
        System.out.println(this.getClass().getSimpleName() + " -> " + message);
    }

    public void run(Connection connection, int CHARACTER_START_GUID, int CHARACTER_END_GUID, int ITEM_START_GUID) throws SQLException {
        printLog("ItemGuidSortTask -> start");

        // Fetch data from db
        List<Item> allItems = getAllItems(connection, CHARACTER_START_GUID, CHARACTER_END_GUID);
        List<MailItem> allMailItems = getMailItems(connection);
        // Filter fetched results
        List<Bag> bags = getBags(allItems);
        List<Item> filteredItems = getItemsWithoutBags(allItems, bags);

        printLog("total items ---------> " + allItems.size());
        printLog("mail items  ---------> " + allMailItems.size());
        printLog("bags ----------------> " + bags.size());
        printLog("total items - bags --> " + filteredItems.size());

        printLog("starting to sort items...");

        int currentGuid = ITEM_START_GUID;

        for (int charGuid = CHARACTER_START_GUID; charGuid <= CHARACTER_END_GUID; charGuid++) {
            currentGuid = sortBagsForCharacter(connection, charGuid, bags, currentGuid);
            currentGuid = sortItemsForCharacter(connection, charGuid, filteredItems, allMailItems, currentGuid);
        }

        printLog("Sorting items finished. Last item guid in database: " + (currentGuid - 1));
    }

    /**
     * @param CHARACTER_START_GUID min guid to fetch data
     * @param CHARACTER_END_GUID   max guid to fetch data
     * @return list containing items created from character_inventory data within desired character guid range
     */
    private List<Item> getAllItems(Connection connection, int CHARACTER_START_GUID, int CHARACTER_END_GUID) throws SQLException {
        List<Item> items = new ArrayList<>();
        // create prepared statement
        PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `character_inventory` WHERE `guid` >= ? AND `guid` <= ?");
        preparedStatement.setInt(1, CHARACTER_START_GUID);
        preparedStatement.setInt(2, CHARACTER_END_GUID);
        ResultSet resultSet = preparedStatement.executeQuery();
        // process the result set
        while (resultSet.next()) {
            int charGuid = resultSet.getInt("guid");
            int bagGuid = resultSet.getInt("bag");
            int itemGuid = resultSet.getInt("item_guid");
            Item item = new Item(charGuid, bagGuid, itemGuid);
            items.add(item);
        }
        return items;
    }

    /**
     * @return list containing all entries from table mail_items
     */
    private List<MailItem> getMailItems(Connection connection) throws SQLException {
        List<MailItem> mailItems = new ArrayList<>();
        // create statement
        Statement statement = connection.createStatement();
        // execute SQL query
        ResultSet resultSet = statement.executeQuery("SELECT * FROM `mail_items`;");
        // process the result set
        while (resultSet.next()) {
            int itemGuid = resultSet.getInt("item_guid");
            int owner = resultSet.getInt("receiver");
            mailItems.add(new MailItem(itemGuid, owner));
        }
        return mailItems;
    }

    /**
     * Filters out bags from Items.
     * A bag can be linked to a character (character_guid) and has a unique id (bag_guid)
     *
     * @param items list which contains all character items
     * @return filtered list which contains only bags
     */
    private List<Bag> getBags(List<Item> items) {
        List<Bag> bags = new ArrayList<>();
        for (Item item : items) {
            // if item is in a bag -> bag_guid ist not 0
            if (item.bag_guid != 0) {
                // before we add the bag, check if we already have it (since many items can be within the same bag)
                boolean alreadyAdded = false;
                for (Bag bag : bags) {
                    if (bag.guid == item.bag_guid) {
                        alreadyAdded = true;
                        break;
                    }
                }
                if (!alreadyAdded) {
                    // create new bag object
                    bags.add(new Bag(item.bag_guid, item.character_guid));
                }
            }
        }
        return bags;
    }

    /**
     * @param allItems entries from character_inventory (items and bags)
     * @param bags     list of bags
     * @return filtered item list which does no longer contain bags
     */
    private List<Item> getItemsWithoutBags(List<Item> allItems, List<Bag> bags) {
        List<Item> filteredItems = new ArrayList<>();
        for (Item item : allItems) {
            boolean isBag = false;
            for (Bag bag : bags) {
                if (bag.guid == item.item_guid) {
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

    public int sortBagsForCharacter(Connection connection, int character_guid, List<Bag> bags, int new_guid) throws SQLException {
        // create prepared statements
        PreparedStatement update_item_instance_item_guid_query       = connection.prepareStatement("UPDATE `item_instance` SET `guid` = ? WHERE `guid` = ?");
        PreparedStatement update_character_inventory_item_guid_query = connection.prepareStatement("UPDATE `character_inventory` SET `item_guid` = ? WHERE `item_guid` = ?");
        PreparedStatement update_character_inventory_bag_guid_query  = connection.prepareStatement("UPDATE `character_inventory` SET `bag` = ? WHERE `bag` = ?");
        // update bag guids:
        for(Bag bag : bags) {
            if(bag.character_guid == character_guid) {
                // update item_instance
                update_item_instance_item_guid_query.setInt(1, new_guid);
                update_item_instance_item_guid_query.setInt(2, bag.guid);
                update_item_instance_item_guid_query.executeUpdate();
                // update character_inventory item
                update_character_inventory_item_guid_query.setInt(1, new_guid);
                update_character_inventory_item_guid_query.setInt(2, bag.guid);
                update_character_inventory_item_guid_query.executeUpdate();
                // update character_inventory bag
                update_character_inventory_bag_guid_query.setInt(1, new_guid);
                update_character_inventory_bag_guid_query.setInt(2, bag.guid);
                update_character_inventory_bag_guid_query.executeUpdate();
                new_guid++;
            }
        }
        return new_guid;
    }

    public int sortItemsForCharacter(Connection connection, int character_guid, List<Item> items, List<MailItem> mailItems, int new_guid)  throws SQLException{
        PreparedStatement update_character_inventory_item_guid_query = connection.prepareStatement("UPDATE `character_inventory` SET `item_guid` = ? WHERE `item_guid` = ?");
        PreparedStatement update_item_instance_item_guid_query       = connection.prepareStatement("UPDATE `item_instance` SET `guid` = ? WHERE `guid` = ? ");
        PreparedStatement update_mail_items_guid_query               = connection.prepareStatement("UPDATE `mail_items` SET `item_guid` = ? WHERE `item_guid` = ?");
        for(Item item : items) {
            if(item.character_guid == character_guid) {
                // update item at character_inventory
                update_character_inventory_item_guid_query.setInt(1, new_guid);
                update_character_inventory_item_guid_query.setInt(2, item.item_guid);
                update_character_inventory_item_guid_query.executeUpdate();
                // update item at item_instance
                update_item_instance_item_guid_query.setInt(1, new_guid);
                update_item_instance_item_guid_query.setInt(2, item.item_guid);
                update_item_instance_item_guid_query.executeUpdate();
                // increase guid
                new_guid++;
            }
        }
        // (mail: mail_item + item_text)
        for(MailItem mailItem : mailItems) {
            if(mailItem.owner == character_guid) {
                // update item at mail_items
                update_mail_items_guid_query.setInt(1, new_guid);
                update_mail_items_guid_query.setInt(2, mailItem.item_guid);
                update_mail_items_guid_query.executeUpdate();
                // update item at item_instance
                update_item_instance_item_guid_query.setInt(1, new_guid);
                update_item_instance_item_guid_query.setInt(2, mailItem.item_guid);
                update_item_instance_item_guid_query.executeUpdate();
                new_guid++;
            }
        }
        return new_guid;
    }
}
