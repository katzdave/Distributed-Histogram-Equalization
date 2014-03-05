package masterprotocol;

public class ConsumerInfo implements Comparable<ConsumerInfo> {
  public Integer id;
  public Double load;
  
  ConsumerInfo(Integer id, double load) {
    this.id = id;
    this.load = load;
  }

  @Override
  public int compareTo(ConsumerInfo o) {
    return this.load.compareTo(o.load);
  }
  
}
