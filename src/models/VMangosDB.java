package models;

public class VMangosDB {

    public static class characters{
        public static final String TABLE_NAME = "characters";
        public static final String GUID       = "guid";
    }

    public static class character_inventory{
        public static final String TABLE_NAME = "character_inventory";
        public static final String GUID       = "guid";
        public static final String BAG        = "bag";
        public static final String ITEM_GUID  = "item_guid";
    }

    public static class mail_items{
        public static final String TABLE_NAME    = "mail_items";
        public static final String ITEM_GUID     = "item_guid";
        public static final String RECEIVER_GUID = "receiver_guid";
    }

    public static class item_instance{
        public static final String TABLE_NAME = "item_instance";
        public static final String GUID       = "guid";
    }
}
