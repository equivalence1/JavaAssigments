package task;

/**
 * Created by equi on 22.04.16.
 *
 * @author Kravchenko Dima
 */
public class GlobalFunctions {

    private static final String RESET = "\u001b[0m";
    private static final String RED = "\u001b[31m";
    private static final String GREEN = "\u001b[32m";
    private static final String YELLOW = "\u001b[33m";
    private static final String BLUE = "\u001b[34m";

    public static void printError(String error) {
        setColour(RED);
        System.out.print(error);
        setColour(RESET);
        System.out.println();
    }

    public static void printInfo(String info) {
        setColour(BLUE);
        System.out.print(info);
        setColour(RESET);
        System.out.println();
    }

    public static void printSuccess(String message) {
        setColour(GREEN);
        System.out.print(message);
        setColour(RESET);
        System.out.println();
    }

    public static void printWarning(String usage) {
        setColour(YELLOW);
        System.out.print(usage);
        setColour(RESET);
        System.out.println();
    }

    public static void printNormal(String text) {
        System.out.println(text);
    }

    public static boolean isInteger(String number) {
        try {
            //noinspection ResultOfMethodCallIgnored
            Integer.parseInt(number);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isPort(String port) {
        return isInteger(port) && checkPort(Integer.parseInt(port));
    }

    private static boolean checkPort(int port) {
        return port >= 0 && port < (1 << 16);
    }

    private static void setColour(String colour) {
        System.out.print(colour);
    }

}
