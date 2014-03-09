package imageconsumer;

import java.net.Socket;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class MasterWrapper {
	Socket master = null;
  DataOutputStream ostream;
  BufferedReader istream;
  
  public MasterWrapper() {
    //Call updateMaster to begin
  }
  
	public MasterWrapper(String ip, int port) {
    updateMaster(ip,port);		
	}
  
	public void updateMaster(String ip, int port) {
    try {
      if (master != null) master.close();
      master = new Socket(ip,port);
      ostream = new DataOutputStream(master.getOutputStream());
      istream = new BufferedReader(
                        new InputStreamReader(master.getInputStream()));
    } catch (IOException ioe) {
      System.err.println("Problem closing oldMaster");
    }
	}
  
  public void sendMessage(String message) {
    try {
      ostream.writeBytes(message + "\n");
    }
    catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }
  
  public String readMessage() {
    try {
      return istream.readLine();
    }
    catch (IOException ioe) {
      ioe.printStackTrace();
      return null;
    }
  }
  
}
