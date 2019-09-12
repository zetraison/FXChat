package client.io;

import java.io.*;

public class HistoryReader<T> {
    private String filePath = "C:\\Users\\MiNotebook\\IdeaProjects\\FXChat\\src\\client\\resources\\history.txt";

    public T read() throws IOException, ClassNotFoundException {
        ObjectInputStream ois = null;;
        T result = null;
        try {
            FileInputStream fin = new FileInputStream(this.filePath);;
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

    private void createFile() throws IOException {
        BufferedWriter output = null;
        try {
            File file = new File(this.filePath);
            output = new BufferedWriter(new FileWriter(file));
        } finally {
            System.out.println("History file is created");
            if (output != null) {
                output.close();
            }
        }
    }
}
