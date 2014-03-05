package masterserver;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import masterprotocol.*;

/**
 * handles messaging all connections
 */
public class ConnectionMessenger extends Thread {
  Boolean isrunning;
  final ConcurrentMap<Integer, Socket> sockets;
  final BlockingQueue<Message> outgoingMessages;
  DataOutputStream ostream;
  
  public ConnectionMessenger(Boolean isrunning,
                             ConcurrentMap<Integer, Socket> sockets,
                             BlockingQueue<Message> outgoingMessages) {
    this.isrunning = isrunning;
    this.sockets = sockets;
    this.outgoingMessages = outgoingMessages;
  }
  
  void sendMessage(Socket s, Message m) {
    try {
      ostream = new DataOutputStream(s.getOutputStream());
      ostream.writeBytes(m.message + '\n');
    } catch (IOException except) {
      System.err.println("Failed to create output stream for socket "
              + "or send message to connection with ID: " + m.connectedID);
    }
  }
  
  @Override
  public void run() {
    System.out.println("Outgoing Messages running");
    while (isrunning) {
      try {
        
        Message outgoingMessage = outgoingMessages.take();
        if (sockets.containsKey(outgoingMessage.connectedID))
          sendMessage(sockets.get(outgoingMessage.connectedID), outgoingMessage);
        
      } catch (InterruptedException ex) {
        System.err.println("Problem getting message from outgoingMessages!");
      }
    }
  }
}
