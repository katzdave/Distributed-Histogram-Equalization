/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package imageclient;

import java.io.*;
import java.net.*;
import java.awt.image.*;
import clientprotocol.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
/**
 *
 * @author David
 */
public class ImageClient {
  //Timeout time limit in milliseconds
  public static final int TIMEOUT = 1000; 
  public static final char BUSY = 'n';
  public static final char READY = '1';
  public static final char OK = 'y';
  public static final char ERROR = '3';
  public static final String DELIM = " ";
  
  //Image information
  public static final String OUT_DIR = "img_output";

  private String masterIP;
  private int masterPort;
  
  private String consumerIP;
  private int consumerPort;
  private Socket consumerSocket;
  private BufferedReader consumerIn;
  private PrintWriter consumerOut;
  
  public ImageClient(String masterIP, int masterPort) {
    this.masterIP = masterIP;
    this.masterPort = masterPort;
  }
  
  /**
   * Tries to open a socket connecting to the master.
   * Tries to tell the master that it is a Client (as opposed to a Consumer)
   * Tries to read a line of data from the master.
   * If that line does not begin with BUSY_MSG, parses the line
   * to get consumerIP and consumerPort
   * @return true on successful location of consumer, false otherwise
   */
  public boolean masterConnect() {
    try {
      Socket masterSocket = new Socket(masterIP, masterPort);
      BufferedReader master_in = new BufferedReader(
            new InputStreamReader(masterSocket.getInputStream()));
      DataOutputStream master_out = new DataOutputStream(masterSocket.getOutputStream());
      
      //Informs the master: "I'm a client. Gimme Consumer Info!"
      master_out.writeBytes("c\n");
      
      String fromServer = master_in.readLine();
      //Don't want to burder Master with multiple connections
      masterSocket.close(); 
      
      String[] splat;
      switch (fromServer.charAt(0)) {
        case ERROR: return false;
        case BUSY:
          System.err.println("Server busy, try again later.");
          return false;
        case OK:
          splat = fromServer.split(DELIM);
          consumerIP = splat[1];
          consumerPort = Integer.parseInt(splat[2]);
          return true;
      }
      return false;
    } catch (IOException ioe) {
      System.err.println("masterConnect(): Problem connecting to " + masterIP);
      return false;
    }
  }
  
  private String[] checkFormat(String filename) {
    String[] ret = new String[2];
    ret[0] = null;
    int index;
    if ((index = filename.lastIndexOf(".png")) > 0) {
      ret[0] = filename.substring(0,index);
      ret[1] = "png";
    } else {
      System.out.println("We can't accept " + filename);
    }
    return ret;
  }

  public boolean consumerTransactionSimple(String[] splitImg){
    String input = splitImg[0] + '.' + splitImg[1];
    String output = splitImg[0] + "_out." + splitImg[1];
    
    try (
      Socket tempConsumerSocket = new Socket(this.consumerIP, this.consumerPort);
    ){
        System.out.println("Connected to: " + tempConsumerSocket);
        BufferedImage img = ImageIO.read(new File(input));
      if (img == null) {
        System.out.println("Couldn't read " + input);
      }
      ImageIO.write(img,splitImg[1],tempConsumerSocket.getOutputStream());
      
      BufferedImage imgFromServer;
      imgFromServer = ImageIO.read(tempConsumerSocket.getInputStream());
      ImageIO.write(imgFromServer,splitImg[1],new File(output));
      return true;
    } catch (IOException ioe) {
      System.err.println("clientConnect(): Problem connecting to " 
                 + this.consumerIP + ": " + this.consumerPort);
      return false;
    }
  }
  
  public static void main(String[] args) throws IOException {
    if (args.length < 3) {
      System.err.println("usage: make runClient IP=masters_ip_address PORT=masters_port IMGS=filename of file with list of images (one img per line)");
      System.exit(0);
    }
    String masterHost = args[0];
    int masterPort = Integer.parseInt(args[1]);
    String filename = args[2];
    try {
      BufferedReader img_list = new BufferedReader(new FileReader(filename));
      ImageClient ic = new ImageClient(masterHost, masterPort);
      ClientProtocol cp = new ClientProtocol(masterHost, masterPort);
      
      String imgname;
      while ((imgname = img_list.readLine()) != null) {
        String[] splitImg = ic.checkFormat(imgname);
        if (splitImg[0] == null) continue; //Not a valid image

        boolean imageSucceeded = false;
        while(!imageSucceeded){
          while(!cp.ConnectToMaster()){
            try {
              Thread.sleep(1000);
            } catch (InterruptedException ex) {
              System.err.println("Error sleeping wut");
              System.exit(1);
            }
          }
          imageSucceeded = cp.ConnectToConsumer(splitImg);
        }
        
//        if(ic.masterConnect()){
//          if(ic.consumerTransactionSimple(splitImg)){
//            System.out.println("Successfully equilized image "+splitImg[0]);
//          } else {
//            System.err.println("Could not process " + splitImg[0]);
//          }
//        } else {
//          System.err.println("Could not connect");
//        }
      }
    } catch (FileNotFoundException fnfe) {
      System.err.println("Could not read file: " + filename);
    }
  }
  
}
