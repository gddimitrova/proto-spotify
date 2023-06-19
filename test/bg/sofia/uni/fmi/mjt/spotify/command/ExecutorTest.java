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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExecutorTest {

    private static final String[] USER_NAMES = {"maria@gmail.com", "pesho@gmail.com", "toshko@gmail.com"};
    private static final String[] SONG_NAMES = {"numberCount", "Redbone", "song", "Why i love you"};
    private static Executor executor;
    private static final int ID = 0;
    private static final int USER_ID = 2;
    private static final String SPACE = " ";
    private static final String PLAYLISTS = "playlists";
    private static final String FILE_ONE = PLAYLISTS + File.separator + "2 testFile.txt";
    private static final String FILE_TWO = PLAYLISTS + File.separator + "2 newTestFile.txt";
    private static final int SIZE = 4;
    private static final String TEST_FILE = "\"testFile\"";
    private static final String SONG = "redbone";


    @BeforeAll
    public static void init() throws IOException {
        Reader userReader = initUsers();
        Writer userWriter = new StringWriter();
        Reader songReader = initSongs();

        File newDir = new File(PLAYLISTS);
        if (!newDir.exists()) {
            newDir.mkdir();
        }

        File newPlaylist = new File(PLAYLISTS + File.separator + "2 testFile.txt");

        if (newPlaylist.createNewFile()) {
            try (var writer = new BufferedWriter(new FileWriter(newPlaylist))) {
                writer.write("Redbone Childish_Gambino 0");
                writer.write(System.lineSeparator());
            }
        }

        executor = new Executor(userReader, userWriter, songReader);
    }

    @Test
    public void testEmailsCreatedSuccessfully() {
        assertTrue(executor.getEmails().keySet().containsAll(Arrays.stream(USER_NAMES).toList()),
                "Not all names contains in the email list!");
    }

    @Test
    public void testSongsCreatedSuccessfully() {
        assertEquals(executor.getSongs().size(), SIZE,
                "Invalid size of the songs list! Check creation of this list!");
        assertTrue(executor.getSongs().keySet().containsAll(Arrays.stream(SONG_NAMES)
                        .map(String::toLowerCase).toList()),
                "Not all songs contains in the song list!");
    }

    @Test
    public void testValidateCommandNotExistingThrowsException() {
        assertThrows(NoSuchCommandException.class, () -> executor.execute("2 execute"),
                "Unknown commands should throw exception!");
    }

    private String getInput(int id, String command, String... arguments) {
        StringBuilder message = new StringBuilder(id + SPACE + command);
        for (String argument : arguments) {
            message.append(SPACE).append(argument);
        }
        return message.toString();
    }

    @Test
    public void testRegisterInvalidArgumentsThrowsException() {
        String register = getInput(ID, AvailableCommands.REGISTER.getName());
        assertThrows(InvalidCommandException.class, () -> executor.execute(register),
                "Register with no arguments should throw exception!");
    }

    @Test
    public void testRegisterInvalidArgumentsMoreThanExpectedThrowsException() {
        String register = getInput(ID, AvailableCommands.REGISTER.getName(),
                "john@gmail.com", "tra", "volta");
        assertThrows(InvalidCommandException.class, () -> executor.execute(register),
                "Register with more than expected arguments should throw exception!");
    }

    @Test
    public void testRegisterWithExistingEmailThrowsException() {
        String register = getInput(ID, AvailableCommands.REGISTER.getName(), "pesho@gmail.com", "passWord");
        assertThrows(SpotifyAccountAlreadyExistsException.class, () -> executor.execute(register),
                "Register with mail that already exists should throw exception!");
    }

    @Test
    public void testRegisterWithInvalidPasswordFormatThrowsException() {
        String register = getInput(ID, AvailableCommands.REGISTER.getName(), "someone@gmail.com", "pass");
        assertThrows(InvalidPasswordException.class, () -> executor.execute(register),
                "Invalid password format should throw exception!");
    }

    @Test
    public void testRegisterWithInvalidEmailFormatThrowsException() {
        String register = getInput(ID, AvailableCommands.REGISTER.getName(), "someoneNew", "NewPa$$1");
        assertThrows(InvalidEmailException.class, () -> executor.execute(register));
    }

    @Test
    public void testRegisterWorksCorrectly() throws IOException, SpotifyExceptions {
        String register = getInput(ID, AvailableCommands.REGISTER.getName(),
                "newAccount@gmail.com", "NewPa$$12");
        executor.execute(register);
        assertEquals(executor.getUsers().size(), SIZE,
                "Adding new account does not work correctly!");
        assertTrue(executor.getEmails().containsKey("newAccount@gmail.com"),
                "Adding new account does not work correctly!");
        assertEquals(executor.getUsers().get(SIZE).email(), "newAccount@gmail.com",
                "Adding new account does not work correctly!");
    }

    @Test
    public void testLoginWithInvalidArgumentsThrowsException() {
        String login = getInput(ID, AvailableCommands.LOGIN.getName());
        assertThrows(InvalidCommandException.class, () -> executor.execute(login),
                "Login with no arguments should throw exception!");
    }

    @Test
    public void testLoginWithLessArgumentsThrowsException() {
        String login = getInput(ID, AvailableCommands.LOGIN.getName(), "pesho@gmail.com");
        assertThrows(InvalidCommandException.class, () -> executor.execute(login),
                "Login with less arguments should throw exception!");
    }

    @Test
    public void testLoginWithoutAccountThrowsException() {
        String login = getInput(ID, AvailableCommands.LOGIN.getName(), "kalina@abv.bg", "kaliPass");
        assertThrows(NoSuchUserException.class, () -> executor.execute(login),
                "Login without account should throw exception!");
    }

    @Test
    public void testLoginWithInvalidPasswordThrowsException() {
        String login = getInput(ID, AvailableCommands.LOGIN.getName(), "pesho@gmail.com", "pesho");
        assertThrows(InvalidPasswordException.class, () -> executor.execute(login),
                "Login with invalid password should throw exception!");
    }

    @Test
    public void testLoginWorksCorrectly() throws IOException, SpotifyExceptions {
        String login = getInput(ID, AvailableCommands.LOGIN.getName(), "pesho@gmail.com", "peshko");
        assertEquals(Integer.parseInt(executor.execute(login)), 2,
                "Login should return string of the user id!");
    }

    @Test
    public void testSearchWithoutWordsThrowsException() {
        String search = getInput(USER_ID, AvailableCommands.SEARCH.getName());
        assertThrows(InvalidCommandException.class, () -> executor.execute(search),
                "Searching without provided words should throw exception!");
    }

    @Test
    public void testSearchWithoutLoginOrRegisterThrowsException() {
        String search = getInput(ID, AvailableCommands.SEARCH.getName());
        assertThrows(NoSuchUserException.class, () -> executor.execute(search),
                "Searching for words before login or registration should throw exception!");
    }

    @Test
    public void testSearchingForMissingWordsWorksCorrectly() throws IOException, SpotifyExceptions {
        String search = getInput(USER_ID, AvailableCommands.SEARCH.getName(), "horse");
        assertEquals(executor.execute(search), "[]",
                "Should return empty list when non of the words match!");
    }

    @Test
    public void testSearchWorksCorrectly() throws IOException, SpotifyExceptions {
        String search = getInput(USER_ID, AvailableCommands.SEARCH.getName(), "why");
        String result = "[" + executor.getSongs().get("why i love you").toString() + "]";
        assertEquals(executor.execute(search), result,
                "Search method does not work correctly!");
    }

    @Test
    public void testTopWithoutArgumentsThrowsException() {
        String top = getInput(USER_ID, AvailableCommands.TOP.getName());
        assertThrows(InvalidCommandException.class, () -> executor.execute(top),
                "Top without arguments should throw exception!");
    }

    @Test
    public void testTopWithMoreThanOneArgumentThrowsException() {
        String top = getInput(USER_ID, AvailableCommands.TOP.getName(), "1", "3");
        assertThrows(InvalidCommandException.class, () -> executor.execute(top),
                "No more than one argument should be provided for top!");
    }

    @Test
    public void testTopWorksCorrectly() throws IOException, SpotifyExceptions {
        String top = getInput(USER_ID, AvailableCommands.TOP.getName(), "1");
        String result = "[" + executor.getSongs().get("why i love you").toString() + "]";
        assertEquals(executor.execute(top), result,
                "Top method does not work correctly!");
    }

    @Test
    public void testCreatePlaylistWithoutArgumentsThrowsException() {
        String create = getInput(USER_ID, AvailableCommands.CREATE_PLAYLIST.getName());
        assertThrows(InvalidCommandException.class, () -> executor.execute(create),
                "Name of the playlist should be provided when creating a new one!");

    }

    @Test
    public void testCreatePlaylistWithPlaylistThatExistsThrowsException() {
        String create = getInput(USER_ID, AvailableCommands.CREATE_PLAYLIST.getName(), "testFile");
        assertThrows(PlaylistAlreadyExistsException.class, () -> executor.execute(create),
                "Creating playlist that already exists should throw exception!");
    }

    @Test
    public void testCreatePlaylistWorksCorrectly() throws IOException, SpotifyExceptions {
        String create = getInput(USER_ID, AvailableCommands.CREATE_PLAYLIST.getName(), "newTestFile");
        executor.execute(create);

        String[] listOfFiles = new File(PLAYLISTS).list();
        assertNotNull(listOfFiles);
        assertTrue(Arrays.stream(listOfFiles).toList().contains("2 newTestFile.txt"),
                "Playlist should be in the playlists directory after creating it!");
        assertTrue(executor.getPlaylist().contains("2 newTestFile.txt"),
                "Creating playlist does not work correctly!");
    }

    @Test
    public void testShowPlaylistWithInvalidArgumentsThrowsException() {
        String show = getInput(USER_ID, AvailableCommands.SHOW_PLAYLIST.getName());
        assertThrows(InvalidCommandException.class, () -> executor.execute(show),
                "Show-playlist without arguments should throw exception!");
    }

    @Test
    public void testShowPlaylistWithNonExistingPlaylistThrowsException() {
        String show = getInput(USER_ID, AvailableCommands.SHOW_PLAYLIST.getName(), "test");
        assertThrows(NoSuchPlaylistException.class, () -> executor.execute(show),
                "Showing non-existing playlist should throw exception!");
    }

    @Test
    public void testShowPlaylistWorksCorrectly() throws IOException, SpotifyExceptions {
        String show = getInput(USER_ID, AvailableCommands.SHOW_PLAYLIST.getName(), "testFile");
        String result = "[" + executor.getSongs().get(SONG).toString() + "]";
        assertEquals(result, executor.execute(show),
                "Showing playlist does not work correctly!");
    }


    @Test
    public void testAddSongToInvalidArgumentsThrowsException() {
        String addCommand = getInput(USER_ID, AvailableCommands.ADD_SONG.getName(), TEST_FILE);
        assertThrows(InvalidCommandException.class, () -> executor.execute(addCommand),
                "Invalid arguments should throw exception!");
    }

    @Test
    public void testAddSongToInvalidPlaylistThrowsException() {
        String addCommand = getInput(USER_ID, AvailableCommands.ADD_SONG.getName(),
                "\"newTestFile\"", "\"why i love you\"");
        assertThrows(NoSuchPlaylistException.class, () -> executor.execute(addCommand),
                "Non existing playlist should throw exception!");
    }

    @Test
    public void testAddSongToInvalidSongThrowsException() {
        String addCommand = getInput(USER_ID, AvailableCommands.ADD_SONG.getName(),
                TEST_FILE, "\"why i love me\"");
        assertThrows(NoSuchSongException.class, () ->executor.execute(addCommand),
                "Non existing song should throw exception!");
    }

    @Test
    public void testAddSongWorksCorrectly() throws IOException, SpotifyExceptions {
        String addCommand = getInput(USER_ID, AvailableCommands.ADD_SONG.getName(),
                TEST_FILE, "\"song\"");
        executor.execute(addCommand);

        Path path = Paths.get(FILE_ONE);
        List<String> lines = Files.readAllLines(path);

        assertTrue(lines.stream().anyMatch(p -> p.contains("song")),
                "Adding song to playlist does not work correctly!");

    }

    @Test
    public void testPlaySongWithInvalidArgumentsThrowsException() {
        String playCommand = getInput(USER_ID, AvailableCommands.PLAY.getName());

        assertThrows(InvalidCommandException.class, () -> executor.execute(playCommand),
                "Play without arguments should throw exception!");
    }

    @Test
    public void testPlaySongWithUnknownSongThrowsException() {
        String playCommand = getInput(USER_ID, AvailableCommands.PLAY.getName(), "unknown");
        assertThrows(NoSuchSongException.class, () -> executor.execute(playCommand),
                "Playing unknown song should throw exception!");
    }

    @Test
    public void testPlaySongWorksCorrectly() throws IOException, SpotifyExceptions {
        String playCommand = getInput(USER_ID, AvailableCommands.PLAY.getName(), "why i love you");
        executor.execute(playCommand);
        assertEquals(executor.getSongs().get("why i love you").getCountPlays(), 2,
                "Play should increment the count streams of the song!");
    }

    @Test
    public void testStopWithInvalidArgumentsThrowsException() {
        String stop = getInput(USER_ID, AvailableCommands.STOP.getName(), SONG);
        assertThrows(InvalidCommandException.class, () -> executor.execute(stop),
                "No arguments should be provided!");
    }

    public static Reader initUsers() {
        String[] users = {"0 test@gmail.com test123",
                "1 maria@gmail.com mari01 ",
                "2 pesho@gmail.com peshko",
                "3 toshko@gmail.com toshiba"};

        return new StringReader(Arrays.stream(users).collect(Collectors.joining(System.lineSeparator())));
    }

    public static Reader initSongs() {
        String[] songs = {
                "song gerii 0",
                "numberCount geri 0",
                "Redbone Childish_Gambino 0",
                "Why_I_love_you Jay-Z_&_Kanye_West 1"
        };

        return new StringReader(Arrays.stream(songs).collect(Collectors.joining(System.lineSeparator())));
    }

    @AfterAll
    public static void deleteFiles() {
        File file1 = new File(FILE_ONE);
        file1.delete();

        File file2 = new File(FILE_TWO);
        file2.delete();
    }
}
