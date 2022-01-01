import java.sql.Connection;
import java.sql.DriverManager;

public class Main {

    //------------------------- Config --------------------------------------------------
    private static final int ITEM_START_GUID         = 10000; // first run: this value should be higher than any item guid in your db! Second run: can start from to 1
    private static final String DB_USER              = "example_user";
    private static final String DB_PASSWORD          = "example_user_pass";
    private static final String DB_LOCATION          = "jdbc:mysql://127.0.0.1:3306/characters";
    private static final boolean clearRespawn        = true;
    private static final boolean clearSpellCoolDowns = true;
    //------------------------------------------------------------------------------------

    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection(DB_LOCATION, DB_USER, DB_PASSWORD);
            DatabaseCleaner databaseCleaner = new DatabaseCleaner(connection);
            databaseCleaner.runItemGuidSortTask(connection, ITEM_START_GUID);
            if(clearRespawn){
                databaseCleaner.clearRespawnTables(connection);
            }
            if(clearSpellCoolDowns){
                databaseCleaner.clearCoolDownTables(connection);
            }
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}