package database_models;

public class Bag {

    public int guid;
    public int character_guid;

    public Bag(int guid, int character_guid) {
        this.guid = guid;
        this.character_guid = character_guid;
    }
}