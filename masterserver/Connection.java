package masterserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import masterprotocol.*;

/*
 * Will spawn and run each time a new client connects
 */
public class Connection extends Thread {
  final int connectedID;
  Boolean isrunning;
  final BlockingQueue<Message> incomingMessages;
  final private BufferedReader incomingStream;
  MasterProtocol protocol;
  
  public Connection(int connectedID,
                    Boolean isrunning,
                    BlockingQueue<Message> incomingMessages,
                    BufferedReader incomingStream,
                    MasterProtocol protocol) {
    super("Connection: " + connectedID);
    this.connectedID = connectedID;
    this.isrunning = isrunning;
    this.incomingMessages= incomingMessages;
    this.incomingStream = incomingStream;
    this.protocol = protocol;
  }
  
  @Override
  public void run() {  
    System.out.println("Connected to client with ID: " + connectedID);
    String incomingMessage;
    while (isrunning) {
      try {
        incomingMessage = incomingStream.readLine();
        if (incomingMessage != null) {
          try {
            incomingMessages.put(new Message(connectedID, incomingMessage));
          } catch (InterruptedException except) {}
        } else {
          System.out.println(connectedID + " disconnected!");
          protocol.handleDisconnection(connectedID);
          break;
        }
      } catch (IOException except) {
        System.out.println(connectedID + " disconnected!");
        protocol.handleDisconnection(connectedID);
        break;
      }
    }
  }
}