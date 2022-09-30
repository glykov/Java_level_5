import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class NioServer {
    private ServerSocketChannel server = ServerSocketChannel.open();
    private Selector selector = Selector.open();
    private SelectionKey selection;
    private Map<Channel, String> dataMap = new HashMap<>();

    private NioServer() throws IOException {
        System.out.println("Starting server");
        server.configureBlocking(false);
        server.socket().bind(new InetSocketAddress(8189));
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    private void start() throws IOException {
        System.out.println("Server started");
        while (true) {
            selector.select();
            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
            while (keyIter.hasNext()) {
                selection = keyIter.next();
                keyIter.remove();
                if (!selection.isValid()) {
                    invalid();
                } else if (selection.isAcceptable()) {
                    createClient();
                } else if (selection.isReadable()) {
                    read();
                } else if (selection.isWritable()) {
                    write();
                }
            }
        }
    }

    private void invalid() throws IOException {
        System.out.println("Invalid selection");
        selection.channel().close();
        selection.cancel();
    }

    private void createClient() throws IOException {
        SocketChannel client = server.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
    }

    private void read() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        SocketChannel client = (SocketChannel) selection.channel();
        int readed = client.read(buffer);
        StringBuilder sb = new StringBuilder();
        if (readed == -1) {
            System.out.println("Connection closed by client");
            client.close();
            selection.cancel();
            return;
        } else {
            buffer.flip();
            sb.append(new String(buffer.array(), "UTF-8"));
            buffer.clear();
        }
        readed = client.read(buffer);
        while (readed != -1 && readed != 0) {
            buffer.flip();
            sb.append(new String(buffer.array(), "UTF-8"));
            buffer.clear();
            readed = client.read(buffer);
        }
        System.out.println("Incoming:");
        System.out.println(sb.toString());
        System.out.println();
        dataMap.put(client, sb.toString());
        selection.interestOps(SelectionKey.OP_WRITE);
    }

    private void write() throws IOException {
        SocketChannel client = (SocketChannel) selection.channel();
        String data = dataMap.get(client);
        System.out.println("Outcomming:");
        System.out.println(data);
        System.out.println("");
        client.write(ByteBuffer.wrap(data.getBytes()));
        selection.interestOps(SelectionKey.OP_READ);
    }

    private static void logln(String s) {
        System.out.println(s);
    }

    private static void log(String s) {
        System.out.print(s);
    }

    public static void main(String[] args) throws IOException {
        new NioServer().start();
    }
}