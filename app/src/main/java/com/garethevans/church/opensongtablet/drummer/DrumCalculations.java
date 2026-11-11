package com.garethevans.church.opensongtablet.drummer;

import com.garethevans.church.opensongtablet.songprocessing.Song;

public class DrumCalculations {
    public static int getStepsPerBar(int beats, int divisions) { return beats * divisions; }
    public static String getFixedTempoString(String tempo, boolean b) { return tempo != null ? tempo : ""; }
    public static String getFixedTimeSignatureString(String timesig, boolean b) { return timesig != null ? timesig : ""; }
    public static boolean isTempoTimeSigValid(Song s, boolean b) { return true; }
}
