package adco.helixsongview.interfaces;

import androidx.fragment.app.FragmentManager;

import adco.helixsongview.autoscroll.Autoscroll;
import adco.helixsongview.performance.PerformanceGestures;
import adco.helixsongview.preferences.Preferences;
import adco.helixsongview.setprocessing.CurrentSet;
import adco.helixsongview.songprocessing.Song;

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
