package logging;

public class Log {

    private static final StringBuffer sb = new StringBuffer();
    public static void print(String message) {
        System.out.println(message);
        sb.append(message);
        sb.append("\n");
    }

    public static String getLogs(){
        String result = sb.toString();
        // clear sb
        sb.delete(0, sb.length());
        return result;
    }
}
