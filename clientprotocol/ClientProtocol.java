/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package clientprotocol;

import java.io.*;
import java.net.*;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;
import imageprocessing.*;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author David
 */
public class ClientProtocol {
  //public static final int TIMEOUT = 1000; 
  public static final char BUSY = 'n';
  //public static final char READY = '1';
  public static final char OK = 'y';
  //public static final char ERROR = '3';
  public static final String DELIM = " ";
  public static final String DELIM2 = "~";
  
  ConnectionInfo Master;
  ConnectionInfo Consumer;
  List<ConnectionInfo> Backups;
  List<ConnectionInfo> Consumers;
  public boolean SingleConsumer;
  
  public ClientProtocol(String masterHost, int masterPort){
    Master = new ConnectionInfo(masterHost, masterPort);
    Backups = new ArrayList<>();
    Consumers = new ArrayList<>();
    //(new Thread(new BackupListener(BackupPort))).start();
  }
  
  public boolean ConnectToMaster(long filesize){
    System.out.println("Attempting to connect to master");

    String backupString;
    String consumerString;
    try {
      Socket masterSocket = new Socket(Master.HostName, Master.Port);
      BufferedReader master_in = new BufferedReader(
        new InputStreamReader(masterSocket.getInputStream()));
      DataOutputStream master_out =
        new DataOutputStream(masterSocket.getOutputStream());
      
      //Informs the master: "I'm a client. Gimme Consumer Info!"
      master_out.writeBytes("c " + filesize + "\n");
      
      //Master sends most updated list of backups
      backupString = master_in.readLine();

      System.out.println(backupString);
      
      if(backupString != null){
        if(backupString.charAt(0) == 'l'){
          System.out.println("correctly went in");
          masterSocket.close();
          UpdateMasterFromLeaderString(backupString);
          return ConnectToMaster(filesize);
        }
      }

      //Master sends an available consumer or BUSY if none
      consumerString = master_in.readLine();
      System.out.println(consumerString);
      
    } catch (IOException ioe) {
      System.err.println("masterConnect(): Problem connecting to " + Master.HostName);
      return ConnectToBackup(filesize);
    }
    
    if(backupString == null || consumerString == null){
      //This should only happen if the master dies before giving a message
      if(backupString != null){
        UpdateBackupsList(backupString);
      }
      return ConnectToBackup(filesize);
    }
    
    UpdateBackupsList(backupString);
    return ParseConsumerString(consumerString);
  }
  
  private boolean ConnectToBackup(long filesize){
    boolean isLeader;
    for(int i=0; i<Backups.size(); i++){
      String backupString;
      String consumerString;
      try {
        Socket masterSocket =
          new Socket(Backups.get(i).HostName, Backups.get(i).Port);
        BufferedReader master_in = new BufferedReader(
          new InputStreamReader(masterSocket.getInputStream()));
        DataOutputStream master_out =
          new DataOutputStream(masterSocket.getOutputStream());

        //Informs the master: "I'm a client. Gimme Consumer Info!"
        master_out.writeBytes("c\n");

        //Backup sends most updated list of backups (if leader) or backups
        backupString = master_in.readLine();
        System.out.println(backupString);

        if(backupString == null)
          continue;
        else if(backupString.charAt(0) == 'l')
          isLeader = false;
        else if(backupString.charAt(0) == 'b')
          isLeader = true;
        else {
          System.err.println("I have no idea what sorcery");
          continue;
        }
        
        //Master sends an available consumer or BUSY if none
        if(!isLeader)
          masterSocket.close();


        System.out.println("Mother fucking consumer string");
        consumerString = master_in.readLine();
        System.out.println(consumerString);

      } catch (IOException ioe) {
        continue;
      }

      if(isLeader){
        if(consumerString == null){
          continue;
        }

        //Successfully found a working backup!
        //Update MasterServer
        //Return whether or not its busy
        Master = new ConnectionInfo(Backups.get(i).HostName, Backups.get(i).Port);
        return ParseConsumerString(consumerString);
      }
      else {
        UpdateMasterFromLeaderString(backupString);
        return ConnectToMaster(filesize);
      }
    } 
    
    System.err.println("Error: Master died and no working backups");
    System.exit(1);
    return false;
  }
  
