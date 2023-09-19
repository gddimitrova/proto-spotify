package bg.sofia.uni.fmi.mjt.spotify.server;

import bg.sofia.uni.fmi.mjt.spotify.command.AvailableCommands;
import bg.sofia.uni.fmi.mjt.spotify.command.Executor;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.SpotifyExceptions;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
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
    private final InetSocketAddress socketAddress;
    private static final int BUFFER_SIZE = 1_000_000_012;
    private static final int STREAM_BUFF = 100_000;
    private static final int SONG_FRAME_SLEEP_TIME = 500;
    private final ByteBuffer buffer;
    private final Executor executor;
    private static final String ERROR = "ERROR:";
    private static final String SPACE = " ";
    private static final String SEMICOLON = ";";
    private final ConcurrentHashMap<Integer, Boolean> stopMap;
    private final ServerHelper serverHelper;
    private boolean shouldRun;
    private Selector selector;

    public SpotifyServer(Executor commandExecutor, InetSocketAddress socketAddress) {
        this.socketAddress = socketAddress;
        this.stopMap = new ConcurrentHashMap<>();
        this.buffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.executor = commandExecutor;
        this.serverHelper = new ServerHelper();
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

                    } else if (key.isReadable()) {
                        SocketChannel commandChannel = (SocketChannel) key.channel();

                        String clientInput = getClientInput(commandChannel);
                        if (clientInput == null) {
                            continue;
                        }

                        try {
                            executionInput(commandChannel, clientInput);
                        } catch (SpotifyExceptions e) {
                            String errorMessage = String.join(SPACE, ERROR, e.getMessage());
                            writeClient(commandChannel, errorMessage.getBytes(), true);

                        } catch (UnsupportedAudioFileException | InterruptedException e) {
                            String clientId = "Client ID: " + clientInput.substring(0, clientInput.indexOf(SPACE));
                            String errorMessage = String.join(SPACE, ERROR, e.getMessage(), clientId);
                            writeClient(commandChannel, errorMessage.getBytes(), true);
                            serverHelper.handleSystemError(clientInput, e);
                        }
                    }
                    keyIterator.remove();
                }
            }
            selector.close();
        } catch (IOException e) {
            String message = "There is a problem with the server socket!" + SPACE + e;
            System.out.println(message);
            serverHelper.handleSystemError("IO Exception", e);
        }

    }

    public void stop() {
        shouldRun = false;

        if (selector.isOpen()) {
            selector.wakeup();
        }
    }

    private void executionInput(SocketChannel commandChannel, String clientInput) throws IOException, SpotifyExceptions,
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

                Thread newThread = new Thread(() -> {
                    try {
                        playFunction(commandChannel, fileName, currId);
                    } catch (UnsupportedAudioFileException | InterruptedException | IOException e) {
                        String message = "ERROR occurred while playing song!";
                        System.out.println(message);
                        try {
                            writeClient(commandChannel, message.getBytes(), true);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        serverHelper.handleSystemError(clientInput, e);
                    }
                });
                newThread.start();
            }
            if (shouldStopSong) {
                stopMap.put(currId, true);
                writeClient(commandChannel, AvailableCommands.STOP.getName().getBytes(), true);
            }

        } else {
            writeClient(commandChannel, output.getBytes(), true);
        }
    }


    private void writeClient(SocketChannel clientChannel, byte[] output, boolean sendNewLine) throws IOException {
        buffer.clear();
        buffer.put(output);
        if (sendNewLine) {
            buffer.put(System.lineSeparator().getBytes());
        }
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

    private void playFunction(SocketChannel clientChannel, String fileName, int id)
            throws UnsupportedAudioFileException, IOException, InterruptedException {

        AudioInputStream stream = AudioSystem.getAudioInputStream(new File(fileName));
        AudioFormat audioFormat = stream.getFormat();

        String newString = audioFormat.getEncoding() + SPACE + audioFormat.getSampleRate() + SPACE +
                audioFormat.getSampleSizeInBits() + SPACE + audioFormat.getChannels()
                + SPACE + audioFormat.getFrameSize() + SPACE + audioFormat.getFrameRate()
                + SPACE + audioFormat.isBigEndian();

        writeClient(clientChannel, newString.getBytes(), true);

        byte[] bytesBuffer = new byte[STREAM_BUFF];
        while (stream.read(bytesBuffer, 0, bytesBuffer.length) != -1 && !stopMap.get(id)) {
            writeClient(clientChannel, bytesBuffer, false);
            Thread.sleep(SONG_FRAME_SLEEP_TIME);
        }

        writeClient(clientChannel, new byte[1], false);
        stream.close();
    }
}
