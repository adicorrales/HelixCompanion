package com.garethevans.church.opensongtablet.drummer;

import android.content.Context;
import com.garethevans.church.opensongtablet.songprocessing.Song;

public class DrumViewModel {

    public void drummerFill() {}
    public void drummerTransition() {}

    public void startMidiClock() {}
    public void prepareSongValues(Song s) {}


    public DrumSoundManager getDrumSoundManager() { return new DrumSoundManager(); }

    public Drummer getDrummer() { return new Drummer(); }
    public MidiClock getMidiClock() { return new MidiClock(); }
}
