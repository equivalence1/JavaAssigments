package task;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by equi on 14.03.16.
 *
 * @author Kravchenko Dima
 */
public class SimpleFTPServer {
    private static int port;

    private static final String usageString = "Usage: java SimpleFTPServer <port>\nAborting";

    public static void main(String args[]) {
        if (!checkArgs(args)) {
            GlobalNamespace.printWarning(usageString);
            return;
        }

        setPort(args);
        startHandleConnections();
    }

    private static boolean checkArgs(String args[]) {
        if (args.length != 1) {
            return false;
        }

        try {
            int port = Integer.parseInt(args[0]);
            return (port >= 0 && port < 65536);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void setPort(String args[]) {
        port = Integer.parseInt(args[0]);
    }

    private static void startHandleConnections() {
        GlobalNamespace.printInfo("Establishing connection.");
        try (
                ServerSocket serverSocket = new ServerSocket(port);
                Socket clientSocket = serverSocket.accept();
                PrintWriter out =
                        new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()))
        ) {
            GlobalNamespace.printSuccess("Connection established.");
            String inputLine, outputLine;

            while ((inputLine = in.readLine()) != null) {
                outputLine = inputLine;
                out.println(outputLine);
                GlobalNamespace.printInfo("Client sent:");
                GlobalNamespace.printWarning(inputLine);
                GlobalNamespace.printSuccess("");
                if (outputLine.equals("Bye."))
                    break;
            }

            GlobalNamespace.printInfo("Client closed the connection.");
        } catch (Exception e) {
            GlobalNamespace.printError("Can not listen to the port #" + port);
            e.printStackTrace();
        }
    }
}
