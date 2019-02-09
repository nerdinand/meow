import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.io.jvm.JVMAudioInputStream;
import be.tarsos.dsp.pitch.PitchProcessor;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

public class Meow {
    private static final int SAMPLE_SIZE_IN_BITS = 16;
    private static final int CHANNELS = 1;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;
    private static final float SAMPLE_RATE = 44100;
    private static final int AUDIO_BUFFER_SIZE = 1024;
    public static final int BUFFER_OVERLAP = 0;

    private AudioDispatcher dispatcher;
    private AudioFormat audioFormat = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_IN_BITS, CHANNELS, SIGNED, BIG_ENDIAN);

    public static void main(String[] args) {
        Meow meow = new Meow();
        meow.go();
    }

    private static Vector<Mixer.Info> getMixerInfo(final boolean supportsPlayback, final boolean supportsRecording) {
        final Vector<Mixer.Info> infos = new Vector<>();
        final Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (final Mixer.Info mixerinfo : mixers) {
            if (supportsRecording
                    && AudioSystem.getMixer(mixerinfo).getTargetLineInfo().length != 0) {
                // Mixer capable of recording audio if target LineWavelet length != 0
                infos.add(mixerinfo);
            } else if (supportsPlayback
                    && AudioSystem.getMixer(mixerinfo).getSourceLineInfo().length != 0) {
                // Mixer capable of audio play back if source LineWavelet length != 0
                infos.add(mixerinfo);
            }
        }
        return infos;
    }

    private static Mixer.Info getMixerInfoByName(String name) {
        for (Mixer.Info info : getMixerInfo(false, true)) {
            if (info.getName().equals(name)) {
                return info;
            }
        }
        return null;
    }

    JVMAudioInputStream getFileInputStream(String fileName) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(fileName));
            return new JVMAudioInputStream(audioInputStream);
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    JVMAudioInputStream getDeviceInputStream(String deviceName) {
        Mixer.Info mixerInfo = getMixerInfoByName(deviceName);

        Mixer mixer = AudioSystem.getMixer(mixerInfo);
        try {
            TargetDataLine targetDataLine = (TargetDataLine) mixer.getLine(new DataLine.Info(TargetDataLine.class, audioFormat));
            targetDataLine.open(audioFormat, AUDIO_BUFFER_SIZE);
            targetDataLine.start();
            final AudioInputStream stream = new AudioInputStream(targetDataLine);
            return new JVMAudioInputStream(stream);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }

        return null;
    }


    public void go() {
        TarsosDSPAudioFormat tarsosDSPAudioFormat = JVMAudioInputStream.toTarsosDSPFormat(audioFormat);
        MeowdioEvent.tarsosDSPAudioFormat = tarsosDSPAudioFormat;

        JVMAudioInputStream jvmAudioInputStream;
        jvmAudioInputStream = getDeviceInputStream("Samson Meteor Mic");
//        jvmAudioInputStream = getFileInputStream("/Users/ferdi/Desktop/brienne-meow-mono.wav");

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
