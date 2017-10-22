package nbm.common;

import java.io.*;
import java.util.*;


public class Message {
  private static final String DELIMETER = "-_-";
  private static final char CHAR = 'S';
  private long ts;
  private String id;
  private String body;
  private long originalTs = 0; // read only, not serializable

  public Message(String id, String body) {
    this.ts = System.currentTimeMillis();
    this.id = id;
    this.body = body;
  }

  public Message(String id, int bodySize) {
    this(id, getChunk(bodySize, CHAR));
  }

  public Message(String msg) { // parse from string
    String[] result = msg.split(DELIMETER);
    this.ts = System.currentTimeMillis();
    this.originalTs = Long.parseLong(result[0]);
    this.id = result[1];
    this.body = result[2];
  }

  public long getTs() {
    return this.ts;
  }

  public long getOriginalTs() {
    return this.originalTs;
  }

  public String getId() {
    return this.id;
  }

  public void getId(String id) {
    this.id = id;
  }

  public String getBody() {
    return this.body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String toString() {
    String[] list = new String[3];
    list[0] = String.valueOf(this.ts);
    list[1] = this.id;
    list[2] = this.body;
    String str = String.join(DELIMETER, list);
    return str;
  }

  public static String getChunk(int chunkSize, char c) {
    char[] data = new char[chunkSize];
    Arrays.fill(data, c);
    return new String(data);
  }
}
