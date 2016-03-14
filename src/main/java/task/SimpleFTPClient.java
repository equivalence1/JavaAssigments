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
    private static BufferedReader stdIn;
    private static boolean isConnected = false;

    public static void main(String args[]) {
        stdIn = new BufferedReader(new InputStreamReader(System.in));
        ClientSideProtocol.printUsageMessage();

        try {
            interactGlobal();
            stdIn.close();
        } catch (Exception e) {
            endWithError("Error occurred during session. See trace", e);
        }
    }

    private static void setHostAndPort(String host, int port) {
        SimpleFTPClient.host = host;
        SimpleFTPClient.port = port;
    }

    private static void connect() {
        try (
                Socket ftpSocket = new Socket(host, port);
                PrintWriter out = new PrintWriter(ftpSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                                new InputStreamReader(ftpSocket.getInputStream()))
        ) {
            isConnected = true;
            ClientSideProtocol.printConnectMessage();
            interactInsideConnection(in, out);
        } catch (Exception e) {
            endWithError("Error occurred during session. See trace", e);
        }
    }

    private static void disconnect() {
        isConnected = false;
        ClientSideProtocol.printDisconnectMessage();
    }

    private static void interactGlobal() throws IOException {
        String userInput;
        while ((userInput = stdIn.readLine()) != null) {
            ClientSideProtocol.process(userInput, null, null);
        }
    }

    private static void interactInsideConnection(BufferedReader in, PrintWriter out)
            throws IOException {
        String userInput;
        while ((userInput = stdIn.readLine()) != null) {
            ClientSideProtocol.process(userInput, in, out);
            if (!isConnected)
                return;
        }
    }

    private static void endWithError(String error, Exception e) {
        GlobalNamespace.printError(error);
        if (e != null) {
            e.printStackTrace();
        }
        GlobalNamespace.printError("Aborting");
        System.exit(1); //TODO something more smart. need to close resources
    }

    public static class ClientSideProtocol {

        public static void printUsageMessage() {
            GlobalNamespace.printInfo("Usage:");
            GlobalNamespace.printInfo("Type 'connect <host> <port> to connect to server.'");
            GlobalNamespace.printInfo("Type 'disconnect' if you want to disconnect from server.");
            GlobalNamespace.printInfo("Type 'list <dir path>' to ask server to list directory.");
            GlobalNamespace.printInfo("Type 'get <file path>' to ask server to print file content.");
            GlobalNamespace.printInfo("Type '?' to see this help again.");
        }

        private static void printConnectMessage() {
            GlobalNamespace.printSuccess("You are now connected to server <" + host + ":" + port + ">");
        }

        private static void printDisconnectMessage() {
            GlobalNamespace.printSuccess("You are disconnected from server <" + host + ":" + port + ">");
        }

        private static void process(String userInput, BufferedReader in, PrintWriter out) throws IOException {
            String tokens[] = userInput.split(" ");
            if (tokens.length > 0) {
                switch (tokens[0]) {
                    case ("connect"):
                        handleConnectInput(tokens);
                        break;
                    case ("disconnect"):
                        handleDisconnectInput();
                        break;
                    case ("list"):
                        handleListInput(tokens, in, out);
                        break;
                    case ("get"):
                        handleGetInput(tokens, in, out);
                        break;
                    case ("?"):
                        handleHelpInput();
                        break;
                    default:
                        handleUnknownInput();
                }
            }
        }

        private static void handleConnectInput(String tokens[]) {
            if (isConnected) {
                GlobalNamespace.printWarning("You are already connected to server. You need to disconnect firstly.");
            } else {
                if (tokens.length == 3 && GlobalNamespace.isPort(tokens[2])) {
                    setHostAndPort(tokens[1], Integer.parseInt(tokens[2]));// NumberFormatException cannot occur here
                    connect();
                } else {
                    incorrectInput("connect");
                }
            }
        }

        private static void handleDisconnectInput() {
            if (isConnected) {
                disconnect();
            } else {
                GlobalNamespace.printWarning("You are not connected to any server yet.");
            }
        }

        private static void handleListInput(String tokens[], BufferedReader in, PrintWriter out) throws IOException {
            if (tokens.length != 2) {
                incorrectInput("list");
            } else {
                out.println("list " + tokens[1]);
                String response = in.readLine();
                GlobalNamespace.printSuccess("Response for 'list':");
                GlobalNamespace.printNormal(response);
            }
        }

        private static void handleGetInput(String tokens[], BufferedReader in, PrintWriter out) throws IOException {
            if (tokens.length != 2) {
                incorrectInput("get");
            } else {
                out.println("get " + tokens[1]);
                String response = in.readLine();
                GlobalNamespace.printSuccess("Response for 'get':");
                GlobalNamespace.printNormal(response);
            }
        }

        private static void handleHelpInput() {
            printUsageMessage();
        }

        private static void handleUnknownInput() {
            GlobalNamespace.printWarning("Unknown command. Type '?' to see usage.");
        }

        private static void incorrectInput(String whichInput) {
            GlobalNamespace.printWarning("Incorrect form of '" + whichInput + "' query.");
            GlobalNamespace.printWarning("Type '?' to get help.");
        }
    }
}
