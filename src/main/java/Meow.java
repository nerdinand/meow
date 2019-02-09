import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;
import be.tarsos.dsp.writer.WriterProcessor;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Meow implements PitchDetectionHandler {
    private static final double SILENCE_THRESHOLD = 0.5;
    private AudioDispatcher dispatcher;
    private float lastMeowTimestamp = Float.MIN_VALUE;
    private int meowCount = 0;
    private AudioFormat audioFormat;

    public static void main(String[] args) {
        Meow meow = new Meow();
        meow.go();
    }

    private void go() {
        AudioInputStream audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(new File("/Users/ferdi/Desktop/brienne-meow-mono.wav"));
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        JVMAudioInputStream jvmAudioInputStream = new JVMAudioInputStream(audioInputStream);
        float sampleRate = 44100;
        audioFormat = new AudioFormat(sampleRate, 16, 1, true, false);

        int audioBufferSize = 512;
        dispatcher = new AudioDispatcher(jvmAudioInputStream, audioBufferSize, 0);
        try {
            dispatcher.addAudioProcessor(new AudioPlayer(audioFormat));
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

        PitchProcessor.PitchEstimationAlgorithm algorithm = PitchProcessor.PitchEstimationAlgorithm.YIN;
        dispatcher.addAudioProcessor(new PitchProcessor(algorithm, sampleRate, audioBufferSize, this));
        dispatcher.addAudioProcessor(new BufferProcessor());
        dispatcher.run();
    }

    @Override
    public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
        boolean meowing;
        if (pitchDetectionResult.isPitched()) {
            float currentTimestamp = dispatcher.secondsProcessed();
            if ((currentTimestamp - lastMeowTimestamp) > SILENCE_THRESHOLD) {
                meowCount += 1;
                meowing = true;
            }
            System.out.println(meowCount + ": " + pitchDetectionResult.getPitch());
            lastMeowTimestamp = currentTimestamp;
        } else {
            meowing = false;
        }
    }

    private class BufferProcessor implements AudioProcessor {
        private final LimitedQueue<AudioEvent> limitedQueue;
        private final TarsosDSPAudioFormat tarsosDSPFormat;
        private int lastMeowCount = 1;
        private WriterProcessor writerProcessor;

        BufferProcessor() {
            limitedQueue = new LimitedQueue<>(50);
            tarsosDSPFormat = JVMAudioInputStream.toTarsosDSPFormat(audioFormat);
        }

        @Override
        public boolean process(AudioEvent audioEvent) {
            AudioEvent audioEventClone = new AudioEvent(tarsosDSPFormat);
            audioEventClone.setFloatBuffer(audioEvent.getFloatBuffer().clone());
            limitedQueue.offer(audioEventClone);

            try {
                if (lastMeowCount != meowCount) {
                    writerProcessor = new WriterProcessor(
                            tarsosDSPFormat, new RandomAccessFile("meow" + meowCount + ".wav", "rw")
                    );

                    for (AudioEvent a : limitedQueue) {
                        writerProcessor.process(a);
                    }
                    writerProcessor.processingFinished();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            lastMeowCount = meowCount;
            return false;
        }

        @Override
        public void processingFinished() {
        }
    }
}
