import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchProcessor;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class Meow {
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;
    private static final float SAMPLE_RATE = 44100;
    private static final int AUDIO_BUFFER_SIZE = 512;
    public static final int BUFFER_OVERLAP = 0;

    private AudioDispatcher dispatcher;
    private AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);

    public static void main(String[] args) {
        Meow meow = new Meow();
        meow.go();
    }

    public void go() {
        TarsosDSPAudioFormat tarsosDSPAudioFormat = JVMAudioInputStream.toTarsosDSPFormat(audioFormat);
        MeowdioEvent.tarsosDSPAudioFormat = tarsosDSPAudioFormat;

        AudioInputStream audioInputStream;
        JVMAudioInputStream jvmAudioInputStream = null;

        try {
            audioInputStream = AudioSystem.getAudioInputStream(new File("/Users/ferdi/Desktop/brienne-meow-mono.wav"));
            jvmAudioInputStream = new JVMAudioInputStream(audioInputStream);
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }

        dispatcher = new AudioDispatcher(jvmAudioInputStream, AUDIO_BUFFER_SIZE, BUFFER_OVERLAP);
        try {
            dispatcher.addAudioProcessor(new AudioPlayer(audioFormat));
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

        MeowPitchDetector meowPitchDetector = new MeowPitchDetector(dispatcher);
        RingBufferProcessor ringBufferProcessor = new RingBufferProcessor(tarsosDSPAudioFormat);

        dispatcher.addAudioProcessor(new PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.YIN, SAMPLE_RATE, AUDIO_BUFFER_SIZE, meowPitchDetector));
        dispatcher.addAudioProcessor(ringBufferProcessor);
        dispatcher.addAudioProcessor(new MeowdioWriter(meowPitchDetector, ringBufferProcessor));
        dispatcher.run();
    }

}
