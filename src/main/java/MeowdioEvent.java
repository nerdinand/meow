import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.writer.WriterProcessor;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

class MeowdioEvent implements AudioProcessor {
    private static final int EVENTS_BEFORE = 50;
    private static final int EVENTS_AFTER = 50;
    static TarsosDSPAudioFormat tarsosDSPAudioFormat;
    private final int eventsBefore;
    private final int eventsAfter;
    private boolean isNew;
    private boolean finishing;

    private WriterProcessor writerProcessor;
    private float startSecondsElapsed;

    private int finishingAudioEventCount = 0;

    MeowdioEvent(float startSecondsElapsed, int meowCount) {
        this.startSecondsElapsed = startSecondsElapsed;
        this.isNew = true;
        this.finishing = false;
        try {
            this.writerProcessor = new WriterProcessor(
                    tarsosDSPAudioFormat, new RandomAccessFile("meow" + meowCount + ".wav", "rw")
            );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        this.eventsBefore = EVENTS_BEFORE;
        this.eventsAfter = EVENTS_AFTER;
    }

    void setFinishing() {
        this.finishing = true;
    }

    boolean isFinished() {
        return finishingAudioEventCount >= eventsAfter;
    }

    boolean isFinishing() {
        return finishing;
    }

    int getEventsBefore() {
        return eventsBefore;
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        isNew = false;
        writerProcessor.process(audioEvent);
        return true;
    }

    @Override
    public void processingFinished() {
        writerProcessor.processingFinished();
    }

    void processFinishing(AudioEvent audioEvent) {
        process(audioEvent);
        finishingAudioEventCount++;

        if (isFinished()) {
            finishing = false;
        }
    }

    boolean isNew() {
        return isNew;
    }
}
