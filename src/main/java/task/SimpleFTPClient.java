package task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by equi on 14.03.16.
 *
 * @author Kravchenko Dima
 */
public class SimpleFTPClient {
    private static String host;
    private static int port;

    private static final String usageString = "Usage: java SimpleFTPClient <host> <port>\n" +
            "Aborting";

    public static void main(String args[]) {
        if (!checkArgs(args)) {
            GlobalNamespace.printWarning(usageString);
            return;
        }

        setHostAndPort(args);
        runClientSide();
    }

    private static boolean checkArgs(String args[]) {
        if (args.length != 2) {
            return false;
        }

        try {
            int port = Integer.parseInt(args[0]);
            return (port >= 0 && port < 65536);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void setHostAndPort(String args[]) {
        host = args[0];
        port = Integer.parseInt(args[1]); // no exceptions can occur here.
    }

    private static void runClientSide() {
        try (
                Socket echoSocket = new Socket(host, port);
                PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                                new InputStreamReader(echoSocket.getInputStream()));
                BufferedReader stdIn = new BufferedReader(
                                new InputStreamReader(System.in))
        ) {
            GlobalNamespace.printSuccess("You are now connected to server <" + host + ":" + port + ">");
            interact(stdIn, in, out);
        } catch (Exception e) {
            GlobalNamespace.printError("Could not establish connection.");
            e.printStackTrace();
        }
    }

    private static void interact(BufferedReader stdIn, BufferedReader in, PrintWriter out)
            throws IOException {
        String userInput;
        while ((userInput = stdIn.readLine()) != null) {
            process(userInput);
            out.println(userInput);
            GlobalNamespace.printSuccess("echo: " + in.readLine());
        }
    }

    private static void process(String userInput) {

    }
}
