package cleaner;

import logging.Log;
import models.VMangosDB;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseCleaner {
    private final Connection connection;
    private int lowestCharacterGuid;
    private int highestCharacterGuid;
    private int highestItemGuid;

    public DatabaseCleaner(Connection connection) {
        this.connection = connection;
    }

    public void run() throws Exception {
        initValues();
        connection.setAutoCommit(false);
        try {
            clearRespawnTables();
            clearCoolDownTables();
            sortItemGuids();
            connection.commit();
            Log.print("All changes committed.");
        } catch (Exception e) {
            connection.rollback();
            Log.print("ERROR: all changes rolled back. Database is unchanged.");
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private void initValues() throws Exception {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(
                    "SELECT MIN(`" + VMangosDB.characters.GUID + "`), MAX(`" + VMangosDB.characters.GUID + "`) FROM `" + VMangosDB.characters.TABLE_NAME + "`")) {
                if (!rs.next())
                    throw new Exception("Error fetching character guid range from database!");
                if (rs.getObject(1) == null)
                    throw new Exception("Characters table is empty — nothing to process.");
                lowestCharacterGuid  = rs.getInt(1);
                highestCharacterGuid = rs.getInt(2);
            }
            try (ResultSet rs = statement.executeQuery(
                    "SELECT MAX(`" + VMangosDB.item_instance.GUID + "`) FROM `" + VMangosDB.item_instance.TABLE_NAME + "`")) {
                if (!rs.next())
                    throw new Exception("Error fetching max item guid from database!");
                if (rs.getObject(1) == null)
                    throw new Exception("item_instance table is empty — nothing to process.");
                highestItemGuid = rs.getInt(1);
            }
        }
        Log.printLine();
        Log.print("Lowest character guid:  " + lowestCharacterGuid  + " (" + VMangosDB.characters.TABLE_NAME + ")");
        Log.print("Highest character guid: " + highestCharacterGuid + " (" + VMangosDB.characters.TABLE_NAME + ")");
        Log.print("Highest item guid:      " + highestItemGuid      + " (" + VMangosDB.item_instance.TABLE_NAME + ")");
        Log.printLine();
    }

    private void sortItemGuids() throws SQLException {
        new ItemGuidSortTask().run(connection, lowestCharacterGuid, highestCharacterGuid, highestItemGuid);
    }

    private void clearRespawnTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM `creature_respawn`");
            stmt.executeUpdate("DELETE FROM `gameobject_respawn`");
        }
    }

    private void clearCoolDownTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM `character_spell_cooldown`");
            stmt.executeUpdate("DELETE FROM `pet_spell_cooldown`");
        }
    }

}
