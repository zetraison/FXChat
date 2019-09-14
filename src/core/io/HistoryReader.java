package core.io;

import core.config.ConfigLoader;

import java.io.*;

public class HistoryReader<T> {
    private String path = ConfigLoader.load().getProperty("history.path");

    /**
     * Read text file and deserialize it
     * @return deserialize object
     *tewt
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public T read() throws IOException, ClassNotFoundException {
        ObjectInputStream ois = null;;
        T result = null;
        try {
            FileInputStream fin = new FileInputStream(path);
            ois = new ObjectInputStream(fin);;
            result = (T) ois.readObject();
        } catch (EOFException e) {
            System.out.println("History file is empty");
        } catch (FileNotFoundException e) {
            System.out.println("History file is not exist");
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
            System.out.println("History file is created");
            if (output != null) {
                output.close();
            }
        }
    }
}
