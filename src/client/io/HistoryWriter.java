package client.io;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class HistoryWriter<T> {
    private String filePath = "C:\\Users\\MiNotebook\\IdeaProjects\\FXChat\\src\\client\\resources\\history.txt";

    public void write(T obj) throws IOException {
        FileOutputStream fout = new FileOutputStream(this.filePath);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(obj);
        oos.close();
    }
}
