import java.io.*;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class ConnectionHandler implements Runnable {

    private DataInputStream is;
    private DataOutputStream os;

    public ConnectionHandler(Socket socket) throws IOException, InterruptedException {
        System.out.println("Connection accepted");
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        Thread.sleep(2000);
    }



    @Override
    public void run() {
        byte [] buffer = new byte[1024];
        while (true) {
            try {
                String command = is.readUTF();
                if (command.equals("./upload")) {
                    String fileName = is.readUTF();
                    System.out.println("fileName: " + fileName);
                    long fileLength = is.readLong();
                    System.out.println("fileLength: " + fileLength);
                    File file = new File(Server.serverPath + "/" + fileName);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    try(FileOutputStream fos = new FileOutputStream(file)) {
                        for (long i = 0; i < (fileLength / 1024 == 0 ? 1 : fileLength / 1024); i++) {
                            int bytesRead = is.read(buffer);
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                    os.writeUTF("OK");
                } else if (command.equals("./download")) { // скачивание файла с сервера
                    String fileName = is.readUTF();
                    File requested = new File(Server.serverPath + "/" + fileName);
                    // если файл существует, открываем для чтения
                    if (requested.exists()) {
                        // сообщаем клиенту, что файл будет отослан и его размер
                        os.writeUTF("./sending");
                        os.writeLong(requested.length());
                        try (FileInputStream fis = new FileInputStream(requested)) {
                            // пока в файле есть содержимое читаем и отсылаем клиенту
                            while(fis.available() > 0) {
                                int bytesRead = fis.read(buffer);
                                os.write(buffer, 0, bytesRead);
                            }
                            // очищаем буфер
                            os.flush();
                            // получаем от клиента ответ, что все прошло успешно и печатема его в консоль
                            String response = is.readUTF();
                            System.out.println(response);
                        }
                    } else {
                        // если файла нет, отсылаемклиенту ответ, что все плохо
                        os.writeUTF("./error! Requested file does not exist!");
                    }
                } else { // если получена непредусмотренная команда, сообщаем об этом клиенту
                    os.writeUTF("./error! Command is wrong!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
