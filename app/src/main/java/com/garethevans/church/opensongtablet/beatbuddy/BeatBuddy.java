package com.garethevans.church.opensongtablet.beatbuddy;

import android.content.Context;
import com.garethevans.church.opensongtablet.songprocessing.Song;

public class BeatBuddy {
    public BeatBuddy(Context c) {}
    public boolean getBeatBuddyUseImported() { return false; }
    public void setBeatBuddyUseImported(boolean b) {}
    public boolean getBeatBuddyAerosMode() { return false; }
    public void setBeatBuddyAerosMode(boolean b) {}
    public boolean getBeatBuddyIncludeSong() { return false; }
    public void setBeatBuddyIncludeSong(boolean b) {}
    public boolean getBeatBuddyIncludeVolume() { return false; }
    public void setBeatBuddyIncludeVolume(boolean b) {}
    public boolean getBeatBuddyAutoLookup() { return false; }
    public void setBeatBuddyAutoLookup(boolean b) {}
    public int getBeatBuddyDrumKit() { return 1; }
    public int getBeatBuddyChannel() { return 1; }
    public void setBeatBuddyChannel(int c) {}
    public void setPrefs() {}
    public int getCC_Transition() { return 0; }
    public int getCC_Exclusive_transition() { return 0; }
    public int getCC_Half_time() { return 0; }
    public int getCC_Double_time() { return 0; }
    public int getCC_HP_vol() { return 0; }
    public int getCC_Mix_vol() { return 0; }
    public int getCC_Intro() { return 0; }
    public int getCC_Outro() { return 0; }
    public int getCC_Pause_unpause() { return 0; }
    public int getCC_Drum_fill() { return 0; }
    public int getCC_Accent_hit() { return 0; }
    public int getBeatBuddyVolume() { return 100; }
    public String getTempoCode(int t) { return ""; }
    public String getSongCode(int f, int s) { return ""; }
    public int tryAutoSend(Context c, Object m, Song s) { return 0; }
    public Object getBbsqLite() { return null; }
    public void beatBuddyStart() {}
    public void beatBuddyStop() {}
    public void beatBuddyPause() {}
    public void beatBuddyAccent() {}
    public void beatBuddyFill() {}
    public void beatBuddyTransition1() {}
    public void beatBuddyTransition2() {}
    public void beatBuddyTransition3() {}
    public void beatBuddyTransitionNext() {}
    public void beatBuddyTransitionPrev() {}
    public void beatBuddyTransitionExit() {}
    public void beatBuddyExclusiveTransition1() {}
    public void beatBuddyExclusiveTransition2() {}
    public void beatBuddyExclusiveTransition3() {}
    public void beatBuddyExclusiveTransitionNext() {}
    public void beatBuddyExclusiveTransitionPrev() {}
    public void beatBuddyExclusiveTransitionExit() {}
    public void beatBuddyHalfTime() {}
    public void beatBuddyHalfTimeExit() {}
    public void beatBuddyDoubleTime() {}
    public void beatBuddyDoubleTimeExit() {}
    public void beatBuddyVolUp() {}
    public void beatBuddyVolDown() {}
    public void beatBuddyVolHPUp() {}
    public void beatBuddyVolHPDown() {}
}
