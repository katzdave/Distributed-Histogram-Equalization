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
import java.util.concurrent.ConcurrentHashMap;

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
 * @author Eli
 */
public class HistogramConsumer extends Consumer {
  
  public static char INDIVIDUAL = 'i';
  public static char MANY = '#';
  public static char APPLY = 'v';
  
  public static String DELIM = " ";
  public static String READY = "2";
  
  private ConcurrentHashMap<Integer,List<BufferedImage>> clientMap;
  
  public HistogramConsumer(Socket clientSocket, 
                  MasterWrapper masterSocket,
                  ExecutorService executor,
                  ConcurrentHashMap<Integer,List<BufferedImage>> clientMap) {
    super(clientSocket,masterSocket,executor);
    this.clientMap = clientMap;
  }

  @Override
  void processMessage(String msg) {
    switch (msg.charAt(0)) {
      case 'i':
        processImage(clientSocket, msg); //process an individual image
        break;
      case '#':
        splitAndProcessImage(clientSocket, msg); //split image and frequency count it
        break;
      case 'v':
        applyToImage(clientSocket, msg); //get changes to image and apply them
        break;
      default:
        System.err.println("<Consumer> received bad message");
        try {
          clientSocket.close();
        } catch (IOException ioe) {
          System.err.println("<Consumer> Could not close socket");
        }
        updateBusyStatus();
        return;
    }
  }
  
  /*
   * Processes an individual image.
   * 1) Tells client "I'm ready" (READY)
   * 2) Receives image
   * 3) Histogram Equalizes
   * 4) Sends image
   */
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
    img = HistogramEQ.histogramEqualization(img);
    BufferedImage outpImg = copyImage(img);
    try {
      ImageIO.write(outpImg, "PNG", clientSocket.getOutputStream());
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
    System.out.println("<Consumer> sent img.");
    
    updateBusyStatus();
  }
  
  private void splitAndProcessImage(Socket clientSocket, String msg) {
    String[] split = msg.split(DELIM);
    if (split.length < 2) {
      System.err.println("<Consumer> received bad message");
      return;
    }
    int numThreads = Integer.parseInt(split[1]);
    int uniqID = Producer.getUniqueID();
    sendMessage(clientSocket, READY + DELIM + uniqID);
    System.out.println("splitAndProcess(): sent 2");

    BufferedImage img = null;
    try {
      img = ImageIO.read(clientSocket.getInputStream());
    } catch (IOException ioe) {
      ioe.printStackTrace();
      return;
    }
    System.out.println("<Consumer> received img to split");
    
    List<BufferedImage> list = ImageProcessing.splitImage(img,numThreads);
    System.out.println("<Consumer> split img");
    clientMap.put(uniqID,list);//Map each client to a unique ID;
    
    Collection<FreqCalculator> dostuff = new LinkedList<FreqCalculator>();
    for (int i=0; i<numThreads; i++) {
      dostuff.add(new FreqCalculator(list.get(i)));
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
      updateBusyStatus();
      
    } catch (InterruptedException ie) {
      ie.printStackTrace();
    } catch (ExecutionException ee) {
      ee.printStackTrace();
    }    
  }
  
  /*
   * Does intensive frequency calculation on an image
   */
  class FreqCalculator implements Callable<int[]> {
    private BufferedImage img;
    
    FreqCalculator(BufferedImage img) {
      this.img = img;
    }
    
    @Override
    public int[] call() {
      return ImageProcessing.getFrequencyCounts(img);
    }
  }
  
  /*
   * Intensively applies result to image
   */
  class Applyer implements Callable<BufferedImage> {
    private BufferedImage img;
    private int[] valuesToApply;
    
    Applyer(int[] valuesToApply, BufferedImage img) {
      this.img = img;
      this.valuesToApply = valuesToApply;
    }
    
    @Override
    public BufferedImage call() {
      return ImageProcessing.applyValuesToImage(valuesToApply, img);
    }
  }
  
  private void applyToImage(Socket clientSocket, String msg) {
    String[] split = msg.split(DELIM);
    if (split.length < 2) {
      System.err.println("<Consumer> received bad message");
      return;
    }
    int id = Integer.parseInt(split[1]);
    List<BufferedImage> list = clientMap.get(id);
    if (list == null) {
      System.err.println("This client hasn't previously sent images!");
      return;
    }
    sendMessage(clientSocket, READY);
    System.out.println("applyToImage(): sent 2");

    
    String serVec = readMessage(clientSocket);
    System.out.println("Received values to process");
    int[] valuesToApply = ImageProcessing.deserializeVector(serVec);
    
    Collection<Applyer> dostuff = new LinkedList<Applyer>();
    for (int i=0; i<list.size(); i++) {
      dostuff.add(new Applyer(valuesToApply,list.get(i)));
    }
    System.out.println("getting ready to do stuff");
    List<Future<BufferedImage>> applylist = null;
    List<BufferedImage> images = new LinkedList<BufferedImage>();
    try {
      applylist = executor.invokeAll(dostuff);
      for (int i=0; i<applylist.size(); i++) {
        images.add(applylist.get(i).get());
      }
      BufferedImage outpImg = ImageProcessing.mergeImages(images);
      try {
        ImageIO.write(outpImg, "PNG", clientSocket.getOutputStream());
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
      System.out.println("<Consumer> sent img.");
      updateBusyStatus();
      
    } catch (InterruptedException ie) {
      ie.printStackTrace();
    } catch (ExecutionException ee) {
      ee.printStackTrace();
    }    
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
