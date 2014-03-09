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
public class Consumer implements Runnable {
  private MasterWrapper masterSocket;
  private Socket clientSocket;
  
  public Consumer(Socket clientSocket, MasterWrapper masterSocket) {
    this.clientSocket = clientSocket;
    this.masterSocket = masterSocket;
  }
  
  @Override
  public void run() {
    System.out.println("Consumer received connection.");
    try {
      //See what the consumer wants me to do.
      String res = ConsumerServer.readMessage(clientSocket);
      System.out.println("<Consumer> received: " + res);
      if(res.charAt(0) != '#') {
	      System.err.println("<Consumer> client not ready");
	      clientSocket.close();
	      updateBusyStatus();
	      return;
      }
      
      //Tell the Consumer "I'm ready to do what you want."
      ConsumerServer.sendMessage(clientSocket, "2");
      System.out.println("<Consumer> sent out: 2");
      
      BufferedImage img = ImageIO.read(clientSocket.getInputStream());
      System.out.println("<Consumer> received img");
      img = HistogramEQ.histogramEqualization(img);
      BufferedImage outpImg = copyImage(img);
      
      ImageIO.write(outpImg, "PNG", clientSocket.getOutputStream());
      System.out.println("<Consumer> sent img.");
      
      updateBusyStatus();
      clientSocket.close();
    } catch (IOException ioe) {
      ioe.printStackTrace();
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
