/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package imageconsumer;

import masterserver.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.List;

import java.awt.image.BufferedImage;


/**
 *
 * @author Eli
 */
public class Producer extends Thread {
  public static final String DELIM = " ";
	public static final int NUM_THREADS = 4;
  static AtomicInteger availCores;
  static AtomicInteger uniqueID;
  
  private ExecutorService executorPool;
  private ServerSocket sSocket;
  
  boolean isrunning;
  
  private MasterWrapper masterSocket;
  private MasterTalker masterComm;
  
  private ConcurrentHashMap<Integer,List<BufferedImage>> clientMap;

  public Producer(int port, String masterIP, int masterPort) {
    
    //Number of available CubbyConsumers
    availCores = new AtomicInteger(NUM_THREADS);
    uniqueID = new AtomicInteger(0);
    
    sSocket = null;
    this.masterSocket = new MasterWrapper();
    masterComm = new MasterTalker(port,masterPort,masterIP,masterSocket);
    
    clientMap = new ConcurrentHashMap<Integer,List<BufferedImage>>();
    executorPool = Executors.newCachedThreadPool();
	  executorPool.execute(masterComm);
    
    try {
      sSocket = new ServerSocket(port);
    } catch (IOException e) {
      System.err.println("Problem creating serverSocket ");
      System.exit(0);
    }
    
    
    isrunning = true;
    for (int i=0; i<NUM_THREADS; i++) {
      masterSocket.sendMessage("a"+DELIM+masterSocket.getLoad());
    }
  }
  
  @Override
  public void run() {
    while (isrunning) {
      try {
        Socket client = sSocket.accept();
        System.out.println("Producer connected to client: " + client);
        executorPool.execute(new Consumer(client, 
                                          masterSocket, 
                                          clientMap,
                                          executorPool));
        availCores.getAndDecrement();
        if(availCores.get() > 0) {
          //tell Master we're not too busy.
          masterSocket.sendMessage("a"+DELIM+masterSocket.getLoad());
        }
      } catch (IOException ioe) {
        System.err.println("Producer.start(): Problem accepting client");
      }
    }
  } 
  
  public static int getUniqueID() {
    return uniqueID.getAndIncrement();
  }
  
  public static void main(String[] args) throws IOException {
    if (args.length < 3) {
      System.err.println("usage: make runConsumer IP=masters_ip PORT=masters_port MYPORT=myport");
      System.exit(1);
    }
    String masterIP = args[0];
    int masterPort = Integer.parseInt(args[1]);
    int port = Integer.parseInt(args[2]);
    Producer cs = new Producer(port, masterIP, masterPort);
    cs.start();
  }
}
