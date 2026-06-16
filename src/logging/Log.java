package logging;

import java.util.List;
import java.util.ArrayList;

public class Log {

    private static final List<IPrintListener> listeners = new ArrayList<>();

    public static void addPrintListener(IPrintListener listener) {
        listeners.add(listener);
    }

    public static void print(String text) {
        for (IPrintListener listener : listeners)
            listener.print(text + "\n");
    }

    public static void printLine() {
        print("--------------------------------------------");
    }
}
