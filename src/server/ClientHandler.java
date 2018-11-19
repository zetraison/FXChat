package server;

import client.EventEnum;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ClientHandler {

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Server server;
    private String nick;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.socket = socket;
            this.server = server;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());

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
                        String nick = AuthService.getNickname(tokens.get(1), tokens.get(2));
                        if (nick == null) {
                            sendEvent(EventEnum.ERROR.getValue(), "Incorrect logo/pass");
                            continue;
                        }
                        this.nick = nick;
                        sendEvent(EventEnum.AUTH_OK.getValue(), nick);
                        server.subscribe(ClientHandler.this);
                        server.broadcastEvent(EventEnum.USER_LOGIN.getValue(), server.getClients().stream()
                                .map(ClientHandler::getNick).collect(Collectors.joining(" ")));
                        continue;
                    }
                    case PRIVATE_MESSAGE: {
                        server.personalMsg(tokens.get(1), EventEnum.MESSAGE.getValue(), tokens.get(2), String.join(" ", tokens.subList(3, tokens.size())));
                        continue;
                    }
                    case END: {
                        sendEvent(EventEnum.SERVER_CLOSED.getValue());
                        break;
                    }
                    case MESSAGE: {
                        server.broadcastEvent(EventEnum.MESSAGE.getValue(), tokens.get(1), String.join(" ", tokens.subList(2, tokens.size())));
                        continue;
                    }
                    case STICKER: {
                        server.broadcastEvent(EventEnum.STICKER.getValue(), tokens.get(1), tokens.get(2));
                    }
                }
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
}
