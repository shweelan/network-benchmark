package nbm.message;

import java.io.*;
import java.util.*;


public class Message {
  private static final String DELIMETER = "-_-";
  private static final char CHAR = 'S';
  private long ts;
  private String id;
  private String body;
  private String inRespTo = null;

  public Message(long ts, String id, String body, String inRespTo) {
    this.ts = ts;
    this.id = id;
    this.body = body;
    if (inRespTo != null) {
      this.inRespTo = inRespTo;
    }
  }

  public Message(long ts, String id, String body) {
    this(ts, id, body, null);
  }

  public Message(long ts, String id, int bodySize, String inRespTo) {
    this(ts, id, getChunk(bodySize, CHAR), inRespTo);
  }

  public Message(long ts, String id, int chunkSize) {
    this(ts, id, chunkSize, null);
  }

  public Message(String msg) {
    this.fromString(msg);
  }

  public void fromString(String str) {
    String[] result = str.split(DELIMETER);
    this.ts = Integer.parseInt(result[0]);
    this.id = result[1];
    this.body = result[2];
    if (result.length > 3) {
      this.inRespTo = result[3];
    }
  }

  public String toString() {
    int size = (inRespTo != null) ? 4 : 3;
    String[] list = new String[size];
    list[0] = String.valueOf(this.ts);
    list[1] = this.id;
    list[2] = this.body;
    if (inRespTo != null) {
      list[3] = this.inRespTo;
    }
    String str = String.join(DELIMETER, list);
    return str;
  }

  public static String getChunk(int chunkSize, char c) {
    char[] data = new char[chunkSize];
    Arrays.fill(data, c);
    return new String(data);
  }
}
