package task.server;

import task.GlobalFunctions;

import java.util.Scanner;

/**
 * Created by equi on 24.04.16.
 *
 * @author Kravchenko Dima
 */
public class TorrentTrackerConsole {

    public static void main(String args[]) {
        TorrentTracker tracker = new TorrentTracker();
        Scanner in = new Scanner(System.in);

        boolean wasStopped = false;

        while (in.hasNextLine() && !wasStopped) {
            String command = in.nextLine();
            switch (command) {
                case "start":
                    if (tracker.status == TorrentTracker.TrackerStatus.RUNNING) {
                        GlobalFunctions.printNormal("Server is already running");
                    } else {
                        tracker.start();
                        GlobalFunctions.printSuccess("Server started");
                    }
                    break;
                case "stop":
                    wasStopped = true;
                    break;
                default:
                    printUsage();
                    break;
            }
        }

        tracker.stop();
        GlobalFunctions.printSuccess("Server stopped. Good buy.");
    }

    private static void printUsage() {
        GlobalFunctions.printInfo("type `start` to start server.");
        GlobalFunctions.printInfo("type `stop` to start server.");
    }

}
