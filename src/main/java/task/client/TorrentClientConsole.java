package task.client;

import task.GlobalFunctions;

import java.io.*;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

/**
 * Created by equi on 24.04.16.
 *
 * @author Kravchenko Dima
 */
public class TorrentClientConsole {
    private static int port;
    private static boolean wasStopped;
    private static BufferedReader stdIn;
    private static TorrentClient torrentClient;

    public static void main(String args[]) {
        stdIn = new BufferedReader(new InputStreamReader(System.in));
        ClientSideProtocol.printUsageMessage();

        wasStopped = false;

        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            GlobalFunctions.printError("First argument should be a port number.");
            System.exit(1);
        }

        torrentClient = new TorrentClient(port);

        try {
            interactGlobal();
        } catch (Exception e) {
            endWithError("Error occurred during session. See trace.", e);
        }
    }

    private static void interactGlobal() throws IOException, TimeoutException {
        String userInput;
        while ((userInput = stdIn.readLine()) != null && !wasStopped) {
            ClientSideProtocol.process(userInput);
        }
    }

    private static void endWithError(String error, Exception e) {
        GlobalFunctions.printError(error);
        if (e != null) {
            e.printStackTrace();
        }
        GlobalFunctions.printError("Aborting");
        System.exit(1);
    }

    private static class ClientSideProtocol {

        private static void printUsageMessage() {
            GlobalFunctions.printInfo("Usage:");
            GlobalFunctions.printInfo("First argument should be a port number of client server.'");
            GlobalFunctions.printInfo("Type 'connect <host> <port>' to connect to server.'");
            GlobalFunctions.printInfo("Type 'disconnect' if you want to disconnect from server.");
            GlobalFunctions.printInfo("Type 'list' to get list of file's ids on server.");
            GlobalFunctions.printInfo("Type 'upload <path_to_file>' to upload file on server.");
            GlobalFunctions.printInfo("Type 'sources <file id>' to get list of seeds for this file.");
            GlobalFunctions.printInfo("Type 'stat <ip> <port> <file id>' get info about parts of file client has.");
            GlobalFunctions.printInfo("Type 'get <ip> <port> <file id> <part>' get part of the file.");
            GlobalFunctions.printInfo("Type '?' to see this help again.");
            GlobalFunctions.printInfo("Node that also every 5 minutes we send `update` request to server " +
                    "automatically");
        }

        private static void process(String userInput) throws IOException, TimeoutException {
            String tokens[] = userInput.split(" ");
            if (tokens.length > 0) {
                switch (tokens[0]) {
                    case ("start"):
                        handleStartInput();
                        break;
                    case ("stop"):
                        handleStopInput();
                        break;
                    case ("list"):
                        handleListInput();
                        break;
                    case ("upload"):
                        handleUploadInput(tokens);
                        break;
                    case ("source"):
                        handleSourceInput(tokens);
                        break;
                    case ("update"):
                        handleUpdateInput();
                        break;
                    case ("stat"):
                        handleStatInput(tokens);
                        break;
                    case ("get"):
                        handleGetInput(tokens);
                        break;
                    case ("?"):
                        handleHelpInput();
                        break;
                    default:
                        handleUnknownInput();
                }
            }
        }

        private static void handleStartInput() {
            torrentClient.start();
            GlobalFunctions.printSuccess("Client started and you are connected to server");
        }

        private static void handleStopInput() throws IOException {
            torrentClient.stop();
            GlobalFunctions.printInfo("Good buy.");
            wasStopped = true;
        }

        private static void handleListInput() throws IOException, TimeoutException {
            ArrayList<TorrentClient.ListResponseEntry> files = torrentClient.list();
            GlobalFunctions.printNormal("" + files.size());
            files.forEach(ClientSideProtocol::printListEntry);
        }

        private static void printListEntry(TorrentClient.ListResponseEntry file) {
            GlobalFunctions.printNormal("file id: " + file.id);
            GlobalFunctions.printNormal("file name: " + file.name);
            GlobalFunctions.printNormal("file size: " + file.size);
            GlobalFunctions.printNormal("");
        }

        private static void handleUploadInput(String tokens[]) throws IOException, TimeoutException {
            if (tokens.length != 2) {
                incorrectInput("upload");
                return;
            }

            try {
                int id = torrentClient.upload(tokens[1]);
                GlobalFunctions.printNormal("id for file '" + tokens[1] + "' is " + id);
            } catch (NoSuchFileException e) {
                GlobalFunctions.printWarning("file '" + tokens[1] + "' does not exist.");
            }
        }

        private static void handleSourceInput(String tokens[]) throws IOException, TimeoutException {
            if (tokens.length != 2) {
                incorrectInput("source");
                return;
            }

            try {
                int id = Integer.parseInt(tokens[1]);
                ArrayList<TorrentClient.SourcesResponseEntry> sources = torrentClient.sources(id);
                GlobalFunctions.printNormal(sources.size() + "");
                sources.forEach(ClientSideProtocol::printSourceEntry);
            } catch (NumberFormatException e) {
                GlobalFunctions.printWarning(tokens[1] + " is not an integer.");
            }
        }

        private static void printSourceEntry(TorrentClient.SourcesResponseEntry source) {
            GlobalFunctions.printNormal("<");
            for (int i = 0; i < 4; i++) {
                if (i != 3)
                    GlobalFunctions.printNormal(source.ip[i] + ".");
                else
                    GlobalFunctions.printNormal(source.ip[i] + ":");
            }
            GlobalFunctions.printNormal(source.port + ">");
        }

        private static void handleStatInput(String tokens[]) throws IOException {
            if (tokens.length != 4) {
                incorrectInput("stat");
                return;
            }

            short port;
            int id;

            try {
                port = Short.parseShort(tokens[2]);
                id = Integer.parseInt(tokens[3]);
            } catch (NumberFormatException e) {
                incorrectInput("stat");
                return;
            }

            try {
                ArrayList<Integer> parts = torrentClient.stat(tokens[1], port, id);
                GlobalFunctions.printNormal(parts.size() + "");
                for (Integer part : parts) {
                    GlobalFunctions.printNormal(part + " ");
                }
                GlobalFunctions.printNormal("");
            } catch (TimeoutException e) {
                GlobalFunctions.printWarning("no response on `stat` query.");
            }
        }

        private static void handleGetInput(String tokens[]) throws IOException {
            if (tokens.length != 5) {
                incorrectInput("get");
                return;
            }

            short port;
            int id;
            int part;

            try {
                port = Short.parseShort(tokens[2]);
                id = Integer.parseInt(tokens[3]);
                part = Integer.parseInt(tokens[4]);
            } catch (NumberFormatException e) {
                incorrectInput("get");
                return;
            }

            try {
                torrentClient.get(tokens[1], port, id, part);
            } catch (TimeoutException e) {
                GlobalFunctions.printWarning("no response on `stat` query.");
            }
        }

        private static void handleUpdateInput() {
            torrentClient.update();
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
    }
}