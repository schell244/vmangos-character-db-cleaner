package database_models;

public class MailItem {

    public int item_guid;
    public int owner;

    public MailItem(int item_guid, int owner) {
        this.item_guid = item_guid;
        this.owner = owner;
    }
}