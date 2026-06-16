package models;

public class MailItem {

    private int itemGuid;
    private int ownerGuid;

    public MailItem(int itemGuid, int owner) {
        this.itemGuid = itemGuid;
        this.ownerGuid = owner;
    }

    public int getItemGuid() {
        return itemGuid;
    }

    public int getOwnerGuid() {
        return ownerGuid;
    }

}