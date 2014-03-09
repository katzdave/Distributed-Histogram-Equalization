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
import java.io.InputStreamReader;
import java.io.IOException;
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

    masterSocket = new MasterWrapper();
    updateMaster(masterIP,masterPort);
    
    producer = new Producer(port, masterSocket, isrunning);
    producer.start();
    
  }
  
  void start() {
    while (isrunning) {
      String msg = masterSocket.readMessage();
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
  
  private void updateMaster(String masterIP, int masterPort) {
    masterSocket.updateMaster(masterIP,masterPort);
    double load = 3.2; // Put SIGAR load here.
    masterSocket.sendMessage("k"+DELIM+port+DELIM+load);
    System.out.println("Connected to master: " + masterIP+DELIM2+masterPort);
  }
  
  public static void main(String[] args) throws IOException {
    if (args.length < 3) {
      System.err.println("usage: make clienConsumer IP=masters_ip PORT=masters_port MYPORT=myport");
      System.exit(1);
    }
    String masterIP = args[0];
    int masterPort = Integer.parseInt(args[1]);
    int port = Integer.parseInt(args[2]);
    ConsumerServer cs = new ConsumerServer(port, masterIP, masterPort);
    cs.start();
  }
  
}
