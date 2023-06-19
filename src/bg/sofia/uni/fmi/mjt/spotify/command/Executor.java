package bg.sofia.uni.fmi.mjt.spotify.command;

import bg.sofia.uni.fmi.mjt.spotify.exceptions.InvalidCommandException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.InvalidEmailException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.InvalidPasswordException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.NoSuchCommandException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.NoSuchPlaylistException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.NoSuchSongException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.NoSuchUserException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.PlaylistAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.SpotifyAccountAlreadyExistsException;
import bg.sofia.uni.fmi.mjt.spotify.exceptions.SpotifyExceptions;
import bg.sofia.uni.fmi.mjt.spotify.song.Song;
import bg.sofia.uni.fmi.mjt.spotify.user.User;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Executor {

    public static final String REGISTER = "register";
    public static final String LOGIN = "login";
    public static final String SEARCH = "search";
    public static final String TOP = "top";
    public static final String CREATE_PLAYLIST = "create-playlist";
    public static final String ADD_SONG_TO = "add-song-to";
    public static final String SHOW_PLAYLIST = "show-playlist";
    public static final String PLAY_SONG = "play";
    public static final String STOP = "stop";
    private static final String SPACE = " ";
    private final Map<Integer, User> users;
    private final Map<String, Integer> emails;
    private final Map<String, Song> songs;
    private final List<String> playlist;
    private static int currId = 0;
    private static final int ZERO = 0;
    private static final int ONE = 1;
    private static final int TWO = 2;
    private static final String PATH_TO = "playlists" + File.separator;
    private static final String LOWER_LETTER = ".*[a-z].*";
    private static final String UPPER_LETTER = ".*[A-Z].*";
    private static final String DIGIT = ".*[\\d].*";
    private static final String CHARACTER = ".*[,$!@#?].*";
    private static final String MAIL_REGEX = "^[a-zA-Z\\d\\-@$!%*?&_.]+@[a-z.]+.(com|bg)$";
    private static final int MIN_LETTERS = 8;
    private final Reader userReader;
    private final Writer userWriter;
    private final Reader songReader;

    public Executor(Reader userReader, Writer userWriter, Reader songReader) {
        this.userReader = userReader;
        this.userWriter = userWriter;
        this.songReader = songReader;

        this.users = new HashMap<>();
        this.emails = new HashMap<>();
        this.songs = new HashMap<>();
        this.playlist = new ArrayList<>();

        setUpUsers();
        setUpSongs();
        setUpPlaylists();
    }

    private void setUpUsers() {
        try (var bufferedReader = new BufferedReader(userReader)) {
            List<User> userList = bufferedReader.lines()
                    .skip(ONE)
                    .filter(p -> !p.isBlank())
                    .map(User::of)
                    .toList();

            if (userList.size() > ZERO) {
                currId = userList.get(userList.size() - ONE).id();

                for (User each : userList) {
                    users.put(each.id(), each);
                    emails.put(each.email(), each.id());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        currId++;

    }

    private void setUpSongs() {
        try (var bufferedReader = new BufferedReader(songReader)) {
            List<Song> songsList = bufferedReader.lines()
                    .filter(p -> !p.isBlank())
                    .map(Song::of)
                    .toList();

            for (Song each : songsList) {
                songs.put(each.getName().toLowerCase(), each);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private void setUpPlaylists() {
        Path dir = Path.of("playlists");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path each : stream) {
                playlist.add(each.getName(ONE).toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void validateCommand(Command newCommand) throws SpotifyExceptions {
        List<String> commands = Arrays.stream(AvailableCommands.values())
                .map(AvailableCommands::getName)
                .toList();

        int id = newCommand.id();

        if (id == ZERO && !newCommand.command().equals(AvailableCommands.REGISTER.getName())
                && !newCommand.command().equals(AvailableCommands.LOGIN.getName())) {
            throw new NoSuchUserException("First you need to login or register!");
        }

        if (!commands.contains(newCommand.command())) {
            throw new NoSuchCommandException("Given command does not exists");
        }
    }

    public String execute(String input) throws SpotifyExceptions, IOException {
        Command newCommand = Command.of(input);
        validateCommand(newCommand);

        return switch (newCommand.command()) {
            case REGISTER -> register(newCommand.arguments());
            case LOGIN -> login(newCommand.arguments());
            case SEARCH -> search(newCommand.arguments());
            case TOP -> top(newCommand.arguments());
            case CREATE_PLAYLIST -> createPlaylist(newCommand.id(), newCommand.arguments());
            case ADD_SONG_TO -> addSongTo(newCommand.id(), newCommand.arguments());
            case SHOW_PLAYLIST -> showPlaylist(newCommand.id(), newCommand.arguments());
            case PLAY_SONG -> playSong(newCommand.arguments());
            case STOP -> stop(newCommand.arguments());
            default -> null;
        };
    }

    private String stop(String... arguments) throws InvalidCommandException {
        if (arguments.length != ZERO) {
            throw new InvalidCommandException("Command stop should not be followed by any arguments!");
        }

        return "STOP;";
    }

    private void rewriteFile() throws IOException {
        try (var writer = new BufferedWriter(new FileWriter("availableSongs.txt", false))) {
            for (Song song : songs.values()) {
                writer.write(song.getFilename() + SPACE + song.getCountPlays() + System.lineSeparator());
            }
        }
    }
    private String playSong(String... arguments) throws NoSuchSongException, IOException, InvalidCommandException {
        if (arguments.length == ZERO) {
            throw new InvalidCommandException("Not enough arguments! Please provide a song name");
        }

        String songName = String.join(SPACE, arguments).toLowerCase();

        if (!songs.containsKey(songName)) {
            throw new NoSuchSongException("There is no song with the specified name");
        }
        String fileName = "songs" + File.separator + songs.get(songName).getFilename() + ".wav";
        songs.get(songName).incrementCount();

        rewriteFile();

        return "PLAY;" + fileName;
    }

    private String showPlaylist(int id, String... arguments)
            throws NoSuchPlaylistException, InvalidCommandException, IOException {

        if (arguments.length == ZERO) {
            throw new InvalidCommandException("Not enough arguments! Please provide a playlist name");
        }

        String file = id + SPACE + String.join(" ", arguments) + ".txt";

        if (!playlist.contains(file)) {
            throw new NoSuchPlaylistException("Playlist with this name does not exist!");
        }

        String path = PATH_TO + file;
        List<String> songs;
        try (var reader = new BufferedReader(new FileReader(path))) {
            songs = reader.lines()
                    .map(Song::of)
                    .map(Song::toString)
                    .distinct()
                    .toList();
        }

        return songs.toString();
    }

    private String addSongTo(int id, String... arguments) throws SpotifyExceptions, IOException {

        if (arguments.length < TWO) {
            throw new InvalidCommandException("Not enough arguments! Please provide a song and a playlist");
        }

        String fileName = id + SPACE + arguments[ZERO] + ".txt";

        if (!playlist.contains(fileName)) {
            throw new NoSuchPlaylistException("Playlist with this name does not exist!");
        }

        if (!songs.containsKey(arguments[ONE])) {
            throw new NoSuchSongException("There is no such song!");
        }

        Song toBeAdded = songs.get(arguments[ONE]);
        String path = PATH_TO + fileName;

        try (var writer = new BufferedWriter(new FileWriter(path, true))) {
            writer.write(toBeAdded.getFilename() + SPACE + toBeAdded.getCountPlays());
            writer.write(System.lineSeparator());
        }

        return "Song successfully added to the playlist!";
    }

    private String createPlaylist(int id, String... arguments) throws SpotifyExceptions, IOException {
        if (arguments.length == ZERO) {
            throw new InvalidCommandException("Please provide name for the playlist!");
        }

        String fileName = id + SPACE + String.join(" ", arguments) + ".txt";
        if (playlist.contains(fileName)) {
            throw new PlaylistAlreadyExistsException("Playlist with the same name already exists");
        }

        playlist.add(fileName);
        File newPlaylist = new File(PATH_TO + fileName);

        newPlaylist.createNewFile();

        return "Playlist created successfully!";
    }
    private String top(String... argument) throws InvalidCommandException {
        if (argument.length != ONE) {
            throw new InvalidCommandException("Not enough arguments! Please provide how many songs you want to see");
        }

        return songs.values().stream()
                .sorted(Comparator.comparing(Song::getCountPlays).reversed())
                .limit(Integer.parseInt(argument[ZERO]))
                .map(Song::toString)
                .toList()
                .toString();
    }

    private String search(String... arguments) throws InvalidCommandException {
        List<String> argList = Arrays.stream(arguments).map(String::toLowerCase).toList();

        if (arguments.length == ZERO) {
            throw new InvalidCommandException("Not enough arguments! No search query provided");
        }

        return songs.values().stream()
                .filter(p -> new HashSet<>(Arrays.stream((p.getName() + SPACE + p.getAuthor())
                                .toLowerCase().split(SPACE))
                        .toList())
                        .containsAll(argList))
                .map(Song::toString)
                .toList()
                .toString();
    }

    private String login(String... arguments) throws SpotifyExceptions {
        if (arguments.length != TWO) {
            throw new InvalidCommandException("Not enough arguments! Please provide email and password");
        }
        if (!emails.containsKey(arguments[ZERO])) {
            throw new NoSuchUserException("User with this email does not exist!");
        }

        int returnId = emails.get(arguments[ZERO]);

        if (!users.get(returnId).password().equals(arguments[ONE])) {
            throw new InvalidPasswordException("Wrong password!");
        }

        return Integer.toString(returnId);
    }

    private String register(String... arguments) throws SpotifyExceptions, IOException {
        if (arguments.length != TWO) {
            throw new InvalidCommandException("Not enough arguments! Please provide email and password");
        }

        if (emails.containsKey(arguments[ZERO])) {
            throw new SpotifyAccountAlreadyExistsException("An account with this email already exists!");
        }

        if (!arguments[ZERO].matches(MAIL_REGEX)) {
            throw new InvalidEmailException("Make sure your email is in the following format: username@domain.com/bg");
        }

        String pass = arguments[ONE];
        if (pass.length() < MIN_LETTERS || !pass.matches(LOWER_LETTER) || !pass.matches(UPPER_LETTER) ||
                !pass.matches(DIGIT) || !pass.matches(CHARACTER)) {
            throw new InvalidPasswordException("Make sure your password is at least 8 characters, using letters," +
                    " numbers and special character [, $ ! @ # ?]");
        }

        int id = currId++;
        String newLine = String.join(SPACE, String.valueOf(id), arguments[ZERO], arguments[ONE]);
        try (var writer = new BufferedWriter(userWriter)) {
            writer.write(newLine);
            writer.write(System.lineSeparator());
        }
        emails.put(arguments[ZERO], id);
        users.put(id, User.of(newLine));


        return Integer.toString(id);
    }

    public Map<Integer, User> getUsers() {
        return Collections.unmodifiableMap(users);
    }

    public Map<String, Integer> getEmails() {
        return Collections.unmodifiableMap(emails);
    }

    public Map<String, Song> getSongs() {
        return Collections.unmodifiableMap(songs);
    }

    public List<String> getPlaylist() {
        return Collections.unmodifiableList(playlist);
    }
}
