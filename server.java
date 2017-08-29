import java.net.*;
import java.io.*;


class Worker implements Runnable {
  private Socket client;
  private String id;

  public Worker(Socket client) {
    this.client = client;
    this.id = client.getInetAddress() + ":" + String.valueOf(client.getPort());
    System.out.println("Connection `" + this.id + "` connected!");
  }

  public void run() {
    try {
      BufferedReader inputStream = new BufferedReader(new InputStreamReader(client.getInputStream()));
      String inputLine;
      PrintStream outputStream = new PrintStream(client.getOutputStream());
      while ((inputLine = inputStream.readLine()) != null) {
        System.out.println("Message from `" + this.id + "` : " + inputLine);
        System.out.println("Message to `" + this.id + "` : " + inputLine);
        outputStream.println(inputLine);
        outputStream.flush();
        if (inputLine.equals("bye")) {
          break;
        }
      }
      inputStream.close();
      outputStream.close();
      client.close();
      System.out.println("Connection `" + this.id + "` disconnected!");
    } catch (IOException e) {
      System.out.println(e);
    }
  }
}

class Server {
  public static void main(String args[]) throws Exception {
    ServerSocket server = new ServerSocket(2912);
    System.out.println("Server listening on port " + server.getLocalPort());
    while (true) {
      Socket socket = server.accept();
      new Thread(new Worker(socket)).start();
    }
  }
}
