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
                            nick = AuthService.getNickname(event.getArgs().get(0), event.getArgs().get(1));
                        }
                        if (nick == null) {
                            Event errorEvent = new Event(nick, EventType.ERROR, Collections.singletonList("Incorrect logo/pass!"));
                            sendEvent(errorEvent);
                            continue;
                        }
                        if (server.isNickBusy(nick)) {
                            Event errorEvent = new Event(nick, EventType.ERROR, Collections.singletonList("Account already used!"));
                            sendEvent(errorEvent);
                            continue;
                        }
                        this.nick = nick;
                        this.blackList = AuthService.getUserBlacklist(nick);
                        Event authOkEvent = new Event(nick, EventType.AUTH_OK, Collections.singletonList(nick));
                        sendEvent(authOkEvent);
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
                            Event errorEvent = new Event(nick, EventType.ERROR, Collections.singletonList("Incorrect username/logo/pass!"));
                            sendEvent(errorEvent);
                        }
                        continue;
                    }
                    case HELP: {
                        Event errorEvent = new Event(nick, EventType.HELP, null);
                        sendEvent(errorEvent);
                        continue;
                    }
                    case PRIVATE_MESSAGE: {
                        server.privateEvent(event.getArgs().get(0), event);
                        continue;
                    }
                    case MESSAGE:
                    case STICKER:
                    case IMAGE: {
                        server.broadcastEvent(this, event);
                        continue;
                    }
                    case BLACKLIST: {
                        boolean result = AuthService.addToBlacklist(this.getNick(), event.getArgs().get(0));
                        this.blackList = AuthService.getUserBlacklist(nick);
                        if (result) {
                            Event event1 = new Event(this.getNick(), EventType.MESSAGE, Arrays.asList(this.nick, "Add user " + event.getArgs().get(0) + " to blacklist"));
                            sendEvent(event1);
                        } else {
                            Event event1 = new Event(this.getNick(), EventType.MESSAGE,Arrays.asList(this.nick, "Error on adding user " +  event.getArgs().get(0) + " to blacklist"));
                            sendEvent(event1);
                        }
                        continue;
                    }
                    case WHITELIST: {
                        boolean result = AuthService.removeFromBlacklist(this.getNick(),  event.getArgs().get(0));
                        this.blackList = AuthService.getUserBlacklist(nick);
                        if (result) {
                            Event event1 = new Event(this.getNick(), EventType.MESSAGE, Arrays.asList(this.nick, "Remove user " +  event.getArgs().get(0) + " from blacklist"));
                            sendEvent(event1);
                        } else {
                            Event event1 = new Event(this.getNick(), EventType.MESSAGE, Arrays.asList(this.nick, "Error on removing user " +  event.getArgs().get(0) + " to blacklist"));
                            sendEvent(event1);
                        }
                        continue;
                    }
                    case CHANGE_LOGIN:
                        boolean result = AuthService.changeLogin(this.getNick(), event.getArgs().get(0));
                        if (result) {
                            Event event1 = new Event(this.getNick(), EventType.MESSAGE, Collections.singletonList("User " + this.getNick() + " updated login to " + event.getArgs().get(0)));
                            sendEvent(event1);
                        } else {
                            Event event1 = new Event(this.getNick(), EventType.MESSAGE, Collections.singletonList("Error updating login to " + event.getArgs().get(0) + " by user" + this.getNick()));
                            sendEvent(event1);
                        }
                        continue;
                    case END: {
                        Event serverCloseEvent = new Event(this.getNick(), EventType.SERVER_CLOSED, null);
                        sendEvent(serverCloseEvent);
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
