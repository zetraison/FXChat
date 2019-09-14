package core.io;

import core.config.ConfigLoader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class HistoryWriter<T> {

    String path = ConfigLoader.load().getProperty("history.path");

    /**
     * Serialize object to text file
     * @param obj   serializable object
     * @throws IOException
     */
    public void write(T obj) throws IOException {
        FileOutputStream fout = new FileOutputStream(path);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(obj);
        oos.close();
    }
}
