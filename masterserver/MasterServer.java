package masterserver;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MasterServer {
  public static void main(String [] args) {
    int serverPort = 0, leadPort = 0;
    try {
      serverPort = Integer.parseInt(args[0]);
      leadPort = Integer.parseInt(args[2]);
    } catch (NumberFormatException e) {
      System.err.println("not a valid number of masters!");
      System.exit(1);
    }

    String leadIP = args[1];
    ConnectionManager masterServer;
    try {
      masterServer = new ConnectionManager(serverPort, leadIP, leadPort);
      masterServer.runManager();
    } catch (IOException ex) {
      System.err.println("Could not start up server!");
    } catch (InterruptedException ex) {
      System.err.println("Could not start up server!");
    }
  }
}
