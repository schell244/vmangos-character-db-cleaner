import cleaner.DatabaseCleaner;
import logging.Log;
import ui.ControlWindow;
import ui.IRunListener;

import javax.swing.SwingWorker;
import java.sql.Connection;
import java.sql.DriverManager;

public class Main {

    public static void main(String[] args) {
        ControlWindow controlWindow = new ControlWindow();
        Log.addPrintListener(controlWindow::appendResultText);
        IRunListener listener = (user, pass, location) -> {
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    controlWindow.clearResultText();
                    controlWindow.setRunButtonEnabled(false);
                    Log.print("Connecting to database: " + location);

                    Connection connection = DriverManager.getConnection(location, user, pass);
                    Log.print("Success!");

                    long startTime = System.currentTimeMillis();

                    DatabaseCleaner databaseCleaner = new DatabaseCleaner(connection);
                    databaseCleaner.sortItemGuids();
                    connection.close();

                    long endTime = System.currentTimeMillis();
                    long durationMillis = endTime - startTime;
                    long milliseconds = durationMillis % 1000;
                    long seconds = (durationMillis / 1000) % 60;
                    long minutes = (durationMillis / (1000 * 60)) % 60;
                    long hours   = (durationMillis / (1000 * 60 * 60));
                    Log.printLine();
                    Log.print("Finished.");
                    Log.print(String.format("Duration: %02d:%02d:%02d.%03d (hh:mm:ss.SSS)", hours, minutes, seconds, milliseconds));

                    return null;
                }

                @Override
                protected void done() {
                    controlWindow.setRunButtonEnabled(true);
                    try {
                        get();
                    } catch (Exception e) {
                        e.printStackTrace();
                        controlWindow.appendResultText("Error: \n" + e.getMessage());
                    }
                }
            };
            worker.execute();
        };
        controlWindow.addListener(listener);
    }
}