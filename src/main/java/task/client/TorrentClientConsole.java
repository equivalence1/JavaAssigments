package task.client;

import task.GlobalFunctions;

import java.io.*;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

/**
 * Created by equi on 14.03.16.
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
            GlobalFunctions.printInfo("Type 'Sources <file id>' to get list of seeds for this file.");
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
                    case ("get"):
                        //handleGetInput(tokens, in, out);
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
            GlobalFunctions.printlnNormal("" + files.size());
            files.forEach(ClientSideProtocol::printListEntry);
        }

        private static void printListEntry(TorrentClient.ListResponseEntry file) {
            GlobalFunctions.printlnNormal("file id: " + file.id);
            GlobalFunctions.printlnNormal("file name: " + file.name);
            GlobalFunctions.printlnNormal("file size: " + file.size);
            GlobalFunctions.printlnNormal("");
        }

        private static void handleUploadInput(String tokens[]) throws IOException, TimeoutException {
            if (tokens.length != 2) {
                incorrectInput("upload");
                return;
            }

            try {
                int id = torrentClient.upload(tokens[1]);
                GlobalFunctions.printlnNormal("id for file '" + tokens[1] + "' is " + id);
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
                ArrayList<TorrentClient.SourceResponseEntry> sources = torrentClient.source(id);
                GlobalFunctions.printlnNormal(sources.size() + "");
                sources.forEach(ClientSideProtocol::printSourceEntry);
            } catch (NumberFormatException e) {
                GlobalFunctions.printWarning(tokens[1] + " is not an integer.");
            }
        }

        private static void printSourceEntry(TorrentClient.SourceResponseEntry source) {
            GlobalFunctions.printNormal("<");
            for (int i = 0; i < 4; i++) {
                if (i != 3)
                    GlobalFunctions.printNormal(source.ip[i] + ".");
                else
                    GlobalFunctions.printNormal(source.ip[i] + ":");
            }
            GlobalFunctions.printlnNormal(source.port + ">");
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
