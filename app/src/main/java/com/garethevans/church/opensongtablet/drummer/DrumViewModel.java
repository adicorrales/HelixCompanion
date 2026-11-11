package com.garethevans.church.opensongtablet.drummer;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.garethevans.church.opensongtablet.interfaces.MainActivityInterface;
import com.garethevans.church.opensongtablet.midi.MidiClock;
import com.garethevans.church.opensongtablet.songprocessing.Song;

public class DrumViewModel extends ViewModel {
    private final String TAG = "DrumViewModel";

    private MainActivityInterface mainActivityInterface;

    // Core Engine Components
    private TimerEngine timerEngine;
    private DrumSoundManager drumSoundManager;
    private Drummer drummer;
    private MidiClock midiClock;

    // State Observables for UI
    private final MutableLiveData<Integer> currentStep = new MutableLiveData<>(-1);
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>(false);
    private final MutableLiveData<DrumSection> activeSection = new MutableLiveData<>(DrumSection.MAIN);
    private final MutableLiveData<DrumPatternJson> currentPattern = new MutableLiveData<>();

    // Song values (current song, next song and temporary song (for saving, etc)
    private int thisBpm = 120;
    private int thisBeats = -1;
    private int thisDivisions = -1;
    private int thisPulsesPerStep = 4;
    private int thisStepsPerBar;
    private int thisStepsPerPulse = 4;
    private long thisBeatDuration = 100;
    private final MutableLiveData<Boolean> drummerRunning = new MutableLiveData<>(false);
    private DrumPatternJson drumPatternJson;

    private boolean drummerIntro = true;

    public void initialiseDrums(Context c) {
        mainActivityInterface = (MainActivityInterface) c;

        // Initialise the classes
        this.drummer = new Drummer(c);
        this.timerEngine = new TimerEngine(c);
        this.drumSoundManager = new DrumSoundManager(c);
        this.midiClock = new MidiClock(c);

        drummerIntro = mainActivityInterface.getPreferences().getMyPreferenceBoolean("drummerIntro",true);
        timerEngine.setOnStepListener(totalSteps -> {
            // Check if the song has actually been loaded/initialized
            if (thisStepsPerBar <= 0) {
                return;
            }

            // Pass the RAW totalSteps to the drummer so it can sync
            if (drummer != null && drummer.getIsRunning()) {
                drummer.onStep(totalSteps);
            }

            // Calculate the wrapped step
            int stepInBar = totalSteps % thisStepsPerBar;

            // 1. Prevent divide by zero and handle BPM safely
            int safeBpm = Math.max(thisBpm, 1);

            // 2. Get the beat duration
            thisBeatDuration = DrumCalculations.getBeatDurationMs(safeBpm, thisDivisions);

            // Update UI for sequencer
            currentStep.postValue(stepInBar);
        });
    }

    public void setDrummerIntro(boolean drummerIntro) {
        this.drummerIntro = drummerIntro;
        mainActivityInterface.getPreferences().setMyPreferenceBoolean("drummerIntro",drummerIntro);
    }
    public boolean getDrummerIntro() {
        return drummerIntro;
    }

    // Get the helper classes
    public DrumSoundManager getDrumSoundManager() {
        return drumSoundManager;
    }
    public Drummer getDrummer() {
        return drummer;
    }
    public MidiClock getMidiClock() {
        return midiClock;
    }

    // The current song values
    /**
     @param song the song to be processed for tempo and time signature
     */
    public void prepareSongValues(Song song) {
        // Only proceed if we aren't editing the sequence
        boolean loadedDrummer = false;
        if (!drummer.getSequencerMode()) {
            if (song != null) {
                if (song.getDrummer()!=null && !song.getDrummer().isEmpty() && !song.getDrummer().equals(".json")) {
                    // Check the file exists
                    Uri drummerUri = mainActivityInterface.getStorageAccess().getUriForItem("Drummer", "", song.getDrummer());
                    if (mainActivityInterface.getStorageAccess().uriExists(drummerUri)) {
                        drummer.loadDrummerFile(song.getDrummer());
                        loadedDrummer = true;
                    }
                }

                if (song.getDrummerKit()==null || song.getDrummerKit().isEmpty()) {
                    drummer.setDrummerStyle("Acoustic");
                } else if (song.getDrummerKit().equals("Acoustic") || song.getDrummerKit().equals("Standard")) {
                    drummer.setDrummerStyle("Acoustic");
                } else if (song.getDrummerKit().equals("Percussion") || song.getDrummerKit().equals("Cajon")) {
                    drummer.setDrummerStyle("Percussion");
                } else {
                    drummer.setDrummerStyle("Acoustic");
                }

                if (loadedDrummer) {
                    thisBeats = getDrumPatternJson().getBeats();
                    thisDivisions = getDrumPatternJson().getDivisions();
                    setCurrentPattern(drumPatternJson);
                } else {
                    int[] sig = DrumCalculations.getFixedTimeSignature(song.getTimesig());
                    thisBeats = sig[0];
                    thisDivisions = sig[1];
                }
                thisBpm = DrumCalculations.getFixedTempo(song.getTempo(), true);
            }
        }

        // 1. Update the interval for the audio click
        thisBeatDuration = DrumCalculations.getBeatDurationMs(thisBpm, thisDivisions);

        // 4. Bar Math
        thisPulsesPerStep = DrumCalculations.getPulsesPerStep(thisDivisions);
        thisStepsPerPulse = DrumCalculations.getStepsPerPulse(thisDivisions);
        thisStepsPerBar = DrumCalculations.getTotalStepsInBar(thisBeats, thisDivisions);

        if (!drummer.getSequencerMode() && !loadedDrummer) {
            // Build an empty drum pattern
            buildEmptyPattern();

            // Now add in the default pattern
            addDefaultPattern();
        }

        // Update the drummer class and timer class to use this as the basis
        updateDrummerAndTimer();
    }

