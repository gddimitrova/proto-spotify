package bg.sofia.uni.fmi.mjt.spotify.server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ServerErrorHandler {
    private static final String ERRORS_FILE = "errors.txt";
    private static final String ERROR = "ERROR:";
    private static final String SPACE = " ";

    private final ByteBuffer buffer;


    public ServerErrorHandler(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public void handleSystemError(String clientInput, Exception e) {
        StringBuilder message = new StringBuilder(clientInput + System.lineSeparator());
        message.append(e.getMessage()).append(System.lineSeparator());

        StackTraceElement[] stackTrace = e.getStackTrace();
        for (StackTraceElement ste : stackTrace) {
            message.append(ste.toString()).append(System.lineSeparator());
        }

        writeErrorInFile(message.toString());
    }

    private void writeErrorInFile(String message) {
        try (var writer = new BufferedWriter(new FileWriter(ERRORS_FILE, true))) {
            writer.write(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeClientError(SocketChannel clientChannel, String... arguments) throws IOException {
        String errorMessage = String.join(SPACE, arguments);
        writeClient(clientChannel, errorMessage);
    }

    private void writeClient(SocketChannel clientChannel, String error) throws IOException {
        buffer.clear();
        buffer.put(error.getBytes());

        buffer.put(System.lineSeparator().getBytes());
        buffer.flip();

        clientChannel.write(buffer);
    }
}
