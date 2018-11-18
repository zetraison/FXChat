package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Vector;

public class Server {

    private Vector<ClientHandler> clients;

    public Server(int port) {
        AuthService.connect();
        clients = new Vector<>();
        ServerSocket server = null;
        Socket socket = null;

        try {
            server = new ServerSocket(port);
            System.out.println("Start server on port " + port);

            while (true) {
                socket = server.accept();
                new ClientHandler(this, socket);
                System.out.println("Client connected");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            AuthService.disconnect();
        }
    }

    public void broadcastMsg(String msg) {
        for (ClientHandler o : clients) {
            o.sendMsg(msg);
        }
    }

    public void sendMsgToClient(String nickname, String msg) {
        for (ClientHandler o : clients) {
            if (o.getNick().equals(nickname)) {
                o.sendMsg(msg);
            }
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        System.out.println("Client authorized " + clientHandler.getNick());
        clients.add(clientHandler);
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    public Vector<ClientHandler> getClients() {
        return clients;
    }
}
