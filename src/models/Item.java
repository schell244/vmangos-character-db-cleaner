package models;

public class Item {

    // character_inventory
    // guid, bag, slot, item_guid, item_id

    private int characterGuid;
    private int bagGuid;
    private int itemGuid;

    public Item(int characterGuid, int bagGuid, int itemGuid) {
        this.itemGuid = itemGuid;
        this.bagGuid = bagGuid;
        this.characterGuid = characterGuid;
    }

    public int getCharacterGuid() {
        return characterGuid;
    }

    public int getBagGuid() {
        return bagGuid;
    }

    public int getItemGuid() {
        return itemGuid;
    }

    public void setCharacterGuid(int characterGuid) {
        this.characterGuid = characterGuid;
    }

    public void setBagGuid(int bagGuid) {
        this.bagGuid = bagGuid;
    }

    public void setItemGuid(int itemGuid) {
        this.itemGuid = itemGuid;
    }
}