package server;

import core.models.Event;
import core.enums.EventType;
import server.services.AuthService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {
    private Vector<Client> clients;

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
                new Client(this, socket);
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

    public Vector<Client> getClients() {
        return clients;
    }

    public void subscribe(Client client) {
        clients.add(client);
        System.out.println("Client authorized " + client.getUsername());
        broadcastClientListEvent();
        if (client.isAdmin()) {
            broadcastBlockedClientListEvent();
        }
        if (client.isBlocked()) {
            broadcastClientBlockedEvent(client.getUsername());
        }
    }

    public void unsubscribe(Client client) {
        clients.remove(client);
        System.out.println("Client unauthorized " + client.getUsername());
        broadcastClientListEvent();
    }

    public boolean isUsernameBusy(String nickname) {
        for (Client client: clients) {
            if (client.getUsername().equalsIgnoreCase(nickname)) {
                return true;
            }
        }
        return false;
    }

    public void broadcastEvent(Client fromUser, Event event) {
        for (Client client : clients) {
            if (!client.checkUserInBlackList(fromUser.getUsername())) {
                client.sendEvent(event);
            }
        }
    }

    public void privateEvent(String toUser, Event event) {
        for (Client client : clients) {
            if (client.getUsername().equals(toUser)) {
                client.sendEvent(event);
            }
        }
    }

    public void blockClient(String user) {
        AuthService.blockUser(user);
        broadcastClientBlockedEvent(user);
    }

    public void broadcastClientListEvent() {
        List<String> clientList = new ArrayList<>();
        for (Client client: clients) {
            clientList.add(client.getUsername());
        }
        for (Client client: clients) {
            Event event = new Event(client.getUsername(), EventType.CLIENT_LIST, clientList);
            client.sendEvent(event);
        }
    }

    public void broadcastBlockedClientListEvent() {
        List<String> blockedUsers = AuthService.getBlockedUsers();
        blockedUsers.add(0, "Blocked users: ");
        for (Client client: clients) {
            if (client.isAdmin()) {
                client.sendEvent(new Event(client.getUsername(), EventType.MESSAGE, blockedUsers));
            }
        }
    }

    public void broadcastClientBlockedEvent(String toUser) {
        for (Client client: clients) {
            if (client.getUsername().equals(toUser)) {
                client.sendEvent(new Event(client.getUsername(), EventType.BLOCK_USER, Collections.singletonList(toUser)));
            }
        }
    }
}