    public void buildEmptyPattern() {
        // FORCE REBUILD: Reset the pattern object so it's fresh for the new song
        drumPatternJson = new DrumPatternJson(
                drumSoundManager.getKit().getDrumParts(),
                this.thisStepsPerBar, thisBeats, thisDivisions
        );
    }
    public void addDefaultPattern() {
        // Repopulate with the default patterns for the new time signature
        DrumPatternBuilder.buildStandardPattern(
                drumPatternJson,
                this.thisBeats,
                this.thisDivisions,
                this.thisStepsPerPulse
        );
    }
    public void updateDrummerAndTimer() {
        // Update the drummer with the new pattern
        if (drummer != null) {
            drummer.setPattern(drumPatternJson);
            setCurrentPattern(drumPatternJson);
        }

        // Update the timer
        if (timerEngine != null) {
            // This now updates both BPM and the 6/8 vs 4/4 resolution in one go
            timerEngine.refresh(thisBpm, thisPulsesPerStep);
            timerEngine.resetTickCounter();
        }
    }
    public void setThisBpm(int thisBpm) {
        this.thisBpm = thisBpm;
        if (timerEngine != null) {
            timerEngine.setBpm(thisBpm);
        }
    }
    public int getThisBpm() {
        return thisBpm;
    }
    public void setThisBeats(int thisBeats) {
        this.thisBeats = thisBeats;
    }
    public int getThisBeats() {
        return thisBeats;
    }
    public void setThisDivisions(int thisDivisions) {
        this.thisDivisions = thisDivisions;
    }
    public int getThisDivisions() {
        return thisDivisions;
    }
    public int getThisStepsPerBar() {
        return thisStepsPerBar;
    }
    public int getThisStepsPerPulse() {
        return thisStepsPerPulse;
    }

    // MIDI clock control
    public void startMidiClock() {
        prepareSongValues(mainActivityInterface.getSong());

        if (midiClock != null) {
            midiClock.setIsRunning(true);
        }

        timerEngine.setBpm(thisBpm);
        timerEngine.setPulsesPerStep(thisPulsesPerStep);

        timerEngine.resetTickCounter();
        timerEngine.start();
        isPlaying.postValue(true);
    }

    public void stopMidiClock() {
        if (midiClock != null) {
            midiClock.setIsRunning(false);
        }
        stopTimerEngine();

        isPlaying.postValue(false);
        currentStep.postValue(-1);
    }

    // Drummer control
    public void toggleDrummer() {
        if (drummer.getIsRunning()) {
            stopDrummer();
        } else {
            startDrummer();
        }
    }

    public void startDrummer() {
        prepareSongValues(mainActivityInterface.getSong());

        if (drummer != null) {
            drummer.setIsRunning(true);
            drummerRunning.postValue(true);
        }

        timerEngine.setBpm(thisBpm);
        timerEngine.setPulsesPerStep(thisPulsesPerStep);

        timerEngine.resetTickCounter();
        timerEngine.start();
        isPlaying.postValue(true);
    }

    public void stopDrummer() {
        if (drummer != null) {
            drummer.setIsRunning(false);
            drummerRunning.postValue(false);
        }
        stopTimerEngine();

        isPlaying.postValue(false);
        currentStep.postValue(-1);
    }

