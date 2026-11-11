package com.garethevans.church.opensongtablet.drummer;

import android.content.Context;
import java.util.ArrayList;

public class Drummer {
    public Drummer(Context c) {}
    public boolean getIsRunning() { return false; }
    public void setSequencerMode(boolean s) {}
    public boolean getSequencerMode() { return false; }
    public String getDrummerStyle() { return "Acoustic"; }
    public void setDrummerStyle(String s) {}
    public String getDrummerStyleForSongXML(String s) { return s; }
    public String getDrummerStyleFromXML(String s) { return s; }
    public ArrayList<String> getDrummerFiles(String t, boolean n) { return new ArrayList<>(); }
    public String getFilenameFromBasics(String n, String t) { return ""; }
    public String getNiceNameFromFilename(String f) { return f; }
    public String getFilenameFromNiceName(String n) { return n; }
    public void loadDrummerFile(String f) {}
    public void saveDrummerFile(String f) {}
    public void updateActiveMap() {}
    public void setPattern(Object p) {}
    public void onStep(int s) {}
    public String getCajonPrefixIfNeeded() { return ""; }
}
