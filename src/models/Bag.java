package models;

public class Bag {

    private int itemGuid;
    private int characterGuid;

    public Bag(int itemGuid, int characterGuid) {
        this.itemGuid = itemGuid;
        this.characterGuid = characterGuid;
    }

    public int getItemGuid() {
        return itemGuid;
    }

    public int getCharacterGuid() {
        return characterGuid;
    }
}