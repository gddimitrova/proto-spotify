package bg.sofia.uni.fmi.mjt.spotify.command;

public enum AvailableCommands {
    HELP("help"),
    DISCONNECT("disconnect"),
    REGISTER("register"),
    LOGIN("login"),
    SEARCH("search"),
    TOP("top"),
    CREATE_PLAYLIST("create-playlist"),
    ADD_SONG("add-song-to"),
    SHOW_PLAYLIST("show-playlist"),
    PLAY("play"),
    STOP("stop");


    private final String name;
    AvailableCommands(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
