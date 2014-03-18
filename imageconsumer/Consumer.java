/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package imageconsumer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.DataOutputStream;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

/**
 *
 * @author Eli
 */
public class Consumer implements Runnable {
  
  public static String DELIM = " ";
  public static String READY = "2";
  
  MasterWrapper masterSocket;
  Socket clientSocket;
  ExecutorService executor;
  
  public Consumer(Socket clientSocket, 
                  MasterWrapper masterSocket,
                  ExecutorService executor) {
    this.clientSocket = clientSocket;
    this.masterSocket = masterSocket;
    this.executor = executor;
  }
  
  @Override
  public void run() {
    System.out.println("Consumer received connection.");
    
    //See what the consumer wants me to do.
    String res = readMessage(clientSocket);
    System.out.println("<Consumer> received: " + res);
    processMessage(res);
  }
  
  void processMessage(String msg) {
    /* Override in subclass */
  }
  
  void updateBusyStatus() {
    if (Producer.availCores.get() == 0) {
      masterSocket.sendMessage("a"+DELIM+masterSocket.getLoad());
    }
    Producer.availCores.getAndIncrement();
  }
  
  public static void sendMessage(Socket s, String message) {
    try {
      DataOutputStream ostream = new DataOutputStream(s.getOutputStream());
      ostream.writeBytes(message + '\n');
    } catch (IOException except) {
      System.err.println("Failed to create output stream for socket " + s);
    }
  }
  
	public static String readMessage(Socket s) {
    String readString = "";
    try {
      BufferedReader istream = new BufferedReader(
                                                  new InputStreamReader(s.getInputStream()));
      readString = istream.readLine();
    } catch (IOException IOException) {
      System.err.println("Problem reading message.");
    }
    return readString;
  }
}
