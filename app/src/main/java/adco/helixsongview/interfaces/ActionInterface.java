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

    void onBackPressed();

    FragmentManager getMyFragmentManager();
    CurrentSet getCurrentSet();
    Song getSong();
    Autoscroll getAutoscroll();
    PerformanceGestures getPerformanceGestures();
}
