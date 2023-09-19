package bg.sofia.uni.fmi.mjt.spotify.memory;

import bg.sofia.uni.fmi.mjt.spotify.song.Song;
import bg.sofia.uni.fmi.mjt.spotify.user.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoryStorage {
    private static final int ZERO = 0;
    private static final int ONE = 1;
    
    private final Reader userReader;
    private final Reader songReader;

    private final Map<Integer, User> users;
    private final Map<String, Integer> emails;
    private final Map<String, Song> songs;
    private final List<String> playlist;
    private static int currId = 0;

    public MemoryStorage(Reader userReader, Reader songReader) {
        this.userReader = userReader;
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

    public List<String> getPlaylist() {
        return playlist;
    }

    public Map<String, Song> getSongs() {
        return songs;
    }

    public Map<String, Integer> getEmails() {
        return emails;
    }

    public Map<Integer, User> getUsers() {
        return users;
    }

    public static int getCurrId() {
        return currId;
    }
}
