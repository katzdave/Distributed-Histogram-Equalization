package masterserver;

public class Message {
  public int connectedID;
  public String message;
  public Message(int connectedID, String message) {
    this.connectedID = connectedID;
    this.message = message;
  }
}
