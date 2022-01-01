package database_models;

public class Item {

    // character_inventory
    // guid, bag, slot, item_guid, item_id

    public int character_guid;
    public int bag_guid;
    public int item_guid;

    public Item(int character_guid, int bag_guid, int item_guid) {
        this.item_guid = item_guid;
        this.bag_guid = bag_guid;
        this.character_guid = character_guid;
    }
}