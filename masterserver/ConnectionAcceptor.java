package masterserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import masterprotocol.*;

public class ConnectionAcceptor extends Thread {
  Boolean isrunning;
  final ConcurrentMap<Integer, Socket> sockets;
  final BlockingQueue<Message> incomingMessages;
  MasterProtocol protocol;
  int serverPort;
  
  public ConnectionAcceptor(int serverPort,
                            Boolean isrunning,
                            ConcurrentMap<Integer, Socket> sockets,
                            BlockingQueue<Message> incomingMessages,
                            MasterProtocol protocol) {
    super("ConnectionAcceptor");
    this.serverPort = serverPort;
    this.isrunning= isrunning;
    this.sockets = sockets;
    this.incomingMessages = incomingMessages;
    this.protocol = protocol;
  }
  
  @Override
  public void run() {
    ServerSocket sSocket = null;
    try {
      sSocket = new ServerSocket(serverPort);
    } catch (IOException except) {
      System.err.println("Failed to listen on port " + serverPort + "!");
    }
    
    int numConnections = 0;
    System.out.println("Connection Acceptor running");
    while (isrunning){
      try {
        Socket cSocket = sSocket.accept();
        if (cSocket == null) {
          System.err.println("Could not receive connection");
          continue;
        }
        BufferedReader incomingStream = null;
        try {
          incomingStream = new BufferedReader(
                  new InputStreamReader(cSocket.getInputStream()));
        } catch (IOException except) {
          System.err.println("Problem getting istream from connection");
        }
        sockets.put(numConnections, cSocket);

        if (protocol.processAcceptorMessages(numConnections, 
                                             incomingStream, 
                                             cSocket)
            != true)
          continue;

        Connection connection;
        connection = new Connection(numConnections,
                                    isrunning,
                                    incomingMessages,
                                    incomingStream,
                                    protocol);
        ++numConnections;
        connection.start();
      } catch (IOException except) {
        System.err.println("Failed to accept incoming connection!");
      }
    }
  }
  
}