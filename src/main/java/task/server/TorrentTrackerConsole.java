package task.server;

import task.GlobalFunctions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by equi on 17.04.16.
 *
 * @author Kravchenko Dima
 */
public class TorrentTrackerConsole {
    public static void main(String args[]) throws IOException {
        BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));

        TorrentTracker server = new TorrentTracker();

        String userInput;
        while ((userInput = stdIn.readLine()) != null) {
            switch(userInput) {
                case "start":
                    if (server.start()) {
                        GlobalFunctions.printSuccess("server was started");
                    } else {
                        GlobalFunctions.printWarning("server was not started");
                    }
                    break;
                case "stop":
                    server.stop();
                    GlobalFunctions.printSuccess("server was stopped");
                    break;
                default:
                    printHelp();
            }
        }

        server.stop();
    }

    private static void printHelp() {
        GlobalFunctions.printInfo("start -- start server\nstop -- stop server\n");
    }
}
