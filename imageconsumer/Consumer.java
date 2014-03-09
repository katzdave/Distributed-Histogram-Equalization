/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package imageconsumer;

import imageprocessing.*;

import java.util.List;
import java.util.LinkedList;
import java.util.Collection;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import javax.imageio.ImageIO;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author David
 */
public class Consumer implements Runnable {
  
  public static char INDIVIDUAL = 'i';
  public static char MANY = '#';
  public static char APPLY = 'v';
  
  public static String DELIM = " ";
  public static String READY = "2";
  
  private MasterWrapper masterSocket;
  private Socket clientSocket;
  private ExecutorService executor;
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
    try {
      //See what the consumer wants me to do.
      String res = readMessage(clientSocket);
      System.out.println("<Consumer> received: " + res);
      if(res.charAt(0) != '#') {
	      
      }
      switch (res.charAt(0)) {
        case 'i':
          processImage(clientSocket, res);
          break;
        case '#':
          splitAndProcessImage(clientSocket, res);
          break;
        case 'v':
          applyToImage(clientSocket, res);
          break;
        default:
          System.err.println("<Consumer> received bad message");
          clientSocket.close();
          updateBusyStatus();
          return;
      }
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }
  
  private void processImage(Socket clientSocket, String msg) {
    //Tell the Consumer "I'm ready to do what you want."
    sendMessage(clientSocket, READY);
    System.out.println("<Consumer>_processImage() sent out: 2");
    BufferedImage img = null;
    try {
      img = ImageIO.read(clientSocket.getInputStream());
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }
    
    System.out.println("<Consumer> received img");
    //img = HistogramEQ.histogramEqualization(img);
    BufferedImage outpImg = copyImage(img);
    try {
      ImageIO.write(outpImg, "PNG", clientSocket.getOutputStream());
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
    System.out.println("<Consumer> sent img.");
    
    updateBusyStatus();
    
    try {
      clientSocket.close();
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }
  }
  
  private void splitAndProcessImage(Socket clientSocket, String msg) {
    String[] split = msg.split(DELIM);
    if (split.length < 2) {
      System.err.println("<Consumer> received bad message");
      return;
    }
    int numThreads = Integer.parseInt(split[1]);
    sendMessage(clientSocket, READY);
    BufferedImage img = null;
    try {
      img = ImageIO.read(clientSocket.getInputStream());
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }
    
    System.out.println("<Consumer> received img");
    List<BufferedImage> list = ImageProcessing.splitImage(img,numThreads);
    Collection<DoStuff> dostuff = new LinkedList<DoStuff>();
    for (int i=0; i<numThreads; i++) {
      dostuff.add(new DoStuff(list.get(i)));
    }
    List<Future<int[]>> freqlist = null;
    try {
      freqlist = executor.invokeAll(dostuff);
      int[] sum = freqlist.get(0).get();
      for (int i=1; i<numThreads; i++) {
        sum = ImageProcessing.sumColorFreqs(freqlist.get(i).get(),sum);
      }
      String sendString = ImageProcessing.serializeVector(sum);
      sendMessage(clientSocket, sendString);
      
    } catch (InterruptedException ie) {
      ie.printStackTrace();
    } catch (ExecutionException ee) {
      ee.printStackTrace();
    }
    
    
  }
  class DoStuff implements Callable<int[]> {
    private BufferedImage img;
    
    DoStuff(BufferedImage img) {
      this.img = img;
    }
    
    @Override
    public int[] call() {
      return ImageProcessing.getFrequencyCounts(img);
    }
  }
  
  private void applyToImage(Socket clientSocket, String msg) {
    
  }
  
  private void updateBusyStatus() {
    if (Producer.availCores.get() == 0) {
      masterSocket.sendMessage("a");
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
  
  public static BufferedImage copyImage(BufferedImage source){
    BufferedImage b = new BufferedImage(source.getWidth(), 
                                        source.getHeight(), 
                                        BufferedImage.TYPE_INT_ARGB);
    Graphics g = b.getGraphics();
    g.drawImage(source, 0, 0, null);
    g.dispose();
    return b;
  }
}
