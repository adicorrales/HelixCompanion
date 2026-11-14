package adco.helixsongview.interfaces;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;

import android.net.Uri;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import adco.helixsongview.abcnotation.ABCNotation;
import adco.helixsongview.animation.CustomAnimation;
import adco.helixsongview.animation.ShowCase;
import adco.helixsongview.appdata.AlertChecks;
import adco.helixsongview.appdata.CheckInternet;
import adco.helixsongview.appdata.FixLocale;
import adco.helixsongview.appdata.MyFonts;
import adco.helixsongview.appdata.TimeTools;
import adco.helixsongview.appdata.VersionNumber;
import adco.helixsongview.autoscroll.Autoscroll;

import adco.helixsongview.chords.ChordDirectory;
import adco.helixsongview.chords.ChordDisplayProcessing;
import adco.helixsongview.chords.Transpose;
import adco.helixsongview.controls.CommonControls;
import adco.helixsongview.controls.Gestures;
import adco.helixsongview.controls.HotZones;
import adco.helixsongview.controls.PageButtons;
import adco.helixsongview.controls.PedalActions;
import adco.helixsongview.controls.Swipes;
import adco.helixsongview.customslides.CustomSlide;
import adco.helixsongview.customviews.DrawNotes;
import adco.helixsongview.customviews.MyToolbar;
import adco.helixsongview.export.ExportActions;
import adco.helixsongview.export.OpenSongSetBundle;
import adco.helixsongview.export.PrepareFormats;
import adco.helixsongview.filemanagement.LoadSong;
import adco.helixsongview.filemanagement.SaveSong;
import adco.helixsongview.filemanagement.StorageAccess;
import adco.helixsongview.importsongs.ImportOnlineFragment;
import adco.helixsongview.importsongs.WebDownload;
import adco.helixsongview.justchords.ConvertJustChords;
import adco.helixsongview.midi.Midi;
import adco.helixsongview.midi.MidiFragment;
import adco.helixsongview.nearby.NearbyActions;
import adco.helixsongview.openchords.OpenChordsAPI;
import adco.helixsongview.pads.Pad;
import adco.helixsongview.pdf.MakePDF;

import adco.helixsongview.performance.DisplayPrevNext;
import adco.helixsongview.performance.PerformanceFragment;
import adco.helixsongview.performance.PerformanceGestures;
import adco.helixsongview.preferences.AppPermissions;
import adco.helixsongview.preferences.Preferences;
import adco.helixsongview.preferences.ProfileActions;
import adco.helixsongview.screensetup.BatteryStatus;
import adco.helixsongview.screensetup.Palette;
import adco.helixsongview.screensetup.ShowToast;
import adco.helixsongview.screensetup.ThemeColors;
import adco.helixsongview.screensetup.WindowFlags;
import adco.helixsongview.setmenu.SetMenuFragment;
import adco.helixsongview.setprocessing.CurrentSet;
import adco.helixsongview.setprocessing.SetActions;
import adco.helixsongview.songmenu.SongListBuildIndex;
import adco.helixsongview.songmenu.SongMenuFragment;
import adco.helixsongview.songprocessing.ConvertChoPro;
import adco.helixsongview.songprocessing.ConvertOnSong;
import adco.helixsongview.songprocessing.ConvertTextSong;
import adco.helixsongview.songprocessing.ConvertWord;
import adco.helixsongview.songprocessing.ProcessSong;
import adco.helixsongview.songprocessing.Song;
import adco.helixsongview.songprocessing.SongSheetHeaders;
import adco.helixsongview.sqlite.AnalyticsSQLiteHelper;
import adco.helixsongview.sqlite.CommonSQL;
import adco.helixsongview.sqlite.NonOpenSongSQLiteHelper;
import adco.helixsongview.sqlite.SQLiteHelper;
import adco.helixsongview.variations.Variations;
import adco.helixsongview.webserver.LocalWiFiHost;
import adco.helixsongview.webserver.WebServer;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ThreadPoolExecutor;


public interface MainActivityInterface {

    // Initialising the activity and settings
    boolean getWaitingOnBootUpFragment();
    void initialiseActivity();
    void initialiseStartVariables();
    void updateSizes(int width, int height);
    int[] getDisplayMetrics();
    float getDisplayDensity();
    Handler getMainHandler();
    ThreadPoolExecutor getThreadPoolExecutor();
    FixLocale getFixLocale();
    Locale getLocale();
    VersionNumber getVersionNumber();
    String getMode();
    void setMode(String whichMode);
    String modePerformanceString();

