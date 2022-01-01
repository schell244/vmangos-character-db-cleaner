import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseCleaner {

    private static int CHARACTER_START_GUID = 1;
    private static int CHARACTER_END_GUID;

    public DatabaseCleaner(Connection connection) throws Exception {
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT MAX(`guid`) FROM `characters`");
        if(resultSet.next()){
            CHARACTER_END_GUID = resultSet.getInt(1);
            System.out.println(this.getClass().getSimpleName() + " -> CHARACTER_START_GUID: " + CHARACTER_START_GUID);
            System.out.println(this.getClass().getSimpleName() + " -> CHARACTER_END_GUID:   " + CHARACTER_END_GUID);
        }else{
            throw new Exception("Error fetching last character id from database!");
        }
    }

    public void runItemGuidSortTask(Connection connection, int ITEM_START_GUID) throws SQLException {
        ItemGuidSortTask itemGuidSortTask = new ItemGuidSortTask();
        itemGuidSortTask.run(connection, CHARACTER_START_GUID, CHARACTER_END_GUID, ITEM_START_GUID);
    }

    public void clearRespawnTables(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate("DELETE FROM `creature_respawn`;");
        statement.executeUpdate("DELETE FROM `gameobject_respawn`;");
    }

    public void clearCoolDownTables(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate("DELETE FROM `character_spell_cooldown`;");
        statement.executeUpdate("DELETE FROM `pet_spell_cooldown`;");
    }
}
