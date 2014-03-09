/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package imageprocessing;

import java.awt.image.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import javax.imageio.ImageIO;

/**
 *
 * @author David
 */
public class ClientImageContainer implements Runnable{
  String ConsumerIP;
  int ConsumerPort;
  int CoresAllowed;
  public BufferedImage Image;
  String ImageType;
  boolean isFirstConnection;
  public int[] FrequencyCounts;
  
  public ClientImageContainer(
          String ip, int port, int cores, BufferedImage img, String type){
    ConsumerIP = ip;
    ConsumerPort = port;
    CoresAllowed = cores;
    Image = img;
    ImageType = type;
    isFirstConnection = true;
    FrequencyCounts = new int[256];
  }
  
  @Override
  public void run(){
    if(isFirstConnection){
      try (
        Socket tempConsumerSocket = new Socket(ConsumerIP, ConsumerPort);
      ){
        System.out.println("Connected to: " + ConsumerIP);

        BufferedReader consumerIn = new BufferedReader(
          new InputStreamReader(tempConsumerSocket.getInputStream()));
        DataOutputStream consumerOut =
          new DataOutputStream(tempConsumerSocket.getOutputStream());

        consumerOut.writeBytes(CoresAllowed + "\n");
        //System.out.println("<Client> sent out: i");
        String res = consumerIn.readLine();
        //System.out.println("<Client> received in: " + res);
        if(res.charAt(0) != '2') {
          System.err.println("<Client> consumer not ready");
          System.exit(-1);
        }

        ImageIO.write(Image, ImageType, tempConsumerSocket.getOutputStream());
        res = consumerIn.readLine();
        FrequencyCounts = ImageProcessing.deserializeVector(res);

      } catch (IOException ioe) {
        System.err.println("clientConnect(): Problem connecting to " 
                   + ConsumerIP + ": " + ConsumerPort);
        System.exit(1);
      }
      isFirstConnection = false;
    }
    else{
      try (
        Socket tempConsumerSocket = new Socket(ConsumerIP, ConsumerPort);
      ){
        System.out.println("Connected to: " + ConsumerIP);

        BufferedReader consumerIn = new BufferedReader(
          new InputStreamReader(tempConsumerSocket.getInputStream()));
        DataOutputStream consumerOut =
          new DataOutputStream(tempConsumerSocket.getOutputStream());

        consumerOut.writeBytes("v\n");
        //System.out.println("<Client> sent out: i");
        String res = consumerIn.readLine();
        //System.out.println("<Client> received in: " + res);
        if(res.charAt(0) != '2') {
          System.err.println("<Client> consumer not ready");
          System.exit(-1);
        }

        consumerOut.writeBytes(ImageProcessing.serializeVector(FrequencyCounts));
        
        BufferedImage imgFromServer;
        imgFromServer = ImageIO.read(tempConsumerSocket.getInputStream());
        if(imgFromServer == null){
          System.err.println("Error image rcvd null");
          System.exit(-1);
        }

      } catch (IOException ioe) {
        System.err.println("clientConnect(): Problem connecting to " 
                   + ConsumerIP + ": " + ConsumerPort);
        System.exit(1);
      }
    }
  }
}
  
  
