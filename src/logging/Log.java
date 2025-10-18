package logging;

public class Log {

    private static IPrintListener iPrintListener;

    public static void addPrintListener(IPrintListener listener){
        iPrintListener = listener;
    }

    public static void print(String text) {
        // System.out.println(message);
        if (iPrintListener != null) {
            iPrintListener.print(text);
            iPrintListener.print("\n");
        }
    }

    public static void printLine() {
        print("--------------------------------------------");
    }
}
