import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    public Button send;
    public ListView<String> listView;
    public TextField text;
    private List<File> clientFileList;
    public static Socket socket;
    private DataInputStream is;
    private DataOutputStream os;
    private String clientPath = "./client/src/main/resources/";

    public void sendCommand(ActionEvent actionEvent) {
        // получаем текст из поля ввода
        String request = text.getText().trim();
        String command = "./download";
        String fileName = request.substring(command.length()).trim();
        //System.out.println("Command: " + command + "; file: " + fileName);
        // отсылаем запрос на сервер
        try {
            os.writeUTF(command);
            os.writeUTF(fileName);
            // получаем ответ сервера, если ОК, то забираем файл
            String response = is.readUTF();
            if (response.equals("./sending")) {
                // получаем размер файла
                long fileSize = is.readLong();
                byte[] buffer = new byte[1024];
                long readingCycles = (fileSize / buffer.length > 0 ? fileSize / buffer.length : 1);
                // создаем файл на диске
                File file = new File(clientPath, fileName);
                try (FileOutputStream fos = new FileOutputStream(file)){
                    for(long i = 0; i < readingCycles; i++) {
                        int bytesRead = is.read(buffer);
                        fos.write(buffer, 0, bytesRead);
                    }
                } catch (FileNotFoundException e) {
                    System.out.println("Cannot create file to write");
                }
                os.writeUTF("OK");
                clientFileList.add(file);
                listView.getItems().add(file.getName() + " : " + file.length());
            } else if (response.startsWith("./error!")) {
                System.out.println(response.substring(9));
            } else {
                System.out.println("Something went completely wrong");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //System.out.println("SEND!");
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TODO: 7/21/2020 init connect to server
        try{
            socket = new Socket("localhost", 8189);
            is = new DataInputStream(socket.getInputStream());
            os = new DataOutputStream(socket.getOutputStream());
            Thread.sleep(1000);
            clientFileList = new ArrayList<>();
            File dir = new File(clientPath);
            if (!dir.exists()) {
                throw new RuntimeException("directory resource not exists on client");
            }
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                clientFileList.add(file);
                listView.getItems().add(file.getName() + " : " + file.length());
            }
            listView.setOnMouseClicked(a -> {
                if (a.getClickCount() == 2) {
                    String fileName = listView.getSelectionModel().getSelectedItem();
                    File currentFile = findFileByName(fileName);
                    if (currentFile != null) {
                        try {
                            os.writeUTF("./upload");
                            os.writeUTF(fileName);
                            os.writeLong(currentFile.length());
                            FileInputStream fis = new FileInputStream(currentFile);
                            byte [] buffer = new byte[1024];
                            while (fis.available() > 0) {
                                int bytesRead = fis.read(buffer);
                                os.write(buffer, 0, bytesRead);
                            }
                            os.flush();
                            String response = is.readUTF();
                            System.out.println(response);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File findFileByName(String fileName) {
        for (File file : clientFileList) {
            if (file.getName().equals(fileName)){
                return file;
            }
        }
        return null;
    }
}
