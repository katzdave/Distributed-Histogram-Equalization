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
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Eli
 */
public class Producer extends Thread {
	protected static final int THREAD_POOL_SIZE = 2;
	protected static final int MAX_POOL_SIZE = 4;
	protected static final int KEEP_ALIVE_TIME = 10;
	protected static final int WORK_Q_SIZE = 2;	
  static AtomicInteger availCores;
  private BlockingQueue<Socket> clientInfo;
  private ServerSocket sSocket;
  
  boolean isrunning;
  
  private MasterWrapper masterSocket;
	private ThreadPoolExecutor executorPool;
  public Producer(int myPort, MasterWrapper masterSocket, boolean isrunning) {
    
    //Number of available CubbyConsumers
    availCores = new AtomicInteger(1);
      //Runtime.getRuntime().availableProcessors() - 1);
    
    clientInfo = new LinkedBlockingQueue<Socket>();
    sSocket = null;
    this.masterSocket = masterSocket;
    try {
      sSocket = new ServerSocket(myPort);
    } catch (IOException e) {
      System.err.println("Problem creating serverSocket ");
      System.exit(0);
    }
    ThreadPoolExecutor executorPool = new ThreadPoolExecutor(THREAD_POOL_SIZE,
															 MAX_POOL_SIZE, 
															 KEEP_ALIVE_TIME, 
															 TimeUnit.SECONDS,
															 new LinkedBlockingQueue<Runnable>());
	  
    this.isrunning = isrunning;
    for (int i=0; i<availCores.get(); i++) {
      executorPool.execute(new Consumer(isrunning, masterSocket, clientInfo));
    }
  }
  
  @Override
  public void run() {
    while (isrunning) {
      try {
        Socket client = sSocket.accept();
        System.out.println("Producer connected to client: " + client);
        availCores.getAndDecrement();
	clientInfo.put(client);
        
        if(availCores.get() > 0) {
          //tell Master we're not too busy.
          ConsumerServer.sendMessage(masterSocket.master,"a");
        }
        clientInfo.put(client);
        
      } catch (IOException ioe) {
        System.err.println("Producer.start(): Problem accepting client");
      } catch (InterruptedException ie) {
        System.err.println("Producer.start(): Problem putting clientInfo on queue");
      }
    }
  }    
  
}
