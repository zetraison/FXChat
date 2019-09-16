package server.services;

public class PrintService {
    private final Object mon = new Object();
    private volatile char currentLetter = 'A';

    public static void main(String[] args) {
        PrintService printer = new PrintService();
        Thread t1 = new Thread(() -> printer.print('A', 'B'), "Thread-A");
        Thread t2 = new Thread(() -> printer.print('B', 'C'), "Thread-B");
        Thread t3 = new Thread(() -> printer.print('C', 'A'), "Thread-C");
        t1.start();
        t2.start();
        t3.start();
    }

    public void print(char current, char next) {
        synchronized (mon) {
            try {
                for (int i = 0; i < 5; i++) {
                    while (currentLetter != current) {
                        mon.wait();
                    }
                    System.out.print(current);
                    currentLetter = next;
                    mon.notifyAll();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

