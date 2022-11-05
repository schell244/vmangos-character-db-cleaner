import cleaner.DatabaseCleaner;
import logging.Log;
import ui.ControlWindow;
import ui.IRunListener;

import java.sql.Connection;
import java.sql.DriverManager;

public class Main {

    public static void main(String[] args) {
        ControlWindow controlWindow = new ControlWindow();
        Log.addPrintListener(controlWindow::appendResultText);
        IRunListener listener = (user, pass, location) -> {
            try {
                controlWindow.clearResultText();
                Connection connection = DriverManager.getConnection(location, user, pass);
                DatabaseCleaner databaseCleaner = new DatabaseCleaner(connection);
                databaseCleaner.sortItemGuids();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
                controlWindow.appendResultText("Error: \n" + e.getMessage());
            }
        };
        controlWindow.addListener(listener);
    }
}