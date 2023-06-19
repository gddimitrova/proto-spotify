package bg.sofia.uni.fmi.mjt.spotify.command;

import java.util.Arrays;
import java.util.stream.Stream;

public record Command(int id, String command, String... arguments) {
    private static final String SPACE = " ";
    private static final String MULTI_NAME_REGEX = "\" \"";
    private static final String QUOTE = "\"";
    private static final int ID = 0;
    private static final int COMMAND = 1;

    private static final int TWO = 2;
    private static final int ONE = 1;

    private static String[] extractTokens(String line) {
        if (line.contains(QUOTE)) {
            int idxOfSecondSpace = line.indexOf(SPACE, line.indexOf(SPACE) + ONE);
            String commandSubstring = line.substring(0, idxOfSecondSpace);
            String argumentsSubstring = line.substring(idxOfSecondSpace + TWO, line.length() - ONE).strip();

            String[] command = commandSubstring.split(SPACE);
            String[] arguments = argumentsSubstring.split(MULTI_NAME_REGEX);

            return Stream.concat(Arrays.stream(command), Arrays.stream(arguments)).toArray(String[]::new);
        }

        return line.split(SPACE);
    }

    public static Command of(String line) {
        String[] tokens = Arrays.stream(extractTokens(line.strip())).filter(p -> !p.isBlank()).toArray(String[]::new);

        int id = Integer.parseInt(tokens[ID]);
        String command = tokens[COMMAND];

        if (tokens.length > TWO) {
            String[] arguments = new String[tokens.length - TWO];
            System.arraycopy(tokens, TWO, arguments, 0, tokens.length - TWO);
            return new Command(id, command, arguments);
        }

        return new Command(id, command);
    }
}
