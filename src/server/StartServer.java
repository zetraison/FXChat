package server;

import core.config.ConfigLoader;

public class StartServer {
    private static int port = Integer.parseInt(ConfigLoader.load().getProperty("server.port"));

    public static void main(String[] args) {
        new Server(port);
    }
}
