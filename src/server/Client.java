package server;

import server.services.CensorService;
import core.models.Event;
import core.enums.EventType;
import server.services.AuthService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class Client {
    private final static String ERROR_INCORRECT_USERNAME_LOGIN_PASSWORD = "Incorrect username/logo/pass!";
    private final static String ERROR_INCORRECT_LOGIN_PASSWORD = "Incorrect logo/pass!";
    private final static String ERROR_USERNAME_ALREADY_USED = "Username already used!";

    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private CensorService censorService;
    private ArrayList<String> blackList;
    private String username;
    private Boolean admin;
    private Boolean blocked;

    public Client(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.censorService = new CensorService();
            this.blackList = new ArrayList<>();
            this.admin = false;
            this.blocked = false;

            new Thread(this::eventLoop).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return username;
    }

    public Boolean isAdmin() {
        return admin;
    }

    public Boolean isBlocked() {
        return blocked;
    }

    public boolean checkUserInBlackList(String username) {
        return this.blackList.contains(username);
    }

    public void sendEvent(Event event) {
        try {
            out.writeUTF(event.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void eventLoop() {
        try {
            while (true) {
                String data = in.readUTF();
                Event event = new Event(data);
                System.out.println("[EVENT]: " + event.toString());

                switch (event.getType()) {
                    case AUTH: {
                        handleAuth(event);
                        continue;
                    }
                    case REGISTER: {
                        handleRegister(event);
                        continue;
                    }
                    case HELP: {
                        sendEvent(new Event(username, EventType.HELP, null));
                        continue;
                    }
                    case PRIVATE_MESSAGE: {
                        String toUser = event.getArgs().get(0);
                        server.privateEvent(toUser, event);
                        continue;
                    }
                    case MESSAGE: {
                        Boolean isDangerUser = censorService.checkMessage(event.getAuthor(), event.getArgs());
                        if (isDangerUser) {
                            server.blockClient(event.getAuthor());
                        }
                    }
                    case STICKER:
                    case IMAGE: {
                        server.broadcastEvent(this, event);
                        continue;
                    }
                    case BLACKLIST: {
                        handleAddToBlackList(event);
                        continue;
                    }
                    case WHITELIST: {
                        handleRemoveFromBlackList(event);
                        continue;
                    }
                    case CHANGE_LOGIN: {
                        handleChangeLogin(event);
                        continue;
                    }
                    case END: {
                        sendEvent(new Event(this.getUsername(), EventType.SERVER_CLOSED, null));
                        break;
                    }
                }
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            server.unsubscribe(Client.this);
        }
    }

    private void handleAuth(Event event) {
        String username = null;
        if (event.getArgs().size() == 2 && event.getArgs().get(0) != null && event.getArgs().get(1) != null) {
            String login = event.getArgs().get(0);
            String password = event.getArgs().get(1);
            username = AuthService.getUsername(login, password);
        }
        if (username == null) {
            sendEvent(new Event(username, EventType.ERROR, Collections.singletonList(ERROR_INCORRECT_LOGIN_PASSWORD)));
            return;
        }
        if (server.isUsernameBusy(username)) {
            sendEvent(new Event(username, EventType.ERROR, Collections.singletonList(ERROR_USERNAME_ALREADY_USED)));
            return;
        }
        this.username = username;
        this.admin = AuthService.isAdmin(username);
        this.blocked = AuthService.isBlocked(username);
        this.blackList = AuthService.getUserBlacklist(username);
        sendEvent(new Event(username, EventType.AUTH_OK, Collections.singletonList(username)));
        server.subscribe(Client.this);
    }

    private void handleRegister(Event event) {
        if (event.getArgs().size() == 3 &&
                event.getArgs().get(0) != null &&
                event.getArgs().get(1) != null &&
                event.getArgs().get(2) != null
        ) {
            String username = event.getArgs().get(0);
            String login = event.getArgs().get(1);
            String passwordHash = AuthService.MD5(event.getArgs().get(2));

            AuthService.addUser(username, login, passwordHash);
        } else {
            sendEvent(new Event(
                    username,
                    EventType.ERROR,
                    Collections.singletonList(ERROR_INCORRECT_USERNAME_LOGIN_PASSWORD)));
        }
    }

    private void handleAddToBlackList(Event event) {
        String username = event.getArgs().get(0);
        AuthService.addToBlacklist(this.getUsername(), username);
        this.blackList = AuthService.getUserBlacklist(username);
        sendEvent(new Event(
                this.getUsername(),
                EventType.MESSAGE,
                Arrays.asList(this.username, "Add user " + username + " to blacklist"))
        );
    }

    private void handleRemoveFromBlackList(Event event) {
        String username = event.getArgs().get(0);
        AuthService.removeFromBlacklist(this.getUsername(), username);
        this.blackList = AuthService.getUserBlacklist(this.username);
        sendEvent(new Event(
                this.getUsername(),
                EventType.MESSAGE,
                Arrays.asList(this.username, "Remove user " + username + " from blacklist"))
        );
    }

    private void handleChangeLogin(Event event) {
        String login = event.getArgs().get(0);
        AuthService.changeLogin(this.getUsername(), login);
        sendEvent(new Event(
                this.getUsername(),
                EventType.MESSAGE,
                Collections.singletonList("User " + this.getUsername() + " updated login to " + login))
        );
    }
}
