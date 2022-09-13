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

    public DatabaseCleaner(Connection connection) throws Exception {
        this.connection = connection;
        initValues();

        // maybe make configurable with gui checkbox?
        clearRespawnTables();
        clearCoolDownTables();
    }

    private void initValues() throws Exception{
        Statement statement = connection.createStatement();
        ResultSet charactersGuidMin = statement.executeQuery(
                "SELECT MIN(`" + VMangosDB.characters.GUID + "`) FROM `" + VMangosDB.characters.TABLE_NAME + "`");
        if(charactersGuidMin.next()){
            lowestCharacterGuid = charactersGuidMin.getInt(1);
            Log.print("Lowest character guid: " + lowestCharacterGuid);
        }else{
            throw new Exception("Error fetching min character guid from database!");
        }
        ResultSet charactersGuidMax = statement.executeQuery(
                "SELECT MAX(`" + VMangosDB.characters.GUID + "`) FROM `" + VMangosDB.characters.TABLE_NAME + "`");
        if(charactersGuidMax.next()){
            highestCharacterGuid = charactersGuidMax.getInt(1);
            Log.print("Highest character guid: " + highestCharacterGuid);
        }else{
            throw new Exception("Error fetching max character guid from database!");
        }
        ResultSet itemInstance = statement.executeQuery(
                "SELECT MAX(`" + VMangosDB.item_instance.GUID + "`) FROM `" + VMangosDB.item_instance.TABLE_NAME + "`");
        if(itemInstance.next()){
            highestItemGuid = itemInstance.getInt(1);
            Log.print("Currently highest item guid at " + VMangosDB.item_instance.TABLE_NAME + ": " + highestItemGuid);
            Log.print("\n");
        }else{
            throw new Exception("Error fetching max item guid from database!");
        }
    }

    public void sortItemGuids() throws SQLException {
        Log.print("Move all items (which can be linked to a character) after highest item guid");
        ItemGuidSortTask task1 = new ItemGuidSortTask();
        int newItemStartGuid = highestItemGuid + 1;
        task1.run(connection, lowestCharacterGuid, highestCharacterGuid, newItemStartGuid);

        Log.print("Remove all items, which could not be linked to any character");
        clearUnusedItems();

        Log.print("Move guids to start, counting from 1");
        ItemGuidSortTask task2 = new ItemGuidSortTask();
        newItemStartGuid = 1;
        task2.run(connection, lowestCharacterGuid, highestCharacterGuid, newItemStartGuid);
    }

    private void clearUnusedItems() throws SQLException{
        Statement statement = connection.createStatement();
        statement.executeUpdate(
                "DELETE FROM `" + VMangosDB.item_instance.TABLE_NAME + "` WHERE `" + VMangosDB.item_instance.GUID + "` <= " + highestItemGuid + " ;");
    }

    public void clearRespawnTables() throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate("DELETE FROM `creature_respawn`;");
        statement.executeUpdate("DELETE FROM `gameobject_respawn`;");
    }

    public void clearCoolDownTables() throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate("DELETE FROM `character_spell_cooldown`;");
        statement.executeUpdate("DELETE FROM `pet_spell_cooldown`;");
    }
}
