package task.task.server;

import task.FSHandler;
import task.GlobalFunctions;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by equi on 14.03.16.
 *
 * @author Kravchenko Dima
 */
public class TorrentTracker {
    private static final int PORT = 8081;
    private static final int PART_SIZE = 10 * 1024 * 1024; // 10M
    private static final int UPDATE_TL = 5 * 60; // 5 min
    //private static final String USAGE = "Usage: java SimpleFTPServer <PORT>\nAborting";
    private static boolean isRunning;

    public static void main(String args[]) {
        isRunning = true;
        startHandleConnections();
    }

    private static void startHandleConnections() {
        GlobalFunctions.printInfo("Starting to accept connections.");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (isRunning) {
                try (
                        Socket clientSocket = serverSocket.accept();
                        DataOutputStream out =
                                new DataOutputStream(clientSocket.getOutputStream());
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(clientSocket.getInputStream()))
                ) {
                    GlobalFunctions.printSuccess("Connection accepted.");
                    String inputLine;

                    while ((inputLine = in.readLine()) != null) {
                        ServerSideProtocol.process(inputLine, out);
                        if (!isRunning)
                            break;
                    }

                    GlobalFunctions.printWarning("ClientInfo closed the connection.");
                } catch (Exception e) {
                    
                }
            }
        } catch (Exception e) {
            GlobalFunctions.printError("Failed to create ServerSocket.");
            e.printStackTrace();
        }
    }

    public static class ServerSideProtocol {

        public static void printUsageMessage() {
            GlobalFunctions.printInfo("Usage:");
            GlobalFunctions.printInfo("Send 'list <dir path>' to ask server to list directory.");
            GlobalFunctions.printInfo("Send 'get <file path>' to ask server to print file content.");
            GlobalFunctions.printInfo("Send 'stop' to stop the server.");
        }

        private static void process(String userInput, DataOutputStream out) throws IOException {
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

        private static void handleListInput(String tokens[], DataOutputStream out) throws IOException {
            if (tokens.length != 2) {
                incorrectInput("list");
            } else {
                FSHandler.handleList(tokens[1], out);
                out.flush();
            }
        }

        private static void handleGetInput(String tokens[], DataOutputStream out) throws IOException {
            if (tokens.length != 2) {
                incorrectInput("get");
            } else {
                FSHandler.handleGet(tokens[1], out);
                out.flush();
            }
        }

        private static void handleStopInput() {
            isRunning = false;
        }

        private static void handleHelpInput() {
            printUsageMessage();
        }

        private static void handleUnknownInput() {
            GlobalFunctions.printWarning("Unknown command. Type '?' to see usage.");
        }

        /**
         * this method should never be invoked on server. Just in case if something strange happened
         */
        private static void incorrectInput(String whichInput) {
            GlobalFunctions.printWarning("Incorrect form of '" + whichInput + "' query.");
            GlobalFunctions.printWarning("Type '?' to get help.");
        }
    }
}
