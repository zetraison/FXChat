package server;

import client.EventEnum;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                String str = in.readUTF();
                List<String> tokens = Arrays.asList(str.split(" "));
                String event = tokens.get(0);
                System.out.println("[EVENT]: " + tokens);

                EventEnum eventEnum = EventEnum.fromValue(event);

                switch (eventEnum) {
                    case AUTH: {
                        String nick = null;
                        if (tokens.get(1) != null && tokens.get(2) != null) {
                            nick = AuthService.getNickname(tokens.get(1), tokens.get(2));
                        }
                        if (nick == null) {
                            sendEvent(EventEnum.ERROR.getValue(), "Incorrect logo/pass!");
                            continue;
                        }
                        if (server.isNickBusy(nick)) {
                            sendEvent(EventEnum.ERROR.getValue(), "Account already used!");
                            continue;
                        }
                        this.nick = nick;
                        this.blackList = AuthService.getUserBlacklist(nick);
                        sendEvent(EventEnum.AUTH_OK.getValue(), nick);
                        server.subscribe(ClientHandler.this);
                        continue;
                    }
                    case REGISTER: {
                        if (tokens.get(1) != null && tokens.get(2) != null && tokens.get(3) != null) {
                            String username = tokens.get(1);
                            String login = tokens.get(2);
                            String passwordHash = AuthService.MD5(tokens.get(3));

                            AuthService.addUser(username, login, passwordHash);
                        } else {
                            sendEvent(EventEnum.ERROR.getValue(), "Incorrect username/logo/pass!");
                        }
                        continue;
                    }
                    case HELP: {
                        sendEvent(EventEnum.HELP.getValue(), nick);
                        continue;
                    }
                    case PRIVATE_MESSAGE: {
                        server.privateEvent(tokens.get(1), EventEnum.MESSAGE.getValue(), tokens.get(2), String.join(" ", tokens.subList(3, tokens.size())));
                        continue;
                    }
                    case MESSAGE: {
                        server.broadcastEvent(this, EventEnum.MESSAGE.getValue(), tokens.get(1), String.join(" ", tokens.subList(2, tokens.size())));
                        continue;
                    }
                    case STICKER: {
                        server.broadcastEvent(this, EventEnum.STICKER.getValue(), tokens.get(1), tokens.get(2));
                        continue;
                    }
                    case IMAGE: {
                        server.broadcastEvent(this, EventEnum.IMAGE.getValue(), tokens.get(1), tokens.get(2));
                        continue;
                    }
                    case BLACKLIST: {
                        boolean result = AuthService.addToBlacklist(this.getNick(), tokens.get(1));
                        this.blackList = AuthService.getUserBlacklist(nick);
                        if (result) {
                            sendEvent(EventEnum.MESSAGE.getValue(), this.nick, "Add user " + tokens.get(1) + " to blacklist");
                        } else {
                            sendEvent(EventEnum.MESSAGE.getValue(), this.nick, "Error on adding user " + tokens.get(1) + " to blacklist");
                        }
                        continue;
                    }
                    case WHITELIST: {
                        boolean result = AuthService.removeFromBlacklist(this.getNick(), tokens.get(1));
                        this.blackList = AuthService.getUserBlacklist(nick);
                        if (result) {
                            sendEvent(EventEnum.MESSAGE.getValue(), this.nick, "Remove user " + tokens.get(1) + " from blacklist");
                        } else {
                            sendEvent(EventEnum.MESSAGE.getValue(), this.nick, "Error on removind user " + tokens.get(1) + " to blacklist");
                        }
                        continue;
                    }
                    case END: {
                        sendEvent(EventEnum.SERVER_CLOSED.getValue());
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
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            server.unsubscribe(ClientHandler.this);
        }
    }

    public void sendEvent(String ... args) {
        try {
            out.writeUTF(String.join(" ", Arrays.asList(args)));
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
