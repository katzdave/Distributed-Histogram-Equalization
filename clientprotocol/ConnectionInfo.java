/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package clientprotocol;

/**
 *
 * @author David
 */
public class ConnectionInfo {
  public String HostName;
  public int Port;
  
  public ConnectionInfo(String hostname, int port){
    HostName = hostname;
    Port = port;
  }
}
