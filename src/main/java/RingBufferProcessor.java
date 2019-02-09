import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;

import java.util.Arrays;

public class RingBufferProcessor implements AudioProcessor {
    private static final int QUEUE_LIMIT = 200;
    private final LimitedQueue<AudioEvent> limitedQueue;
    private final TarsosDSPAudioFormat tarsosDSPFormat;

    RingBufferProcessor(TarsosDSPAudioFormat tarsosDSPFormat) {
        limitedQueue = new LimitedQueue<>(QUEUE_LIMIT);
        this.tarsosDSPFormat = tarsosDSPFormat;
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        AudioEvent audioEventClone = cloneAudioEvent(audioEvent);
        limitedQueue.offer(audioEventClone);
        return true;
    }

    private AudioEvent cloneAudioEvent(AudioEvent audioEvent) {
        AudioEvent audioEventClone = new AudioEvent(tarsosDSPFormat);
        audioEventClone.setFloatBuffer(audioEvent.getFloatBuffer().clone());
        return audioEventClone;
    }

    @Override
    public void processingFinished() {

    }

    public AudioEvent[] getLast(int count) {
        AudioEvent[] audioEvents = limitedQueue.toArray(new AudioEvent[]{});
        return Arrays.copyOfRange(audioEvents, audioEvents.length - 2 - count, audioEvents.length - 2);
    }
}
