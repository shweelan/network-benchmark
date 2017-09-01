import java.net.*;
import java.io.*;


class ServerWorker implements Runnable {
  private Socket socket;
  private String id;

  public ServerWorker(Socket socket) {
    this.socket = socket;
    this.id = this.socket.getInetAddress() + ":" + String.valueOf(this.socket.getPort());
    System.out.println("Connection `" + this.id + "` connected!");
  }

  public void run() {
    try {
      BufferedReader inputStream = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
      String inputLine;
      PrintStream outputStream = new PrintStream(this.socket.getOutputStream());
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
      this.socket.close();
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
      new Thread(new ServerWorker(socket)).start();
    }
  }
}
