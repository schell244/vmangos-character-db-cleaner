# vmangos-character-db-cleaner

Small tool to order item guids used in VMangos character database.
Affected tables:
```
character_inventory
mail_items
item_instance
```
Each run also clears out tables related to last world sessions.
Affected tables:
```
creature_respawn
gameobject_respawn
character_spell_cooldown
pet_spell_cooldown
```

# How to run

**Backup your character db before continuing!**

### Option 1: Use Build Scripts (Linux)
```bash
./build.sh
./run.sh
```

### Option 2: IDE Setup (IntelliJ/Eclipse)

### 1) Add sql driver to project (driver working with MySQL version 5.5.62)

Example: IntelliJ IDE
- File -> Project Structure... -> Modules -> select Dependencies tab
- press [+] -> Add JARs or Directories -> select mysql-connector-java-5.1.49.jar from lib folder
- apply, done.

### 2) Open Main.Java and run

Enter database connection details into gui fields:
- **Username:** Database user (default: `mangos`)
- **Password:** Database password (default: `mangos`)
- **Host:** Database host (default: `127.0.0.1`)
- **Port:** Database port (default: `3306`)
- **Database:** Database name (default: `characters`)

Press run button to start the cleaning process.

## Example Results

All item guids at `item_instance` are ordered sequentially.
Any items which could not be linked to any character
(because character was deleted, or a bot, ...) have been removed.

**Before run:**
- character with guid 1 has items with guids 2,50,767,800
- character with guid 2 has items with guids 5,23,77,9000

**After run:**
- character with guid 1 has items with guids 1,2,3,4
- character with guid 2 has items with guids 5,6,7,8

<img src="./img/preview.png" alt="Preview" width="600">
