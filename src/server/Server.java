package server;

import client.models.Event;
import client.models.EventType;
import server.services.AuthService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

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
                if (socket != null) {
                    socket.close();
                }
                if (server != null) {
                    server.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            AuthService.disconnect();
        }
    }

    public void broadcastEvent(ClientHandler from, Event event) {
        for (ClientHandler client : clients) {
            if (!client.checkBlackList(from.getNick())) {
                client.sendEvent(event);
            }
        }
    }

    public void privateEvent(String nickname, Event event) {
        for (ClientHandler client : clients) {
            if (client.getNick().equals(nickname)) {
                client.sendEvent(event);
            }
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        System.out.println("Client authorized " + clientHandler.getNick());
        clients.add(clientHandler);
        broadcastClientList();
        if (AuthService.getIsAdmin(clientHandler.getNick())) {
            broadcastForbiddenList();
        }
        if (AuthService.getIsBlocked(clientHandler.getNick())) {
            broadcastUserBlocked(clientHandler.getNick());
        }
    }

    public void blockUser(String user) {
        AuthService.blockUser(user);
        broadcastUserBlocked(user);
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientList();
    }

    public Vector<ClientHandler> getClients() {
        return clients;
    }

    public boolean isNickBusy(String nickname) {
        for (ClientHandler client: clients) {
            if (client.getNick().equalsIgnoreCase(nickname)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastClientList() {
        List<String> clientList = new ArrayList<>();
        for (ClientHandler client: clients) {
            clientList.add(client.getNick());
        }
        for (ClientHandler client: clients) {
            Event event = new Event(client.getNick(), EventType.CLIENT_LIST, clientList);
            client.sendEvent(event);
        }
    }

    public void broadcastForbiddenList() {
        List<String> blockedUsers = AuthService.getBlockedUsers();
        blockedUsers.add(0, "Blocked users: ");
        for (ClientHandler client: clients) {
            if (client.isAdmin()) {
                client.sendEvent(new Event(client.getNick(), EventType.MESSAGE, blockedUsers));
            }
        }
    }

    public void broadcastUserBlocked(String user) {
        for (ClientHandler client: clients) {
            if (client.getNick().equals(user)) {
                client.sendEvent(new Event(client.getNick(), EventType.USER_BLOCKED, Collections.singletonList(user)));
            }
        }
    }
}
