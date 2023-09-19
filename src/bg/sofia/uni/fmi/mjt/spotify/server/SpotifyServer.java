package bg.sofia.uni.fmi.mjt.spotify.server;

import bg.sofia.uni.fmi.mjt.spotify.command.AvailableCommands;
import bg.sofia.uni.fmi.mjt.spotify.command.Executor;
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
    private final InetSocketAddress socketAddress;
    private final ByteBuffer buffer;
    private final Executor executor;

    private final ConcurrentHashMap<Integer, Boolean> stopMap;
    private final ServerErrorHandler serverErrorHandler;
    private boolean shouldRun;
    private Selector selector;

    public SpotifyServer(Executor commandExecutor, InetSocketAddress socketAddress) {
        this.socketAddress = socketAddress;
        this.stopMap = new ConcurrentHashMap<>();
        this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.executor = commandExecutor;
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
            System.out.println(String.join(SPACE, "There is a problem with the server socket!", e.toString()));
            serverErrorHandler.handleSystemError("IO Exception", e);
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

        String output = executor.execute(clientInput);
        boolean isAudioOperation = output.contains(SEMICOLON);


        if (isAudioOperation) {
            boolean shouldPlaySong = output.contains("PLAY;");
            boolean shouldStopSong = output.contains("STOP;");
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
                writeClient(commandChannel, AvailableCommands.STOP.getName().getBytes());
            }

        } else {
            writeClient(commandChannel, output.getBytes());
        }
    }


    private void writeClient(SocketChannel clientChannel, byte[] output) throws IOException {
        buffer.clear();
        buffer.put(output);
        buffer.put(System.lineSeparator().getBytes());

        buffer.flip();
        clientChannel.write(buffer);
    }
    private void  acceptClient(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel clientAccepted = server.accept();
        clientAccepted.configureBlocking(false);
        clientAccepted.register(selector, SelectionKey.OP_READ);

        System.out.println("Connection accepted for client " + clientAccepted.getRemoteAddress());
    }

    private String getClientInput(SocketChannel clientChannel) throws IOException {
        buffer.clear();
        int r = clientChannel.read(buffer);
        if (r < 0) {
            System.out.println("Client has closed the connection");
            clientChannel.close();
            return null;
        }

        buffer.flip();
        return new String(buffer.array(), 0, buffer.remaining()).strip();
    }
}