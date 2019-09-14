package client.io;

import client.ConfigLoader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class HistoryWriter<T> {

    String filePath = ConfigLoader.load().getProperty("history.filepath");

    public void write(T obj) throws IOException {
        FileOutputStream fout = new FileOutputStream(filePath);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(obj);
        oos.close();
    }
}
