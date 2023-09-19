package bg.sofia.uni.fmi.mjt.spotify.server;

import bg.sofia.uni.fmi.mjt.spotify.command.AvailableCommands;
import bg.sofia.uni.fmi.mjt.spotify.command.CommandExecutor;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.SpotifyExceptions;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SpotifyServer implements Server {
    private static final int BUFFER_SIZE = 1_000_000_012;
    private static final String ERROR = "ERROR:";
    private static final String SPACE = " ";
    private static final String SEMICOLON = ";";
    private static final String PLAY = "PLAY;";
    private static final String STOP = "STOP";
    private static final String SERVER_ERROR = "An unexpected error occurred with the server!";
    private static final String CONNECTION_CLOSED = "Client has closed the connection";
    private static final String CONNECTION_ACCEPTED = "Connection accepted for client ";

    private final ServerErrorHandler serverErrorHandler;
    private final InetSocketAddress socketAddress;
    private final ByteBuffer buffer;
    private final CommandExecutor commandExecutor;
    private Selector selector;

    private final ConcurrentHashMap<Integer, Boolean> stopMap;
    private boolean shouldRun;

    public SpotifyServer(CommandExecutor commandExecutor, InetSocketAddress socketAddress) {
        this.socketAddress = socketAddress;
        this.stopMap = new ConcurrentHashMap<>();
        this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.commandExecutor = commandExecutor;
        this.serverErrorHandler = new ServerErrorHandler(this.buffer);
        this.shouldRun = true;
    }

    @Override
    public void run() {
        start();
    }

    @Override
    public void start() {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open()) {

            serverChannel.bind(socketAddress);
            serverChannel.configureBlocking(false);

            selector = Selector.open();
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("New Server is created!");

            while (shouldRun) {

                int readyChannels = selector.select();

                if (readyChannels == 0) {
                    continue;
                }

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();

                    if (key.isAcceptable()) {
                        acceptClient(key, selector);
                    }
                    if (key.isReadable()) {
                        SocketChannel commandChannel = (SocketChannel) key.channel();
                        processInput(commandChannel);
                    }
                    keyIterator.remove();
                }
            }
            selector.close();
        } catch (IOException e) {
            System.out.println(String.join(SPACE, SERVER_ERROR, e.toString()));
            serverErrorHandler.handleSystemError(SERVER_ERROR, e);
        }
    }

    public void stop() {
        shouldRun = false;

        if (selector.isOpen()) {
            selector.wakeup();
        }
    }

    private void processInput(SocketChannel commandChannel) throws IOException {
        String clientInput = getClientInput(commandChannel);

        if (clientInput == null) {
            return;
        }

        try {
            executeInput(commandChannel, clientInput);
        } catch (SpotifyExceptions e) {
            serverErrorHandler.writeClientError(commandChannel, ERROR, e.getMessage());
        } catch (UnsupportedAudioFileException | InterruptedException e) {
            String clientId = "Client ID: " + clientInput.substring(0, clientInput.indexOf(SPACE));
            serverErrorHandler.writeClientError(commandChannel, ERROR, e.getMessage(), clientId);
            serverErrorHandler.handleSystemError(clientInput, e);
        }
    }

    private void executeInput(SocketChannel commandChannel, String clientInput) throws IOException, SpotifyExceptions,
            UnsupportedAudioFileException, InterruptedException {

        String output = commandExecutor.execute(clientInput);
        boolean isAudioOperation = output.contains(SEMICOLON);

        if (isAudioOperation) {
            boolean shouldPlaySong = output.contains(PLAY);
            boolean shouldStopSong = output.contains(STOP);

            int currId = Integer.parseInt(clientInput.substring(0, clientInput.indexOf(" ")));

            if (shouldPlaySong) {
                stopMap.put(currId, false);
                String fileName = output.split(SEMICOLON)[1];

                ServerPlay serverPlay = new ServerPlay(serverErrorHandler, commandChannel, fileName, currId,
                        clientInput, buffer, stopMap);

                Thread newThread = new Thread(serverPlay);
                newThread.start();
            }
            if (shouldStopSong) {
                stopMap.put(currId, true);
                writeClient(commandChannel, AvailableCommands.STOP.getName());
            }
        } else {
            writeClient(commandChannel, output);
        }
    }

    private void writeClient(SocketChannel clientChannel, String output) throws IOException {
        buffer.clear();
        buffer.put(output.getBytes());
        buffer.put(System.lineSeparator().getBytes());

        buffer.flip();
        clientChannel.write(buffer);
    }

    private void  acceptClient(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();

        SocketChannel clientAccepted = server.accept();
        clientAccepted.configureBlocking(false);
        clientAccepted.register(selector, SelectionKey.OP_READ);

        System.out.println(CONNECTION_ACCEPTED + clientAccepted.getRemoteAddress());
    }

    private String getClientInput(SocketChannel clientChannel) throws IOException {
        buffer.clear();

        if (clientChannel.read(buffer) < 0) {
            System.out.println(CONNECTION_CLOSED);
            clientChannel.close();
            return null;
        }

        buffer.flip();
        return new String(buffer.array(), 0, buffer.remaining()).strip();
    }
}