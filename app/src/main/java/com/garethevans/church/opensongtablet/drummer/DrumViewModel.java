package com.garethevans.church.opensongtablet.drummer;

import android.content.Context;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.garethevans.church.opensongtablet.songprocessing.Song;

public class DrumViewModel extends ViewModel {
    private final Drummer drummer = new Drummer(null);
    private final DrumSoundManager soundManager = new DrumSoundManager();
    private final Metronome metronome = new Metronome();
    private final MidiClock midiClock = new MidiClock();
    private final MutableLiveData<Boolean> drummerRunning = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> currentStep = new MutableLiveData<>(0);
    private final MutableLiveData<Object> activeSection = new MutableLiveData<>(null);
    private final MutableLiveData<Object> currentPattern = new MutableLiveData<>(null);

    public void initialiseDrums(Context c) {}
    public Drummer getDrummer() { return drummer; }
    public DrumSoundManager getDrumSoundManager() { return soundManager; }
    public Metronome getMetronome() { return metronome; }
    public MidiClock getMidiClock() { return midiClock; }
    public boolean getDrummerIntro() { return true; }
    public void setDrummerIntro(boolean b) {}
    public void stopAll() {}
    public void stopDrummer() {}
    public void startDrummer() {}
    public void startDrummerOnFill() {}
    public void startDrummerOnVariation() {}
    public void toggleDrummer() {}
    public void startMetronome() {}
    public void stopMetronome() {}
    public void startMidiClock() {}
    public void stopMidiClock() {}
    public void startTimerEngine() {}
    public void stopTimerEngine() {}
    public void drummerFill() {}
    public void drummerTransition() {}
    public void updateStepCount(int s) {}
    public int getThisStepsPerBar() { return 4; }
    public int getThisStepsPerPulse() { return 1; }
    public int getThisBeats() { return 4; }
    public int getThisDivisions() { return 4; }
    public int getThisBpm() { return 120; }
    public MutableLiveData<Boolean> getDrummerRunning() { return drummerRunning; }
    public MutableLiveData<Integer> getCurrentStep() { return currentStep; }
    public MutableLiveData<Object> getActiveSection() { return activeSection; }
    public MutableLiveData<Object> getCurrentPattern() { return currentPattern; }
    public void updateActiveSection(Object s) {}
    public void prepareSongValues(Song s) {}
    public void updateDrummerAndTimer() {}
    public void updateAllTimingValues(int b, int d, int bpm) {}
    public Object getDrumPatternJson() { return null; }
    public void setDrumPatternJson(Object p) {}
    public void setCurrentPattern(Object p) {}
    public String getTempDrumFileName() { return ""; }
    public void setTempDrumFileName(String n) {}
    public String getTempDrumAuthor() { return ""; }
    public void setTempDrumAuthor(String a) {}
    public String getTempDrumKit() { return ""; }
    public void setTempDrumKit(String k) {}
    public boolean getTempDrumsReceived() { return false; }
    public void setTempDrumsReceived(boolean r) {}
}
