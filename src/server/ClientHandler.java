package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
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

            new Thread(() -> {
                try {
                    while (true) {
                        String str = in.readUTF();
                        // /auth <login> <password>
                        if (str.startsWith("/auth")) {
                            String[] tokens = str.split(" ");
                            String newNick = AuthService.getNickname(tokens[1], tokens[2]);
                            if (newNick != null) {
                                sendMsg("/authok " + newNick);
                                nick = newNick;
                                server.subscribe(ClientHandler.this);
                                server.broadcastMsg("/userlogin " +
                                        server.getClients()
                                                .stream()
                                                .map(ClientHandler::getNick)
                                                .collect(Collectors.joining(" ")));
                            } else {
                                sendMsg("/error Incorrect_Logo/Pass");
                            }
                            continue;
                        }
                        // /w <username> <message>
                        if (str.startsWith("/w")) {
                            String[] tokens = str.split(" ");
                            server.sendMsgToClient(tokens[1], tokens[2]);
                            continue;
                        }
                        // /end
                        if (str.equals("/end")) {
                            out.writeUTF("/serverclosed");
                            break;
                        }
                        server.broadcastMsg(str);
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
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNick() {
        return nick;
    }
}