    public LiveData<Boolean> getIsDrummerRunning() {
        return drummerRunning;
    }
    public LiveData<Boolean> getIsDrummerPlaying() {
        return isPlaying;
    }
    public LiveData<Integer> getCurrentStep() {
        return currentStep;
    }
    public LiveData<DrumSection> getActiveSection() {
        return activeSection;
    }
    public MutableLiveData<DrumPatternJson> getCurrentPattern() {
        return currentPattern;
    }

    public void setCurrentPattern(DrumPatternJson pattern) {
        this.drumPatternJson = pattern;
        this.currentPattern.postValue(pattern);
    }

    public DrumPatternJson getDrumPatternJson() {
        return drumPatternJson;
    }

    public void stopTimerEngine() {
        boolean drummerActive = (drummer != null && drummer.getIsRunning());
        boolean midiClockActive = (midiClock != null && midiClock.getIsRunning());

        if (!drummerActive && !midiClockActive) {
            timerEngine.stop();
            isPlaying.postValue(false);
        }
    }

    public void stopAll() {
        if (mainActivityInterface.getToolbar() != null) {
            mainActivityInterface.getToolbar().makeAllBeatsTransparent();
        }
        isPlaying.postValue(false);

        if (drummer != null) drummer.setIsRunning(false);
        if (midiClock != null) midiClock.setIsRunning(false);

        timerEngine.stop();
    }

    public void drummerFill() {
        if (drummer != null) {
            drummer.fill(0);
        }
    }

    public void drummerTransition() {
        if (drummer != null) {
            drummer.transition();
        }
    }

    public static class MetronomeStub {
        private boolean isRunning = false;
        private boolean metronomeAutoStart = false;
        private boolean metronomeMidi = false;
        private int metronomeLength = 4;
        private int tickColor = 0;
        private int tockColor = 0;
        public boolean getIsRunning() { return isRunning; }
        public void setIsRunning(boolean running) { this.isRunning = running; }
        public boolean getMetronomeAutoStart() { return metronomeAutoStart; }
        public boolean getMetronomeUseDefaults() { return true; }
        public boolean getMetronomeMidi() { return metronomeMidi; }
        public void setMetronomeMidi(boolean m) { this.metronomeMidi = m; }
        public int getMetronomeLength() { return metronomeLength; }
        public void setMetronomeLength(int l) { this.metronomeLength = l; }
        public int getTickColor() { return tickColor; }
        public int getTockColor() { return tockColor; }
        public String getMetronomeTickSound() { return "tick"; }
        public String getMetronomeTockSound() { return "tock"; }
        public int getMetronomePan() { return 0; }
        public void setVisualListener(Object l) {}
        public void setMetronomeFragment(Object f) {}
        public Object getMetronomeFragment() { return null; }
        public void resetTotalStepsProcessed() {}
        public void prepare(int d, int b) {}
        public void updateStartStopButton() {}
        public void onStep(int s, int b, long d) {}
        public void checkTickTockColors() {}
    }

    private final MetronomeStub metronomeStub = new MetronomeStub();
    public MetronomeStub getMetronome() { return metronomeStub; }
    public void startMetronome() {}
    public void stopMetronome() {}
    public void startTimerEngine() { if (timerEngine != null) { timerEngine.start(); } }
    public void setDrumPatternJson(DrumPatternJson pattern) { setCurrentPattern(pattern); }
    public void updateAllTimingValues(int beats, int divisions, int bpm) { this.thisBeats = beats; this.thisDivisions = divisions; this.thisBpm = bpm; }
    private boolean tempDrumsReceived = false;
    public boolean getTempDrumsReceived() { return tempDrumsReceived; }
    public void setTempDrumsReceived(boolean val) { this.tempDrumsReceived = val; }
    private int stepCount = 0;
    public void updateStepCount(int step) { this.stepCount = step; }
    public void updateActiveSection(DrumSection section) { activeSection.postValue(section); }
    private String tempDrumFileName = "";
    public String getTempDrumFileName() { return tempDrumFileName; }
    public void setTempDrumFileName(String name) { this.tempDrumFileName = name; }
    private String tempDrumAuthor = "";
    public String getTempDrumAuthor() { return tempDrumAuthor; }
    public void setTempDrumAuthor(String author) { this.tempDrumAuthor = author; }
    private String tempDrumTimeSig = "4/4";
    public void setTempDrumTimeSig(String ts) { this.tempDrumTimeSig = ts; }
    private int tempDrumTempo = 120;
    public void setTempDrumTempo(int tempo) { this.tempDrumTempo = tempo; }
    private String tempDrumKit = "Acoustic";
    public String getTempDrumKit() { return tempDrumKit; }
    public void setTempDrumKit(String kit) { this.tempDrumKit = kit; }
    public void startDrummerOnFill() { startDrummer(); }
    public void startDrummerOnVariation() { startDrummer(); }
}
