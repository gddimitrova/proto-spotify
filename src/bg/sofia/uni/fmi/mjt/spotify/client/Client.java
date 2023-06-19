package bg.sofia.uni.fmi.mjt.spotify.client;

import bg.sofia.uni.fmi.mjt.spotify.command.AvailableCommands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Client {
    private static final int SERVER_PORT = 15157;
    private static final String SERVER_HOST = "localhost";
    private final ClientCommandLineHandler clientHelper = new ClientCommandLineHandler();

    public void startClient() {

        try (
            SocketChannel commandsChannel = SocketChannel.open();
            SocketChannel playChannel = SocketChannel.open();
            BufferedReader commandsReader =
                    new BufferedReader(Channels.newReader(commandsChannel, StandardCharsets.ISO_8859_1));
            PrintWriter commandsWriter =
                    new PrintWriter(Channels.newWriter(commandsChannel, StandardCharsets.UTF_8), true);
            BufferedReader playReader =
                    new BufferedReader(Channels.newReader(playChannel, StandardCharsets.ISO_8859_1));
            PrintWriter playWriter =
                    new PrintWriter(Channels.newWriter(playChannel, StandardCharsets.UTF_8), true);
            Scanner scanner = new Scanner(System.in)
        ) {

            commandsChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            playChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));

            while (true) {
                String message = clientHelper.startUpCommand(scanner);

                if (message == null) {
                    break;
                }

                if (message.isBlank()) {
                    continue;
                }

                String command = clientHelper.getCommand(message);
                String line;

                if (command.equals(AvailableCommands.PLAY.getName())) {
                    playWriter.println(message);
                    if ((line = playReader.readLine()) != null) {

                        if (clientHelper.isError(line)) {
                            clientHelper.handleError(line);
                            continue;
                        }

                        Thread newThread = new Thread(() -> clientHelper.playFunction(line, playChannel));
                        newThread.start();
                    }

                } else {
                    commandsWriter.println(message);

                    line = commandsReader.readLine();
                    clientHelper.handleServerResponse(command, line);
                }
            }
        } catch (IOException e) {
            System.out.println("Unable to connect to the server. Please try again later");
        }
    }

    public static void main(String[] args) {
        new Client().startClient();
    }

}
