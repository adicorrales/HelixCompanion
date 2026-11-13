package com.garethevans.church.opensongtablet.drummer;

import android.content.Context;
import com.garethevans.church.opensongtablet.songprocessing.Song;

public class DrumViewModel {
    public void initialiseDrums(Context c) {}
    public void toggleDrummer() {}
    public void stopDrummer() {}
    public void drummerFill() {}
    public void drummerTransition() {}
    public void startMetronome() {}
    public void stopMetronome() {}
    public void stopAll() {}
    public void startMidiClock() {}
    public void prepareSongValues(Song s) {}
    public void startDrummer() {}

    public DrumSoundManager getDrumSoundManager() { return new DrumSoundManager(); }
    public Metronome getMetronome() { return new Metronome(); }
    public Drummer getDrummer() { return new Drummer(); }
    public MidiClock getMidiClock() { return new MidiClock(); }
}
