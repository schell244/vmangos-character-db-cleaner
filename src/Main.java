import cleaner.DatabaseCleaner;
import logging.Log;
import ui.ControlWindow;
import ui.IRunListener;

import java.sql.Connection;
import java.sql.DriverManager;

public class Main {

    public static void main(String[] args) {
        ControlWindow controlWindow = new ControlWindow();
        IRunListener listener = (user, pass, location) -> {
            try {
                controlWindow.showResultText("");
                Connection connection = DriverManager.getConnection(location, user, pass);
                DatabaseCleaner databaseCleaner = new DatabaseCleaner(connection);
                databaseCleaner.sortItemGuids();
                controlWindow.showResultText(Log.getLogs());
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
                controlWindow.showResultText("Error: \n" + e.getMessage());
            }
        };
        controlWindow.addListener(listener);
    }
}