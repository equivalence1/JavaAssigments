package task;

import java.io.*;
import java.net.Socket;

/**
 * Created by equi on 14.03.16.
 *
 * @author Kravchenko Dima
 */
public class TorrentClient {
    private static String host;
    private static int port;
    private static BufferedReader stdIn;
    private static boolean isConnected = false;
    private static final int BUFFER_SIZE = 1000000;

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
        TorrentClient.host = host;
        TorrentClient.port = port;
    }

    private static void connect() {
        try (
                Socket ftpSocket = new Socket(host, port);
                PrintWriter out = new PrintWriter(ftpSocket.getOutputStream(), true);
                DataInputStream in = new DataInputStream(ftpSocket.getInputStream())
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

    private static void interactInsideConnection(DataInputStream in, PrintWriter out)
            throws IOException {
        String userInput;
        while ((userInput = stdIn.readLine()) != null) {
            ClientSideProtocol.process(userInput, in, out);
            if (!isConnected)
                return;
        }
    }

    private static void endWithError(String error, Exception e) {
        GlobalFunctions.printError(error);
        if (e != null) {
            e.printStackTrace();
        }
        GlobalFunctions.printError("Aborting");
        try {
            stdIn.close();
        } catch (Exception e1) {
            //ignoring on this stage
        }
        System.exit(1);
    }

    public static class ClientSideProtocol {

        public static void printUsageMessage() {
            GlobalFunctions.printInfo("Usage:");
            GlobalFunctions.printInfo("Type 'connect <host> <port>' to connect to server.'");
            GlobalFunctions.printInfo("Type 'disconnect' if you want to disconnect from server.");
            GlobalFunctions.printInfo("Type 'list <dir path>' to ask server to list directory.");
            GlobalFunctions.printInfo("Type 'get <file path>' to ask server to print file content.");
            GlobalFunctions.printInfo("Type 'stop' to send to server.'");
            GlobalFunctions.printInfo("Type '?' to see this help again.");
        }

        private static void printConnectMessage() {
            GlobalFunctions.printSuccess("You are now connected to server <" + host + ":" + port + ">");
        }

        private static void printDisconnectMessage() {
            GlobalFunctions.printSuccess("You are disconnected from server <" + host + ":" + port + ">");
        }

        private static void process(String userInput, DataInputStream in, PrintWriter out) throws IOException {
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
                    case ("stop"):
                        handleStopInput(out);
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
                GlobalFunctions.printWarning("You are already connected to server. You need to disconnect firstly.");
            } else {
                if (tokens.length == 3 && GlobalFunctions.isPort(tokens[2])) {
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
                GlobalFunctions.printWarning("You are not connected to any server yet.");
            }
        }

        private static void handleListInput(String tokens[], DataInputStream in, PrintWriter out) throws IOException {
            if (tokens.length != 2) {
                incorrectInput("list");
            } else {
                out.println("list " + tokens[1]);

                StringBuilder response = new StringBuilder();
                String toAppend;

                long size = in.readLong();
                toAppend = GlobalFunctions.BLUE + Long.toString(size) + GlobalFunctions.RESET + "\n";
                response.append(toAppend);

                for (int i = 0; i < size; i++) {
                    if (i != size - 1) {
                        toAppend = showFileInfo(in.readUTF(), in.readBoolean()) + "\n";
                    } else {
                        toAppend = showFileInfo(in.readUTF(), in.readBoolean());
                    }

                    response.append(toAppend);
                }

                GlobalFunctions.printSuccess("Response for 'list':");
                GlobalFunctions.printlnNormal(response.toString());
            }
        }

        private static void handleGetInput(String tokens[], DataInputStream in, PrintWriter out) throws IOException {
            if (tokens.length != 2) {
                incorrectInput("get");
            } else {
                out.println("get " + tokens[1]);

                long size = in.readLong();
                GlobalFunctions.printSuccess("Response for 'get':");
                GlobalFunctions.printInfo("file size: " + Long.toString(size));

                byte[] ioBuf  = new byte[BUFFER_SIZE];
                for (int i = 0; i < size / BUFFER_SIZE; i++) {
                    in.read(ioBuf, 0, BUFFER_SIZE);
                    String newString = new String(ioBuf);
                    GlobalFunctions.printNormal(newString);
                }

                ioBuf = new byte[(int)(size % BUFFER_SIZE)];
                in.read(ioBuf, 0, (int)(size % BUFFER_SIZE));
                String newString = new String(ioBuf);
                GlobalFunctions.printlnNormal(newString);
            }
        }

        private static void handleStopInput(PrintWriter out) throws IOException {
            out.println("stop");
            handleDisconnectInput();
        }

        private static void handleHelpInput() {
            printUsageMessage();
        }

        private static void handleUnknownInput() {
            GlobalFunctions.printWarning("Unknown command. Type '?' to see usage.");
        }

        private static void incorrectInput(String whichInput) {
            GlobalFunctions.printWarning("Incorrect form of '" + whichInput + "' query.");
            GlobalFunctions.printWarning("Type '?' to get help.");
        }

        private static String showFileInfo(String name, Boolean isDir) {
            return  "name: " + GlobalFunctions.GREEN + name + GlobalFunctions.RESET +
                    ", isDir: " + GlobalFunctions.YELLOW + isDir + GlobalFunctions.RESET;
        }
    }
}
