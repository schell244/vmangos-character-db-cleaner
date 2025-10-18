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

    public void setItemGuid(int itemGuid) {
        this.itemGuid = itemGuid;
    }

    public void setOwnerGuid(int ownerGuid) {
        this.ownerGuid = ownerGuid;
    }
}