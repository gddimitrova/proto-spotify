package bg.sofia.uni.fmi.mjt.spotify.server;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;

public class ServerPlay implements Runnable {
    private static final String SPACE = " ";
    private static final int STREAM_BUFF = 100_000;
    private static final int SONG_FRAME_SLEEP_TIME = 500;

    private final ServerErrorHandler errorHandler;
    private final SocketChannel commandChannel;
    private final String fileName;
    private final int currId;
    private final String clientInput;
    private final ByteBuffer buffer;
    private final ConcurrentHashMap<Integer, Boolean> stopMap;

    public ServerPlay(ServerErrorHandler errorHandler, SocketChannel commandChannel, String filename, int id,
                      String clientInput, ByteBuffer buffer, ConcurrentHashMap<Integer, Boolean> stopMap) {
        this.errorHandler = errorHandler;
        this.commandChannel = commandChannel;
        this.fileName = filename;
        this.currId = id;
        this.clientInput = clientInput;
        this.buffer = buffer;
        this.stopMap = stopMap;
    }

    @Override
    public void run() {
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
            errorHandler.handleSystemError(clientInput, e);
        }
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

    private void writeClient(SocketChannel clientChannel, byte[] output, boolean sendNewLine) throws IOException {
        buffer.clear();
        buffer.put(output);
        if (sendNewLine) {
            buffer.put(System.lineSeparator().getBytes());
        }
        buffer.flip();

        clientChannel.write(buffer);
    }
}
