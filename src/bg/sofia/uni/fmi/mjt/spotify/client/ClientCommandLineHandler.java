package bg.sofia.uni.fmi.mjt.spotify.client;

import bg.sofia.uni.fmi.mjt.spotify.command.AvailableCommands;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ClientCommandLineHandler {


    private static final int BUFFER_MAX = 100_000;
    private static final String SPACE = " ";

    private static final String PROMPT = "> ";

    private static final int ENCODING = 0;
    private static final int RATE = 1;
    private static final int SIZE_BITS = 2;
    private static final int CHANNELS = 3;
    private static final int FRAME = 4;
    private static final int FRAME_RATE = 5;
    private static final int ENDIAN = 6;

    private static boolean isFirstCommand = true;

    private int userId;
    private int currentFrameSize;

    public List<String> commandsWithReply = new ArrayList<>(Arrays.asList(
            AvailableCommands.TOP.getName(),
            AvailableCommands.SHOW_PLAYLIST.getName(),
            AvailableCommands.SEARCH.getName(),
            AvailableCommands.CREATE_PLAYLIST.getName(),
            AvailableCommands.ADD_SONG.getName()
    ));
    private static void helpCommand() {
        System.out.println(
            """
            
            *******************************************
            Please enter any of the following commands:
            register <email> <password>
            login <email> <password>
            disconnect
            search <words>
            top <number>
            create-playlist <name_of_the_playlist>
            add-song-to <name_of_the_playlist> <song>
            show-playlist <name_of_the_playlist>
            play <song>
            stop
            *******************************************
            """
        );
    }
    public String startUpCommand(Scanner scanner) {
        String message;

        if (isFirstCommand) {
            isFirstCommand = false;
            System.out.println("Hello! Welcome to Spotify!");
            System.out.println("If you need help type \"help\"");
            System.out.println("To start, please login or register");
        }
        System.out.print("Please enter command: ");

        message = scanner.nextLine();

        if (AvailableCommands.DISCONNECT.getName().equals(message)) {
            return null;
        }
        if (AvailableCommands.HELP.getName().equals(message)) {
            helpCommand();
            return startUpCommand(scanner);
        }

        if (message.isBlank()) {
            return startUpCommand(scanner);
        }

        return userId + SPACE + message.strip();
    }

    public String getCommand(String message) {

        return message.split(SPACE)[1];
    }

    public boolean isError(String line) {
        return line.contains("ERROR:");
    }

    public void handleError(String line) {
        printLine(PROMPT + line);
    }

    public void printLine(String line) {
        System.out.println(line);
    }
    public void handleServerResponse(String command, String line) {
        if (line == null) {
            return;
        }

        if (isError(line)) {
            handleError(line);
            return;
        }

        if (command.equals(AvailableCommands.REGISTER.getName()) || command.equals(AvailableCommands.LOGIN.getName())) {
            userId = Integer.parseInt(line);
            printLine( PROMPT + "Successful " + command + "!");
            return;
        }

        if (commandsWithReply.contains(command)) {
            printLine(PROMPT + line);
        }
    }

    public DataLine.Info createDataLine(String line) {
        if (line == null) {
            return null;
        }

        String[] lineArguments = line.split(SPACE);
        AudioFormat.Encoding encoding = new AudioFormat.Encoding(lineArguments[ENCODING]);
        float sampleRate = Float.parseFloat(lineArguments[RATE]);
        int sampleSizeInBits = Integer.parseInt(lineArguments[SIZE_BITS]);
        int channels = Integer.parseInt(lineArguments[CHANNELS]);
        this.currentFrameSize = Integer.parseInt(lineArguments[FRAME]);
        float frameRate = Float.parseFloat(lineArguments[FRAME_RATE]);
        boolean bigEndian = Boolean.parseBoolean(lineArguments[ENDIAN]);

        AudioFormat format = new AudioFormat(encoding, sampleRate, sampleSizeInBits,
                channels, currentFrameSize, frameRate, bigEndian);

        return new DataLine.Info(SourceDataLine.class, format);
    }

    public void playFunction(String line, SocketChannel playChannel) {
        DataLine.Info info = createDataLine(line);

        try {
            SourceDataLine dataLine = (SourceDataLine) AudioSystem.getLine(info);
            dataLine.open();
            dataLine.start();

            ByteBuffer buff = ByteBuffer.allocate(BUFFER_MAX);

            int byteCount;
            while ((byteCount = playChannel.read(buff)) != -1) {
                buff.flip();

                int remaining = buff.remaining();
                remaining = remaining % currentFrameSize > 0 ?
                        (remaining / currentFrameSize) * currentFrameSize : remaining;

                byte[] byteArr = new byte[remaining];
                buff.get(byteArr);

                dataLine.write(byteArr, 0, remaining);
                buff.clear();

                if (byteCount == 1) {
                    break;
                }
            }

            dataLine.drain();
            dataLine.close();
        } catch (LineUnavailableException | IOException e) {
            handleError(e.getMessage());
        }
    }
}
