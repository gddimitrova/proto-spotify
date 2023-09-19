import bg.sofia.uni.fmi.mjt.spotify.client.Client;
import bg.sofia.uni.fmi.mjt.spotify.client.SpotifyClient;

import java.net.InetSocketAddress;

public class MainClient {
    private static final int SERVER_PORT = 15157;
    private static final String SERVER_HOST = "localhost";

    public static void main(String[] args) {
        Client client = new SpotifyClient(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
        Thread clientThread = new Thread(client);
        clientThread.start();
    }
}
