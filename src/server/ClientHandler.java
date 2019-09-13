package server;

import client.models.Event;
import client.models.EventType;
import server.services.AuthService;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class ClientHandler {

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Server server;
    private String nick;
    private ArrayList<String> blackList;

    private final static String ERROR_INCORRECT_USERNAME_LOGIN_PASSWORD = "Incorrect username/logo/pass!";
    private final static String ERROR_INCORRECT_LOGIN_PASSWORD = "Incorrect logo/pass!";
    private final static String ERROR_ACCOUNT_ALREADY_USED = "Account already used!";

    public ClientHandler(Server server, Socket socket) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.blackList = new ArrayList<>();

            new Thread(this::eventLoop).start();

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
                        String nick = null;
                        if (event.getArgs().size() == 2 && event.getArgs().get(0) != null && event.getArgs().get(1) != null) {
                            String login = event.getArgs().get(0);
                            String password = event.getArgs().get(1);
                            nick = AuthService.getNickname(login, password);
                        }
                        if (nick == null) {
                            sendEvent(new Event(
                                    nick,
                                    EventType.ERROR,
                                    Collections.singletonList(ERROR_INCORRECT_LOGIN_PASSWORD)));
                            continue;
                        }
                        if (server.isNickBusy(nick)) {
                            sendEvent(new Event(
                                    nick,
                                    EventType.ERROR,
                                    Collections.singletonList(ERROR_ACCOUNT_ALREADY_USED)));
                            continue;
                        }
                        this.nick = nick;
                        this.blackList = AuthService.getUserBlacklist(nick);
                        sendEvent(new Event(nick, EventType.AUTH_OK, Collections.singletonList(nick)));
                        server.subscribe(ClientHandler.this);
                        continue;
                    }
                    case REGISTER: {
                        if (
                                event.getArgs().size() == 3 &&
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
                                    nick,
                                    EventType.ERROR,
                                    Collections.singletonList(ERROR_INCORRECT_USERNAME_LOGIN_PASSWORD)));
                        }
                        continue;
                    }
                    case HELP: {
                        sendEvent(new Event(nick, EventType.HELP, null));
                        continue;
                    }
                    case PRIVATE_MESSAGE: {
                        String toUser = event.getArgs().get(0);
                        server.privateEvent(toUser, event);
                        continue;
                    }
                    case MESSAGE:
                    case STICKER:
                    case IMAGE: {
                        server.broadcastEvent(this, event);
                        continue;
                    }
                    case BLACKLIST: {
                        String user = event.getArgs().get(0);
                        boolean result = AuthService.addToBlacklist(this.getNick(), user);
                        this.blackList = AuthService.getUserBlacklist(nick);
                        sendEvent(new Event(
                                this.getNick(),
                                EventType.MESSAGE,
                                result
                                        ? Arrays.asList(this.nick, "Add user " + user + " to blacklist")
                                        : Arrays.asList(this.nick, "Error on adding user " +  user + " to blacklist")
                                )
                        );
                        continue;
                    }
                    case WHITELIST: {
                        String user = event.getArgs().get(0);
                        boolean result = AuthService.removeFromBlacklist(this.getNick(),  user);
                        this.blackList = AuthService.getUserBlacklist(nick);
                        sendEvent(new Event(
                                this.getNick(),
                                EventType.MESSAGE,
                                result
                                        ? Arrays.asList(this.nick, "Remove user " +  user + " from blacklist")
                                        : Arrays.asList(this.nick, "Error on removing user " +  user + " to blacklist")
                                )
                        );
                        continue;
                    }
                    case CHANGE_LOGIN:
                        String login = event.getArgs().get(0);
                        boolean result = AuthService.changeLogin(this.getNick(), login);
                        sendEvent(new Event(
                                this.getNick(),
                                EventType.MESSAGE,
                                result
                                        ? Collections.singletonList("User " + this.getNick() + " updated login to " + login)
                                        : Collections.singletonList("Error updating login to " + login + " by user" + this.getNick())
                                )
                        );
                        continue;
                    case END: {
                        sendEvent(new Event(this.getNick(), EventType.SERVER_CLOSED, null));
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
            server.unsubscribe(ClientHandler.this);
        }
    }

    public void sendEvent(Event event) {
        try {
            out.writeUTF(event.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }

    public boolean checkBlackList(String nick) {
        return this.blackList.contains(nick);
    }
}
