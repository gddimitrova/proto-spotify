package bg.sofia.uni.fmi.mjt.spotify.server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class ServerHelper {
    private static final String ERRORS_FILE = "errors.txt";

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

}
