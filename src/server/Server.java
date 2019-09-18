package server;

import core.config.ConfigLoader;
import core.models.Event;
import core.enums.EventType;
import server.services.AuthService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private Vector<Client> clients;

    public Server(int port) {
        ServerSocket server = null;
        Socket socket = null;
        ExecutorService executor = null;

        AuthService.connect();
        clients = new Vector<>();

        int threadPoolCount = Integer.parseInt(ConfigLoader.load().getProperty("server.thread-pool-count"));

        try {
            server = new ServerSocket(port);
            System.out.println("Start server on port " + port);
            executor = Executors.newFixedThreadPool(threadPoolCount);

            while (true) {
                socket = server.accept();
                Client client = new Client(this, socket);
                executor.execute(client);
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
                if (executor != null) {
                    executor.shutdown();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            AuthService.disconnect();
        }
    }

    public synchronized void subscribe(Client client) {
        clients.add(client);
        System.out.println("Client authorized: " + client.getUsername());
        broadcastClientListEvent();
        if (client.isAdmin()) {
            broadcastBlockedClientListEvent();
        }
        if (client.isBlocked()) {
            broadcastClientBlockedEvent(client.getUsername());
        }
    }

    public synchronized void unsubscribe(Client client) {
        clients.remove(client);
        System.out.println("Client unauthorized " + client.getUsername());
        broadcastClientListEvent();
    }

    public synchronized boolean isUsernameBusy(String nickname) {
        for (Client client: clients) {
            if (client.getUsername().equalsIgnoreCase(nickname)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void broadcastEvent(Client fromUser, Event event) {
        for (Client client : clients) {
            if (!client.checkUserInBlackList(fromUser.getUsername())) {
                client.sendEvent(event);
            }
        }
    }

    public synchronized void privateEvent(String toUser, Event event) {
        for (Client client : clients) {
            if (client.getUsername().equals(toUser)) {
                client.sendEvent(event);
            }
        }
    }

    public synchronized void blockClient(String user) {
        AuthService.blockUser(user);
        broadcastClientBlockedEvent(user);
    }

    public synchronized void broadcastClientListEvent() {
        List<String> clientList = new ArrayList<>();
        for (Client client: clients) {
            clientList.add(client.getUsername());
        }
        for (Client client: clients) {
            Event event = new Event(client.getUsername(), EventType.CLIENT_LIST, clientList);
            client.sendEvent(event);
        }
    }

    public synchronized void broadcastBlockedClientListEvent() {
        List<String> blockedUsers = AuthService.getBlockedUsers();
        blockedUsers.add(0, "Blocked users: ");
        for (Client client: clients) {
            if (client.isAdmin()) {
                client.sendEvent(new Event(client.getUsername(), EventType.MESSAGE, blockedUsers));
            }
        }
    }

    public synchronized void broadcastClientBlockedEvent(String toUser) {
        for (Client client: clients) {
            if (client.getUsername().equals(toUser)) {
                client.sendEvent(new Event(client.getUsername(), EventType.BLOCK_USER, Collections.singletonList(toUser)));
            }
        }
    }
}
