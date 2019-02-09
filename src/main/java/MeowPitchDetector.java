import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;

public class MeowPitchDetector implements PitchDetectionHandler {
    private AudioDispatcher dispatcher;
    private float lastMeowTimestamp = Float.MIN_VALUE;
    private static final double SILENCE_THRESHOLD = 0.5;
    private int meowCount = 0;
    private MeowdioEvent currentMeowdioEvent;
    private boolean meowing;

    MeowPitchDetector(AudioDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void handlePitch(PitchDetectionResult pitchDetectionResult, AudioEvent audioEvent) {
        if (pitchDetectionResult.isPitched()) {
            float currentTimestamp = dispatcher.secondsProcessed();
            if ((currentTimestamp - lastMeowTimestamp) > SILENCE_THRESHOLD) {
                startMeow(currentTimestamp);
            }

            System.out.println(meowCount + ": " + pitchDetectionResult.getPitch());
        } else {
            stopMeow();
        }
    }

    private void stopMeow() {
        if (currentMeowdioEvent == null) {
            return;
        }

        currentMeowdioEvent.setFinishing();
        meowing = false;
    }

    private void startMeow(float currentTimestamp) {
        meowing = true;
        meowCount += 1;
        lastMeowTimestamp = currentTimestamp;
        currentMeowdioEvent = new MeowdioEvent(currentTimestamp, meowCount);
    }

    boolean isMeowing() {
        return meowing;
    }

    public MeowdioEvent getMeowdioEvent() {
        return currentMeowdioEvent;
    }
}
