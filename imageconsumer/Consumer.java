/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package imageconsumer;

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

/**
 *
 * @author David
 */
public class Consumer extends Thread {
  private MasterWrapper masterSocket;
  private boolean isrunning;
  private final BlockingQueue<Socket> clientInfo;
  
  public Consumer(boolean isrunning, 
    MasterWrapper masterSocket, 
    BlockingQueue<Socket> clientInfo) {
    super("Consumer");
    this.masterSocket = masterSocket;
    this.isrunning = isrunning;
    this.clientInfo = clientInfo;
  }
  
  @Override
  public void run() {
    System.out.println("Consumer running.");
    int i = 0;
    while (isrunning) {
      try {
	Socket clientSocket = clientInfo.take();
      BufferedReader clientIn = new BufferedReader(
        new InputStreamReader(clientSocket.getInputStream()));
      DataOutputStream clientOut =
        new DataOutputStream(clientSocket.getOutputStream());

      String res = clientIn.readLine();
      System.out.println("<Consumer> received: " + res);
      if(res.charAt(0) != 'i') {
	      System.err.println("<Consumer> client not ready");
	      clientSocket.close();
	      updateBusyStatus();
	      continue;
      }
      clientOut.writeBytes("2\n");
      System.out.println("<Consumer> sent out: 2");
      
	BufferedImage img = ImageIO.read(clientSocket.getInputStream());
	System.out.println("<Consumer> received img: " + img);
	img = HistogramEQ.histogramEqualization(img);
	BufferedImage outpImg = copyImage(img);

        ImageIO.write(outpImg, "PNG", clientSocket.getOutputStream());
	System.out.println("<Consumer> sent img: " + outpImg);
	
	updateBusyStatus();
        clientSocket.close();
      } catch (InterruptedException ie) {
        System.err.println("Consumer.run(): Problem getting client socket");
      } catch (IOException ioe) {
        System.err.println("Consumer.run(): Problem reading or writing");
      }
    }
  }
  private void updateBusyStatus() {
        if (Producer.availCores.get() == 0) {
          ConsumerServer.sendMessage(masterSocket.master,"a");
        }
        Producer.availCores.getAndIncrement();
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
