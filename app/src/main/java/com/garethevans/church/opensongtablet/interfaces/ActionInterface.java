package com.garethevans.church.opensongtablet.interfaces;

import androidx.fragment.app.FragmentManager;

import com.garethevans.church.opensongtablet.autoscroll.Autoscroll;
import com.garethevans.church.opensongtablet.performance.PerformanceGestures;
import com.garethevans.church.opensongtablet.preferences.Preferences;
import com.garethevans.church.opensongtablet.setprocessing.CurrentSet;
import com.garethevans.church.opensongtablet.songprocessing.Song;

public interface ActionInterface {
    Preferences getPreferences();
    void navigateToFragment(String deepLink, int id);
    default void showSticky(boolean forceShow, boolean hide) {}
    void showAbc(boolean forceShow, boolean hide);
    default void toggleHighlighter() {}
    void onBackPressed();
    default void drummerToggle() {}
    FragmentManager getMyFragmentManager();
    CurrentSet getCurrentSet();
    Song getSong();
    Autoscroll getAutoscroll();
    PerformanceGestures getPerformanceGestures();
}
