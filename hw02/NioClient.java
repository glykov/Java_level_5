import java.io.*;
import java.net.*;

public class NioClient {
    public static void main(String[] args) throws Exception {
        Socket sock = new Socket("localhost", 8189);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String message = null;

        while ((message = br.readLine()) != null) {
            if (message.toLowerCase().equals("bye")) {
                break;
            }
            sock.getOutputStream().write(message.getBytes());
        }

        sock.close();
    }
}