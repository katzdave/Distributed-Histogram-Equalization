/*
 *                   ConnectionManager
 *                    /   |   \
 *   ConnectionMessenger  |   ServerLogic (done by main thread)
 *                 ConnectionAcceptor
 *                        |
 * spawns multiple threads (one for each connection)
 *
 */

package masterserver;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import masterprotocol.*;

/**
 * @author Harrison
 * This is the master server class that pairs up connections
 */
public class ConnectionManager {
  Boolean isrunning;
  final ConcurrentMap<Integer, Socket> sockets;
  final BlockingQueue<Message> incomingMessages;
  final BlockingQueue<Message> outgoingMessages;
  final MasterProtocol protocol;
  ConnectionAcceptor scAcceptor;
  ConnectionMessenger sMessenger;

  public ConnectionManager(int serverPort, String leadIP, int leadPort) 
          throws IOException, InterruptedException{
    isrunning = true;
    sockets = new ConcurrentHashMap<>();
    incomingMessages = new LinkedBlockingQueue<>();
    outgoingMessages = new LinkedBlockingQueue<>();
    protocol = new MasterProtocol(serverPort,
                                  leadIP,
                                  leadPort,
                                  isrunning,
                                  sockets, 
                                  incomingMessages, 
                                  outgoingMessages);
    scAcceptor = new ConnectionAcceptor(serverPort,
                                        isrunning, 
                                        sockets,
                                        incomingMessages,
                                        protocol);
    sMessenger = new ConnectionMessenger(isrunning,
                                         sockets,
                                         outgoingMessages);
  }
  
  public void runManager() {
    scAcceptor.start();
    sMessenger.start();
    System.out.println("Connection Manager running");
    while (isrunning) {
      try {
        protocol.processManagerMessages();
      } catch (InterruptedException except) {
        System.out.println("Interrupted");
      }
    }
  }
}
