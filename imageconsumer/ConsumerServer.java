/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package imageconsumer;

import masterserver.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;


/**
 *
 * @author Eli
 */
public class ConsumerServer {
  public static String DELIM = " ";
  public static String DELIM2 = "~";
  
  private int port;
  
  boolean isrunning;
  
  private MasterWrapper masterSocket;
  private Producer producer;
  
  private String[] backupList;

  public ConsumerServer(int port, String masterIP, int masterPort) {
    this.port = port;
    isrunning = true;

    masterSocket = new MasterWrapper(masterIP, masterPort);
    producer = new Producer(port, masterSocket, isrunning);
    producer.start();
    
    sendMessage(masterSocket.master,"k "+port);
  }
  
  void start() {
    while (isrunning) {
      String msg = readMessage(masterSocket.master);
      if (msg != null) {
        processMasterMessages(msg);
      } else { //Master disconnected.
        if (backupList.length > 0) {
          String[] newMasterInfo = backupList[0].split(DELIM2);
          updateMaster(newMasterInfo[0],Integer.parseInt(newMasterInfo[1]));
        } else {
          System.out.println("No backup left. Bye.");
          System.exit(-1);
        }
      }
    }
  }
  
  private void processMasterMessages(String msg) {
    System.out.println("Received msg: " + msg);
    if (msg.length() < 1) return;
    try {
      String[] split = msg.split(DELIM);
      switch (split[0]) {
        case "l":
          String[] ip_host = split[1].split(DELIM2);
          updateMaster(ip_host[0],Integer.parseInt(ip_host[1]));
          break;
        case "b":
          backupList = Arrays.copyOfRange(split,1,split.length);
          break;
        default:
          break;
      }
    } catch (ArrayIndexOutOfBoundsException ooB) {
        System.err.println("ConsumerServer.processMasterMessage(): Message not well formatted.");
    }
  }
  
  private void updateMaster(String ip, int port) {
    masterSocket.updateMaster(ip,port);
    sendMessage(masterSocket.master,"k "+port);
    System.out.println("Connected to master: " + ip+DELIM2+port);
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
  
  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      System.err.println("usage: make clienConsumer IP=masters_ip PORT=masters_port");
      System.exit(1);
    }
    String ip = args[0];
    int port = Integer.parseInt(args[1]);
    ConsumerServer cs = new ConsumerServer(1090, ip, port);
    cs.start();
  }
  
}
