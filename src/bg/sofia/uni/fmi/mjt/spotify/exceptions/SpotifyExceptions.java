package bg.sofia.uni.fmi.mjt.spotify.exceptions;

public class SpotifyExceptions extends Exception {
    public SpotifyExceptions(String message) {
        super(message);
    }

    public SpotifyExceptions(String message, Throwable cause) {
        super(message, cause);
    }
}
