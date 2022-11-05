package logging;

public class Log {

    private static IPrintListener iPrintListener;

    public static void addPrintListener(IPrintListener listener){
        iPrintListener = listener;
    }

    public static void print(String text) {
        // System.out.println(message);
        iPrintListener.print(text);
        iPrintListener.print("\n");
    }
}