  public boolean ConnectToConsumer(String[] splitImg){
    String input = splitImg[0] + '.' + splitImg[1];
    String output = splitImg[0] + "_out." + splitImg[1];
    
    try (
      Socket tempConsumerSocket = new Socket(Consumer.HostName, Consumer.Port);
    ){
      System.out.println("Connected to: " + tempConsumerSocket);
      BufferedImage img = ImageIO.read(new File(input));
      if (img == null) {
        // Bad image... is it even possible for it to get this far?
        System.out.println("Couldn't read " + input);
      }
      BufferedReader consumerIn = new BufferedReader(
        new InputStreamReader(tempConsumerSocket.getInputStream()));
      DataOutputStream consumerOut =
        new DataOutputStream(tempConsumerSocket.getOutputStream());
      
      consumerOut.writeBytes("i\n");
      System.out.println("<Client> sent out: i");
      String res = consumerIn.readLine();
      System.out.println("<Client> received in: " + res);
      if(res.charAt(0) != '2') {
	      System.err.println("<Client> consumer not ready");
	      System.exit(-1);
      }

      ImageIO.write(img,splitImg[1],tempConsumerSocket.getOutputStream());
      System.out.println("<Client> sent img: "+img);
      
      BufferedImage imgFromServer;
      imgFromServer = ImageIO.read(tempConsumerSocket.getInputStream());
      System.out.println("<Client> received img: " +imgFromServer);
      if(imgFromServer == null){
        return false;
      }
      
      ImageIO.write(imgFromServer,splitImg[1],new File(output));
      System.out.println("<Client> wrote file: " + output);
      return true;
    } catch (IOException ioe) {
      System.err.println("clientConnect(): Problem connecting to " 
                 + Consumer.HostName + ": " + Consumer.Port);
      return false;
    }
  }
  
  @SuppressWarnings("empty-statement")
  public boolean ConnectToConsumers(String[] splitImg){
    List<ClientImageContainer> containers = new ArrayList<>();
    List<BufferedImage> splitImages;
    
    String input = splitImg[0] + '.' + splitImg[1];
    String output = splitImg[0] + "_out." + splitImg[1];
    
    BufferedImage image = null;
    try{
      image = ImageIO.read(new File(input));
    }catch(IOException ioe){
      System.err.println("Bad Image");
      System.exit(1);
    }
    
    splitImages = ImageProcessing.splitImage(image, Consumers.size());
    ExecutorService executor = Executors.newFixedThreadPool(Consumers.size()+1);
    
    for(int i=0; i<Consumers.size(); i++){
      ConnectionInfo info = Consumers.get(i);
      containers.add(new ClientImageContainer(
              info.HostName,info.Port,info.NumCores,
              splitImages.get(i),splitImg[1]));
      executor.execute(containers.get(i));
    }
    executor.shutdown();
    while(!executor.isTerminated()){
      ;
    }
    
    int[] frequencyCounts = new int[256];
    Arrays.fill(frequencyCounts, 0);
    frequencyCounts = ImageProcessing.sumColorFreqsFromCIC(containers);
    
//    for(int i=0; i<containers.size(); i++){
//      frequencyCounts = ImageProcessing.sumColorFreqs(
//              frequencyCounts, containers.get(i).FrequencyCounts);
//    }
    frequencyCounts = ImageProcessing.equalizeFreqs(
            frequencyCounts, (long)image.getHeight()*(long)image.getWidth());
    
    executor = Executors.newFixedThreadPool(Consumers.size()+1);
    
    for(int i=0; i<containers.size(); i++){
      containers.get(i).FrequencyCounts = frequencyCounts;
      executor.execute(containers.get(i));
    }
    executor.shutdown();
    while(!executor.isTerminated()){
      ;
    }
    
    image = ImageProcessing.mergeImagesFromCIC(containers);
    // Write image here!

    return true;
  }
  
  private boolean ParseConsumerString(String cs){
    switch (cs.charAt(0)) {
      case BUSY:
        System.err.println("Server busy, try again later.");
        return false;
      case OK:
        String[] splat = cs.split(DELIM);
        if(splat.length == 2){
          String[] ip_port = splat[1].split(DELIM2);
          Consumer = new ConnectionInfo(ip_port[0], Integer.parseInt(ip_port[1]));
          SingleConsumer = true;
        }
        else{
          for(int i=1; i<splat.length; i++){
            Consumers.clear();
            String[] ip_port = splat[i].split(DELIM2);
            ConnectionInfo CI = new ConnectionInfo(
                    ip_port[0], Integer.parseInt(ip_port[1]));
            if(Consumers.contains(CI)){
              int indx = Consumers.indexOf(CI);
              Consumers.get(indx).NumCores += 1;
            }
            else{
              Consumers.add(CI);
            }            
            SingleConsumer = false;
          }
        }
        return true;
    }
    System.err.println("I have no idea how we got here (ParseConsumerString)");
    return false;
  }

  private void UpdateMasterFromLeaderString(String leaderString){
    String splat[], again[];
    splat = leaderString.split(" ");
    again = splat[1].split("~");
    Master.HostName = again[0];
    Master.Port = Integer.parseInt(again[1]);
  }
  
  private void UpdateBackupsList(String backupString){
    Backups.clear();
    String[] splat = backupString.split(DELIM);
    for(int i=1; i<splat.length; i++){
      String[] hostPort = splat[i].split(DELIM2);
      ConnectionInfo ci = new ConnectionInfo(
        hostPort[0], Integer.parseInt(hostPort[1]));
      Backups.add(ci);
    }
  }
}
