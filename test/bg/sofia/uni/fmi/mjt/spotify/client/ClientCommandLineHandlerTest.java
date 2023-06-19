package bg.sofia.uni.fmi.mjt.spotify.client;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.DataLine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClientCommandLineHandlerTest {
    private static final float RATE = 48000.0f;
    private static final double DELTA = 0.1;
    private static final int SIZE_BITS = 16;
    private static final int CURR_FRAME = 4;
    private static final int CHANNEL = 2;

    @Test
    public void testCreateDataLineInfoFromString() {
        String result = "PCM_SIGNED 48000.0 16 2 4 48000.0 false";
        String message = "Creating DataLineInfo does not work correctly!";

        ClientCommandLineHandler clientCommandLineHandler = new ClientCommandLineHandler();
        DataLine.Info info = clientCommandLineHandler.createDataLine(result);

        AudioFormat audioFormat = info.getFormats()[0];

        assertEquals("PCM_SIGNED", audioFormat.getEncoding().toString(), message);
        assertEquals(RATE, audioFormat.getSampleRate(), DELTA, message);
        assertEquals(SIZE_BITS, audioFormat.getSampleSizeInBits(), message);
        assertEquals(CHANNEL, audioFormat.getChannels(), message);
        assertEquals(CURR_FRAME, audioFormat.getFrameSize(), message);
        assertEquals(RATE, audioFormat.getFrameRate(), DELTA, message);
        assertFalse(audioFormat.isBigEndian(), message);
    }

}
