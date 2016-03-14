package task;

import java.io.BufferedReader;
import java.io.IOException;
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
    private static boolean isRunning;

    public static void main(String args[]) {
        if (!checkArgs(args)) {
            GlobalNamespace.printWarning(usageString);
            return;
        }

        isRunning = true;

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
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                ServerSideProtocol.process(inputLine, out);
                if (!isRunning)
                    break;
            }

            GlobalNamespace.printWarning("Client closed the connection.");
        } catch (Exception e) {
            GlobalNamespace.printError("Can not listen to the port #" + port);
            e.printStackTrace();
        }
    }

    public static class ServerSideProtocol {

        public static void printUsageMessage() {
            GlobalNamespace.printInfo("Usage:");
            GlobalNamespace.printInfo("Send 'list <dir path>' to ask server to list directory.");
            GlobalNamespace.printInfo("Send 'get <file path>' to ask server to print file content.");
            GlobalNamespace.printInfo("Send 'stop' to stop the server.");
        }

        private static void process(String userInput, PrintWriter out) throws IOException {
            String tokens[] = userInput.split(" ");
            if (tokens.length > 0) {
                switch (tokens[0]) {
                    case ("list"):
                        handleListInput(tokens, out);
                        break;
                    case ("get"):
                        handleGetInput(tokens, out);
                        break;
                    case ("stop"):
                        handleStopInput();
                        break;
                    case ("?"):
                        handleHelpInput();
                        break;
                    default:
                        handleUnknownInput();
                }
            }
        }

        private static void handleListInput(String tokens[], PrintWriter out) throws IOException {
            if (tokens.length != 2) {
                incorrectInput("list");
            } else {
                String listResult = FSHandler.list(tokens[1]);
                out.println(listResult);
            }
        }

        private static void handleGetInput(String tokens[], PrintWriter out) throws IOException {
            if (tokens.length != 2) {
                incorrectInput("get");
            } else {
                String getResult = FSHandler.get(tokens[1]);
                out.println(getResult);
            }
        }

        private static void handleStopInput() {
            isRunning = false;
        }

        private static void handleHelpInput() {
            printUsageMessage();
        }

        private static void handleUnknownInput() {
            GlobalNamespace.printWarning("Unknown command. Type '?' to see usage.");
        }

        /**
         * this method should never be invoked on server. Just in case something strange happened
         */
        private static void incorrectInput(String whichInput) {
            GlobalNamespace.printWarning("Incorrect form of '" + whichInput + "' query.");
            GlobalNamespace.printWarning("Type '?' to get help.");
        }
    }
}
