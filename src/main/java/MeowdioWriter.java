import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

import javax.sound.sampled.AudioFormat;
import java.util.ArrayList;

public class MeowdioWriter implements AudioProcessor {
    private final MeowPitchDetector meowPitchDetector;
    private final RingBufferProcessor ringBufferProcessor;
    private final ArrayList<MeowdioEvent> meowdioEventsToBeFinished = new ArrayList<>();

    MeowdioWriter(MeowPitchDetector meowPitchDetector, RingBufferProcessor ringBufferProcessor) {
        this.meowPitchDetector = meowPitchDetector;
        this.ringBufferProcessor = ringBufferProcessor;
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        MeowdioEvent currentMeowdioEvent = meowPitchDetector.getMeowdioEvent();

        if (currentMeowdioEvent != null) {
            if (currentMeowdioEvent.isNew()) {
                for (AudioEvent event : ringBufferProcessor.getLast(currentMeowdioEvent.getEventsBefore())) {
                    currentMeowdioEvent.process(event);
                }
            }

            if (meowPitchDetector.isMeowing()) {
                currentMeowdioEvent.process(audioEvent);
            } else {
                if (currentMeowdioEvent.isFinishing() && !currentMeowdioEvent.isFinished()) {
                    if (!meowdioEventsToBeFinished.contains(currentMeowdioEvent)) {
                        meowdioEventsToBeFinished.add(currentMeowdioEvent);
                    }
                }
            }
        }

        for (MeowdioEvent meowdioEvent : meowdioEventsToBeFinished) {
            meowdioEvent.processFinishing(audioEvent);

            if (meowdioEvent.isFinished()) {
                meowdioEvent.processingFinished();
            }
        }

        meowdioEventsToBeFinished.removeIf(MeowdioEvent::isFinished);

        return true;
    }


    @Override
    public void processingFinished() {
        for (MeowdioEvent meowdioEvent : meowdioEventsToBeFinished) {
            meowdioEvent.processingFinished();
        }
    }
}
