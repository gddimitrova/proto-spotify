package bg.sofia.uni.fmi.mjt.spotify.song;

public class Song {
    private static final String SPACE = " ";
    private static final String LOWER_LINE = "_";
    private static final int NAME = 0;
    private static final int AUTHOR = 1;
    private static final int COUNT = 2;
    private final String name;
    private final String author;
    private int countPlays;
    private final String filename;

    private Song(String name, String author, int count, String fileName) {
        this.name = name;
        this.author = author;
        this.countPlays = count;
        this.filename = fileName;
    }
    public static Song of(String line) {
        String[] tokens = line.split(SPACE);

        String filename = tokens[NAME] + SPACE + tokens[AUTHOR];
        String name = tokens[NAME].replace(LOWER_LINE, SPACE);
        String author = tokens[AUTHOR].replace(LOWER_LINE, SPACE);
        int count = Integer.parseInt(tokens[COUNT]);

        return new Song(name, author, count, filename);
    }

    public void incrementCount() {
        this.countPlays++;
    }
    @Override
    public String toString() {
        return "Song: \"" + name + '\"' +
                " by \"" + author + '\"';
    }

    public String getName() {
        return this.name;
    }

    public String getFilename() {
        return this.filename;
    }

    public int getCountPlays() {
        return this.countPlays;
    }

    public String getAuthor() {
        return this.author;
    }
}
