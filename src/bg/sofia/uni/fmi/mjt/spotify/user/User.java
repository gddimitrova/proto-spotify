package bg.sofia.uni.fmi.mjt.spotify.user;

public record User(int id, String email, String password) {
    private static final String REGEX = " ";
    private static final int ID = 0;
    private static final int EMAIL = 1;
    private static final int PASS = 2;

    public static User of(String line) {
        String[] tokens = line.split(REGEX);

        int id = Integer.parseInt(tokens[ID]);
        String email = tokens[EMAIL];
        String password = tokens[PASS];

        return new User(id, email, password);
    }
}
