package imageconsumer;

import java.net.Socket;
import java.io.IOException;
public class MasterWrapper {
	Socket master;
	public MasterWrapper(String ip, int port) {
    try {
      this.master = new Socket(ip,port);
    } catch (IOException ioe) {
      System.err.println("Problem opening master");
    }
		
	}
  
	public void updateMaster(String ip, int port) {
    try {
      master.close();
      master = new Socket(ip,port);
    } catch (IOException ioe) {
      System.err.println("Problem closing oldMaster");
    }
		
	}
}
