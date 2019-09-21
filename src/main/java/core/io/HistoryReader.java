package core.io;

import core.config.ConfigLoader;
import org.apache.log4j.Logger;

import java.io.*;

public class HistoryReader<T> {
    private static final Logger LOGGER = Logger.getLogger(HistoryReader.class);
    private static final String path = ConfigLoader.load().getProperty("history.path");

    /**
     * Read text file and deserialize it
     * @return deserialize object
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public T read() throws IOException, ClassNotFoundException {
        ObjectInputStream ois = null;
        T result = null;
        try {
            FileInputStream fin = new FileInputStream(path);
            ois = new ObjectInputStream(fin);
            result = (T) ois.readObject();
        } catch (EOFException e) {
            LOGGER.error("History file is empty.");
        } catch (FileNotFoundException e) {
            LOGGER.error("History file is not exist.");
            this.createFile();
        } finally {
            if (ois != null) {
                ois.close();
            }
        }
        return result;
    }

    /**
     * Create text file if not exist
     *
     * @throws IOException
     */
    private void createFile() throws IOException {
        BufferedWriter output = null;
        try {
            File file = new File(path);
            output = new BufferedWriter(new FileWriter(file));
        } finally {
            LOGGER.info("History file is created.");
            if (output != null) {
                output.close();
            }
        }
    }
}
