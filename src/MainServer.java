import bg.sofia.uni.fmi.mjt.spotify.command.Executor;
import bg.sofia.uni.fmi.mjt.spotify.server.Server;
import bg.sofia.uni.fmi.mjt.spotify.server.SpotifyServer;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;

public class MainServer {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 15157;
    private static final String USERS_FILE = "userData.txt";
    private static final String SONGS_FILE = "availableSongs.txt";

    public static void main(String[] args) {
        try (
                Reader userReader = new FileReader(USERS_FILE);
                Writer userWriter = new FileWriter(USERS_FILE, true);
                Reader songsReader = new FileReader(SONGS_FILE)
        ) {
            Executor commandExecutor = new Executor(userReader, userWriter, songsReader );

            Server server = new SpotifyServer(commandExecutor, new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            Thread serverThread = new Thread(server);
            serverThread.start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
