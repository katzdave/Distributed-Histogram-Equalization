/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package clientprotocol;

import java.io.*;
import java.net.*;


/**
 * This class opens a server socket and listens for a backup message
 * @author David
 */
public class BackupListener extends Thread{
  int PortListening;
  
  public BackupListener(int portListening){
    PortListening = portListening;
  }
  
  
  @Override
  public void run(){
    try (
      ServerSocket sSocket = new ServerSocket(PortListening);
      Socket cSocket = sSocket.accept();
      PrintWriter out = new PrintWriter(
        cSocket.getOutputStream(), true);
      BufferedReader in = new BufferedReader(
        new InputStreamReader(cSocket.getInputStream()));
    ) {
      String backupMessage = in.readLine();
    } catch (IOException except) {
      System.err.println("Failed to listen on port " + PortListening);
    }
  }
}
