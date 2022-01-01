# vmangos-character-db-cleaner

Small tool to move and order item guids used in VMangos character database.

Optional:
clear tables that are related to last world session:
```
creature_respawn
gameobject_respawn
character_spell_cooldown
pet_spell_cooldown
```

# how to run

Backup your character db before continuing!

### 1) Add sql driver to project

Example: intellij IDE
- File -> Project Structure... -> Modules -> select Dependencies tab
- press [+] -> Add JARs or Directories -> select mysql-connector-java-5.1.49.jar from lib folder
- apply, done.

###  2) Open Main.Java to set up the config
```
    //------------------------- Config --------------------------------------------------
    private static final int ITEM_START_GUID         = 10000; // first run: this value should be higher than any item guid in your db! Second run: can start from to 1
    private static final String DB_USER              = "example_user";
    private static final String DB_PASSWORD          = "example_user_pass";
    private static final String DB_LOCATION          = "jdbc:mysql://127.0.0.1:3306/characters";
    private static final boolean clearRespawn        = true;
    private static final boolean clearSpellCoolDowns = true;
    //------------------------------------------------------------------------------------
```
### 3) Run

Example result with first run and ITEM_START_GUID = 10000

before run:

- character with guid 1 has items 2,50,767,800
- character with guid 2 has items 5,23,77,9000

after run:

- character with guid 1 has items 10000,10002,10003,10004
- character with guid 2 has items 10005,10006,10007,10008

Any item from ```item_instance``` which has a guid value below 10000 could not be linked to a character (because the owner character was deleted or may have been a bot).
Make Sure there a no such items or remove them! Then continue with ITEM_START_GUID set to 1.

Example result with second run and ITEM_START_GUID = 1

before run:

- character with guid 1 has items 10000,10002,10003,10004
- character with guid 2 has items 10005,10006,10007,10008

after run:

- character with guid 1 has items 1,2,3,4
- character with guid 2 has items 5,6,7,8    
	