import cleaner.DatabaseCleaner;
import logging.Log;
import ui.ControlWindow;
import ui.IRunListener;

import javax.swing.SwingWorker;
import java.sql.Connection;
import java.sql.DriverManager;

public class Main {

    private static String formatDuration(long millis) {
        return String.format("%02d:%02d:%02d.%03d (hh:mm:ss.SSS)",
                millis / (1000 * 60 * 60),
                (millis / (1000 * 60)) % 60,
                (millis / 1000) % 60,
                millis % 1000);
    }

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

                    try (Connection connection = DriverManager.getConnection(location, user, pass)) {
                        Log.print("Connected.");
                        long startTime = System.currentTimeMillis();
                        try {
                            new DatabaseCleaner(connection).run();
                            Log.printLine();
                            Log.print("Finished. Duration: " + formatDuration(System.currentTimeMillis() - startTime));
                        } catch (Exception e) {
                            Log.printLine();
                            Log.print("Failed after " + formatDuration(System.currentTimeMillis() - startTime));
                            throw e;
                        }
                    }

                    return null;
                }

                @Override
                protected void done() {
                    controlWindow.setRunButtonEnabled(true);
                    try {
                        get();
                    } catch (Exception e) {
                        Throwable cause = e.getCause() != null ? e.getCause() : e;
                        cause.printStackTrace();
                        controlWindow.appendResultText("Error: \n" + cause.getMessage());
                    }
                }
            };
            worker.execute();
        };
        controlWindow.addListener(listener);
    }
}