    void setFirstRun(boolean firstRun);
    boolean getFirstRun();
    int getOrientation();
    WebServer getWebServer();
    void openWebServerMessages();
    LocalWiFiHost getLocalWiFiHost();
    void recreateActivity();

    // Preferences and settings
    Preferences getPreferences();

    ProfileActions getProfileActions();
    MyFonts getMyFonts();
    ThemeColors getMyThemeColors();
    AppPermissions getAppPermissions();
    void prepareStrings();

    // Song stuff
    Song getSong();
    Song getIndexingSong();
    Song getTempSong();
    LoadSong getLoadSong();
    SaveSong getSaveSong();
    void setSong(Song song);
    void setTempSong(Song tempSong);
    String getMainfoldername();

    // Capo
    void dealWithCapo();

    // Pads
    Pad getPad();
    boolean playPad();

    // Autoscroll
    Autoscroll getAutoscroll();
    void toggleAutoscroll();

    // Set stuff
    CurrentSet getCurrentSet();
    SetActions getSetActions();
    void updateSetList();
    void checkSetMenuItemHighlighted(int setPosition);
    void toggleInlineSet();
    void notifyInlineSetInserted();
    void notifyInlineSetInserted(int position);
    void notifyInlineSetMove(int from, int to);
    void notifyInlineSetRemoved(int postion);
    void notifyInlineSetChanged(int position);
    void notifyInlineSetRangeChanged(int from, int count);
    void notifyToClearInlineSet(int from, int count);
    void notifyToInsertAllInlineSet();
    void updateInlineSetVisibility();
    void notifyInlineSetHighlight();
    void notifySetFragment(String what, int position);
    void notifyInlineSetScrollToItem();
    void notifyInlineSetCueItem();
    int getSongWidth();
    Variations getVariations();
    SetMenuFragment getSetMenuFragment();
    OpenSongSetBundle getOpenSongSetBundle();

    // Menus
    void lockDrawer(boolean lock);
    void closeDrawer(boolean close);
    void moveToSongInSongMenu();
    int getPositionOfSongInMenu();
    void updateSongList();
    void quickSongMenuBuild();
    void fullIndex(String specificFolder);
    void indexSongs();
    void updateSongMenu(String fragName, Fragment callingFragment, ArrayList<String> arguments);
    void updateSongMenu(Song song);
    void chooseMenu(boolean showSetMenu);
    void setIndexingSong(Song indexingSong);
    Song getSongInMenu(int position);
    boolean getShowSetMenu();
    void refreshMenuItems();
    ArrayList<Song> getSongsInMenu();
    ArrayList<Song> getSongsFound(String whichMenu);
    SongListBuildIndex getSongListBuildIndex();
    void scrollOpenMenu(boolean scrollDown);
    boolean getMenuOpen();
    boolean getSettingsOpen();
    void setSettingsOpen(boolean settingsOpen);
    void updateCheckForThisSong(Song thisSong);
    SongMenuFragment getSongMenuFragment();
    void setHighlightChangeAllowed(boolean highlightChangeAllowed);

    // OpenChords stuff
    OpenChordsAPI getOpenChordsAPI();


    void setRequireAudioRecorder();
    void displayAudioRecorder();
    void toggleChordDetection();

    // Action bar
    MyToolbar getToolbar();
    BatteryStatus getBatteryStatus();
    void hideActionBar();
    void showActionBar();
    ImageView disableActionBarStuff(boolean disable);
    void updateToolbar(String what);
    void updateToolbarHelp(String webAddress);
    void updateActionBarSettings(String prefName, float floatval, boolean isvisible);
    boolean needActionBar();
    void allowNavigationUp(boolean allow);

    // Page button(s)
    PageButtons getPageButtons();
    void hideActionButton(boolean hide);
    void updatePageButtonLayout();
    void miniPageButton(boolean mini);
    void initialisePageButtons();

    // Controls
    CommonControls getCommonControls();
    PedalActions getPedalActions();
    Gestures getGestures();
    PerformanceGestures getPerformanceGestures();
    Swipes getSwipes();
    void enableSwipe(String which, boolean canSwipe);
    HotZones getHotZones();
    boolean onKeyUp(int keyCode, KeyEvent keyEvent);
    boolean onKeyDown(int keyCode, KeyEvent keyEvent);

    // Navigation
    void navHome();
    void navigateToFragment(String deepLink, int id);
    void popTheBackStack(int id, boolean inclusive);
    void registerFragment(Fragment frag, String what);
    void updateFragment(String fragName, Fragment callingFragment, ArrayList<String> arguments);
    FragmentManager getMyFragmentManager();
    void dealWithIntent(int fragmentId);
    void setForceReload(boolean forceReload);
    boolean getForceReload();
    boolean getPerformanceValid();
    PerformanceFragment getPerformanceFragment();

    // Showcase
    ShowCase getShowCase();
    void showTutorial(String what,ArrayList<View> viewsToHighlight);

    // File work
    StorageAccess getStorageAccess();
    void doSongLoad(String folder, String filename, boolean closeDrawer);
    void refreshSong();
    void loadSongFromSet(int position);
    ExportActions getExportActions();
    Uri getImportUri();
    void setImportFilename(String importFilename);
    void setImportUri(Uri importUri);
    String getImportFilename();
    PrepareFormats getPrepareFormats();
    void openFragmentBasedOnFileImport();

    // Nearby connections
    NearbyActions getNearbyActions();


    // Midi
    Midi getMidi();
    MidiFragment getMidiFragment();
    void setMidiFragment(MidiFragment midiFragment);
    void registerMidiPedalAction(boolean actionDown, boolean actionUp, boolean actionLong, String note);
    void onTransportPrev();
    void onTransportRt0();
    void onTransportPlay();
    void onTransportNext();
    void sendHelixSetMidi(int setIdx);
    void sendHelixMidiSetAndSong(int setIdx, int songPosition);
    void resetLastSentMidiSetIdx();
    void toggleBeatBuddyControlPopUp();
    void forceCloseBeatBuddyControlPopUp();
    void updateBeatBuddyControlPopUp();

    // Database
    SQLiteHelper getSQLiteHelper();
    NonOpenSongSQLiteHelper getNonOpenSongSQLiteHelper();
    CommonSQL getCommonSQL();
    AnalyticsSQLiteHelper getAnalyticsHelper();
    void showRepairSongs();

    // Web activities
    WebDownload getWebDownload();
    CheckInternet getCheckInternet();
    void isWebConnected(Fragment fragment, int fragId, boolean isConnected);
    void songSelectDownload(Fragment fragment, int fragId, Uri uri, String filename);
    void openDocument(String location);
    void chordinatorResult(ImportOnlineFragment importOnlineFragment, String songText);

    // General tools
    void selectFile(Intent intent);
    void selectFolder(Intent intent);
    CustomAnimation getCustomAnimation();
    Transpose getTranspose();
    AlertChecks getAlertChecks();
    TimeTools getTimeTools();
    DisplayPrevNext getDisplayPrevNext();
    void displayAreYouSure(String what, String action, ArrayList<String> arguments, String fragName, Fragment callingFragment, Song song);
    void confirmedAction(boolean agree, String what, ArrayList<String> arguments, String fragName, Fragment callingFragment, Song song);
    ShowToast getShowToast();
    String getWhattodo();
    void setWhattodo(String whattodo);
    WindowFlags getWindowFlags();
    void setAlreadyBackPressed(boolean alreadyBackPressed);
    int[] getAvailableSizes();
    void setAvailableSizes(int availableWidth, int availableHeight);
    Palette getPalette();
    void refreshToolbarMenu();
    void tintBackgroundToTheme();
    Activity getMyActivity();

    // Custom slides
    CustomSlide getCustomSlide();

    // ABC Notation
    ABCNotation getAbcNotation();

    File getScreenshotFile();
    boolean validScreenShotFile();
    void setScreenshotFile(Bitmap bitmap);


    MakePDF getMakePDF();



    // Song sections and view for display
    void setSectionViews(ArrayList<View> views);
    ArrayList<View> getSectionViews();
    ArrayList<Integer> getSectionWidths();
    ArrayList<Integer> getSectionHeights();
    ArrayList<Integer> getSectionColors();
    void addSectionSize(int position, int width, int height);
    void setSectionColors(ArrayList<Integer> colors);
    void toggleScale();
    void updateMargins();
    int[] getViewMargins();


    // Song sheet titles
    void setSongSheetTitleLayout(LinearLayout linearLayout);
    LinearLayout getSongSheetTitleLayout();
    SongSheetHeaders getSongSheetHeaders();

    // Song processing
    ConvertChoPro getConvertChoPro();
    ConvertOnSong getConvertOnSong();
    ConvertJustChords getConvertJustChords();
    ConvertTextSong getConvertTextSong();
    ConvertWord getConvertWord();
    ProcessSong getProcessSong();
    ChordDisplayProcessing getChordDisplayProcessing();
    ChordDirectory getChordDirectory();
    void updateOnScreenInfo(String what);

}
