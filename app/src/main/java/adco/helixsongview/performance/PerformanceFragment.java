package adco.helixsongview.performance;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;


import adco.helixsongview.R;
import adco.helixsongview.appdata.AlertInfoBottomSheet;
import adco.helixsongview.controls.GestureListener;
import adco.helixsongview.customviews.RecyclerLayoutManager;
import adco.helixsongview.databinding.ModePerformanceBinding;
import adco.helixsongview.interfaces.DisplayInterface;
import adco.helixsongview.interfaces.MainActivityInterface;
import adco.helixsongview.songprocessing.Song;


import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;

public class PerformanceFragment extends Fragment {

    @SuppressWarnings({"FieldCanBeLocal","unused"})
    private final String TAG = "PerformanceFragment";
    private Bitmap.Config bmpFormat = Bitmap.Config.ARGB_8888;
    private MainActivityInterface mainActivityInterface;
    private DisplayInterface displayInterface;
    private int swipeMinimumDistance;
    private int swipeMaxDistanceYError;
    private int swipeMinimumVelocity;
    private int availableWidth;
    private int availableHeight;
    private int widthBeforeScale;
    private int heightBeforeScale;
    private long doSongLoadStartTime;
    private final long doSongLoadQOSTime = 0;
    private int widthAfterScale;
    private int heightAfterScale;
    private boolean processingTestView;
    private boolean isLoadingSong = false;
    // Song requested while another load was running (only the latest one is kept)
    private String pendingSongFolder = null, pendingSongFilename = null;
    // Signature of the last generated chord diagram set (avoids regenerating on every song)
    private String lastChordImageSignature = "";
    @SuppressWarnings("FieldCanBeLocal")
    private boolean songChange;
    private boolean firstSongLoad = true;
    private float scaleFactor = 1.0f;
    private ModePerformanceBinding myView;
    private Animation animSlideIn, animSlideOut;
    private GestureDetector gestureDetector;

    private RecyclerLayoutManager recyclerLayoutManager;
    private final Handler dealWithExtraStuffOnceSettledHandler = new Handler(Looper.getMainLooper());
    private final Runnable dealWithExtraStuffOnceSettledRunnable = this::dealWithExtraStuffOnceSettled;
    private String mainfoldername="";
    private String mode_performance="";
    private String mode_presenter="";
    private String mode_stage="";
    private String mode_hybrid="";
    private String image_string="";
    private String nearby_large_file_string="";
    private String inline_set_string="";
    private int sendSongDelay = 0;
    @SuppressWarnings("FieldCanBeLocal")
    // GE - hidden this option, but reserving the right to reinstate even just for me
    private final int graceTime = 1500;
    private final Handler sendSongAfterDelayHandler = new Handler(Looper.getMainLooper()),
        autoHideHighlighterHandler = new Handler(Looper.getMainLooper());
    private final Runnable sendSongAfterDelayRunnable = () -> {
        // IV - The send is always called by the 'if' and will return true if a large file has been sent
        // nearby removed
        sendSongDelay = 3000;
    };
    private final Runnable autoHideHighlighterRunnable = new Runnable() {
        @Override
        public void run() {
            if (myView!=null) {
                myView.highlighterView.setVisibility(View.GONE);
            }
        }
    };

    private final Handler resetSendSongAfterDelayHandler = new Handler(Looper.getMainLooper());
    private final Runnable resetSendSongAfterDelayRunnable = () -> {
        sendSongDelay = 0;
        // nearby removed
    };

    // Attaching and destroying
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivityInterface = (MainActivityInterface) context;
        displayInterface = (DisplayInterface) context;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Register this fragment
        mainActivityInterface.registerFragment(this,"PerformanceFragment");
        mainActivityInterface.getToolbar().setPerformanceMode(true);
        prepareStrings();
        firstSongLoad = true;
        updateInlineSetSortTitles();
        displayInterface.checkDisplays();
        if (myView!=null && getContext()!=null) {
            myView.inlineSetList.initialisePreferences(getContext(), mainActivityInterface);
            updateInlineSetVisibility();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Register this fragment
        mainActivityInterface.registerFragment(this,"PerformanceFragment");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mainActivityInterface!=null) {
            if (mainActivityInterface.getToolbar() != null) {
                mainActivityInterface.getToolbar().setPerformanceMode(false);
            }
            mainActivityInterface.registerFragment(null, "PerformanceFragment");
        }
    }
    @Override
    public void onDestroyView() {
        myView = null;
        super.onDestroyView();
    }

    // The logic to start this fragment
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        myView = ModePerformanceBinding.inflate(inflater, container, false);
        prepareStrings();

        mainActivityInterface.updateFragment("updateSongMenuSortTitles",this,null);

        // Initialise the helper classes that do the heavy lifting
        initialiseHelpers();

        // Initialise the recyclerview
        if (recyclerLayoutManager==null && getContext()!=null) {
            recyclerLayoutManager = new RecyclerLayoutManager(getContext());
            myView.recyclerView.setLayoutManager(recyclerLayoutManager);
        }
        myView.recyclerView.setItemAnimator(null);

        // Pass view references to the Autoscroll class
        mainActivityInterface.getAutoscroll().initialiseAutoscroll(myView.zoomLayout, myView.recyclerView);

        // Allow the song menu and page buttons
        mainActivityInterface.lockDrawer(false);
        mainActivityInterface.hideActionButton(false);
        //mainActivityInterface.expandActionButton();

        // Load in preferences
        loadPreferences();

        // Prepare the song menu (will be called again after indexing from the main activity index songs)
        if (mainActivityInterface.getSongListBuildIndex().getIndexRequired() &&
                !mainActivityInterface.getSongListBuildIndex().getCurrentlyIndexing()) {
            // Do this on a background thread after a 1 second delay to allow app start!
            mainActivityInterface.getMainHandler().postDelayed(() -> {
                if (mainActivityInterface!=null && myView!=null && getContext()!=null) {
                    mainActivityInterface.getThreadPoolExecutor().execute(() -> mainActivityInterface.fullIndex(null));
                }
            },2000);
        }

        // Set listeners for the scroll/scale/gestures
        setGestureListeners();

        // Show the actionBar and hide it after a time if that's the user's preference
        mainActivityInterface.getToolbar().setPerformanceMode(true);
        mainActivityInterface.showActionBar();

        // MainActivity initialisation has firstRun set as true.
        // Check for connected displays now we have loaded preferences, etc
        if (mainActivityInterface.getFirstRun()) {
            // IV - Make sure second screen overlays are off
            displayInterface.updateDisplay("showBlankscreen");
            displayInterface.updateDisplay("showBlackscreen");
            displayInterface.checkDisplays();
            mainActivityInterface.setFirstRun(false);
        }

        // Tint the watermark to the text colour
        myView.waterMark.setColorFilter(mainActivityInterface.getMyThemeColors().getLyricsTextColor(),
                android.graphics.PorterDuff.Mode.SRC_IN);

        removeViews();

        if (mainActivityInterface.getWhattodo().equals("pendingLoadSet")) {
            // If we have chosen to open the first item (preference), do that, otherwise, look for the current song
            if (mainActivityInterface.getPreferences().getMyPreferenceBoolean("setLoadFirst",true)) {
                mainActivityInterface.getCurrentSet().setIndexSongInSet(0);
                mainActivityInterface.loadSongFromSet(0);
            } else {
                processingTestView = false;
                mainActivityInterface.loadSongFromSet(Math.max(mainActivityInterface.getCurrentSet().getIndexSongInSet(), 0));

            }
            mainActivityInterface.chooseMenu(true);
            mainActivityInterface.getMainHandler().postDelayed(() -> mainActivityInterface.closeDrawer(false),2000);

        } else {
            processingTestView = false;
            String songFolder = mainActivityInterface.getPreferences().getMyPreferenceString("songFolder",mainfoldername);
            String songFilename = mainActivityInterface.getPreferences().getMyPreferenceString("songFilename", "Welcome to OpenSongApp");
            mainActivityInterface.getSetActions().indexSongInSet(songFolder,songFilename,null);
            doSongLoad(songFolder, songFilename);
        }

        // Check if we need to show an alert
        if (mainActivityInterface.getAlertChecks().showPlayServicesAlert() ||
                mainActivityInterface.getAlertChecks().showBackup() ||
                mainActivityInterface.getAlertChecks().showUpdateInfo() ||
                mainActivityInterface.getAlertChecks().showBadSongMoved()) {
                    AlertInfoBottomSheet alertInfoBottomSheet = new AlertInfoBottomSheet();
                    alertInfoBottomSheet.show(mainActivityInterface.getMyFragmentManager(), "AlertInfoBottomSheet");
                    // When we close the alert, we check again
        }

        // Pass a reference of the zoom layout to the next/prev so we can stop fling scrolls
        mainActivityInterface.getDisplayPrevNext().setZoomLayout(myView.zoomLayout);

        return myView.getRoot();
    }

    private void prepareStrings() {
        if (getContext()!=null) {
            mainfoldername = getString(R.string.mainfoldername);
            mode_performance = getString(R.string.mode_performance);
            mode_presenter = getString(R.string.mode_presenter);
            mode_stage = getString(R.string.mode_stage);
            mode_hybrid = getContext().getString(R.string.mode_hybrid);
            String not_allowed = getString(R.string.not_allowed);
            image_string= getString(R.string.image);
            nearby_large_file_string = getString(R.string.nearby_large_file);
            inline_set_string = getString(R.string.set_inline_showcase);
        }
    }

    // Getting the preferences and helpers ready
    private void initialiseHelpers() {
        if (getContext() != null) {
            mainActivityInterface.getPerformanceGestures().setZoomLayout(myView.zoomLayout);
            mainActivityInterface.getPerformanceGestures().setRecyclerView(myView.recyclerView);
            //myView.recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
            myView.recyclerView.setLayoutManager(new RecyclerLayoutManager(getContext()));
            mainActivityInterface.getHotZones().initialiseHotZones(myView.hotZoneTopLeft, myView.hotZoneTopCenter, myView.hotZoneBottomCenter);
        }
    }
    private void loadPreferences() {
        mainActivityInterface.getProcessSong().updateProcessingPreferences();
        mainActivityInterface.getMyThemeColors().getDefaultColors();
        swipeMinimumDistance = mainActivityInterface.getPreferences().getMyPreferenceInt("swipeMinimumDistance", 250);
        swipeMaxDistanceYError = mainActivityInterface.getPreferences().getMyPreferenceInt("swipeMaxDistanceYError", 200);
        swipeMinimumVelocity = mainActivityInterface.getPreferences().getMyPreferenceInt("swipeMinimumVelocity", 600);
        if (mainActivityInterface.getMode().equals(mode_performance) || mainActivityInterface.getMode().equals(mode_hybrid)) {
            myView.mypage.setBackgroundColor(mainActivityInterface.getMyThemeColors().getLyricsBackgroundColor());
            myView.waterMark.setVisibility(View.VISIBLE);
        } else if (mainActivityInterface.getMode().equals(mode_stage)) {
            // Stage Mode - sections have correct colour, but the background is different - set to background colour with a reduced alpha
            int newColor = mainActivityInterface.getMyThemeColors().adjustAlpha(mainActivityInterface.getMyThemeColors().getLyricsBackgroundColor(),0.9f);
            myView.mypage.setBackgroundColor(newColor);
        } else {
            // Presenter mode, just use primary color
            myView.mypage.setBackgroundColor(mainActivityInterface.getPalette().primary);
            myView.waterMark.setVisibility(View.GONE);
        }
        mainActivityInterface.updateOnScreenInfo("setpreferences");
        boolean allowPinchToZoom = mainActivityInterface.getPreferences().getMyPreferenceBoolean("allowPinchToZoom",true);
        myView.zoomLayout.setAllowPinchToZoom(allowPinchToZoom);
        myView.recyclerView.setAllowPinchToZoom(allowPinchToZoom);
        displayInterface.updateDisplay("showLogo");
    }

    public void tryToImportIntent() {
        // We may have opened the app at this fragment by clicking on an openable file
        // Get the main activity to check and fix backstack to this as home if required
        mainActivityInterface.dealWithIntent(R.id.performanceFragment);
    }
    private void removeViews() {
        removeViews(false);
    }

    /**
     * Clears the song views.
     * @param keepSongContent true during a song change: the previous song stays on screen
     *                        (avoiding a black flash) until the new one replaces it.
     */
    private void removeViews(boolean keepSongContent) {
        if (myView!=null) {
            try {
                // The song sheet header is null when the 'songSheet' preference is off:
                // the old code threw an NPE here and silently skipped the rest of the cleanup
                if (mainActivityInterface.getSongSheetTitleLayout() != null) {
                    mainActivityInterface.getSongSheetTitleLayout().removeAllViews();
                }
                if (!keepSongContent) {
                    myView.songView.clearViews();
                }
                myView.testPane.removeAllViews();
                myView.recyclerView.removeAllViews();
                myView.imageView.setImageDrawable(null);
                mainActivityInterface.setSectionViews(null);
            } catch (Exception e) {
                Log.e(TAG, "PerformanceFragment error", e);
            }
        }
    }

    // Inline set
    public void orientationInlineSet(int orientation) {
        myView.inlineSetList.orientationChanged(orientation);
    }
    public void toggleInlineSet() {
        myView.inlineSetList.toggleInlineSet();
        mainActivityInterface.getHotZones().checkIfRequired();
    }
    public void updateInlineSetVisibility() {
        if (myView!=null) {
            myView.inlineSetList.checkVisibility();
            mainActivityInterface.getHotZones().checkIfRequired();
        }
    }
    public void notifyToClearInlineSet(int from, int count) {
        if (myView!=null) {
            myView.inlineSetList.notifyToClearInlineSet(from,count);
        }
    }
    public void notifyToInsertAllInlineSet() {
        if (myView!=null) {
            myView.inlineSetList.post(() -> myView.inlineSetList.notifyToInsertAllInlineSet());
        }
    }
    public void notifyInlineSetInserted() {
        if (myView!=null) {
            myView.inlineSetList.notifyInlineSetInserted();
        }
    }
    public void notifyInlineSetInserted(int position) {
        if (myView!=null) {
            try {
                myView.inlineSetList.notifyInlineSetInserted(position);
            } catch (Exception e) {
                Log.d(TAG, "Couldn't update inline set - might just not be shown currently");
            }
        }
    }
    public void notifyInlineSetRemoved(int position) {
        if (myView!=null) {
            myView.inlineSetList.notifyInlineSetRemoved(position);
        }
    }
    public void notifyInlineSetMove(int from, int to) {
        if (myView!=null) {
            try {
                myView.inlineSetList.notifyInlineSetMove(from, to);
            } catch (Exception e) {
                Log.e(TAG, "PerformanceFragment error", e);
            }
        }
    }
    public void notifyInlineSetChanged(int position) {
        if (myView!=null) {
            mainActivityInterface.getMainHandler().post(() -> {
                if (myView!=null) {
                    myView.inlineSetList.notifyInlineSetChanged(position);
                    myView.inlineSetList.scrollToPosition(position);
                }
            });
        }
    }
    public void notifyInlineSetRangeChanged(int from, int count) {
        if (myView!=null) {
            myView.inlineSetList.notifyInlineSetRangeChanged(from,count);
        }
    }
    public void notifyInlineSetHighlight() {
        if (myView!=null) {
            try {
                myView.inlineSetList.notifyInlineSetHighlight();
            } catch (Exception e) {
                Log.e(TAG, "PerformanceFragment error", e);
            }
        }
    }
    public void notifyInlineSetScrollToItem() {
        if (myView!=null) {
            myView.inlineSetList.notifyInlineSetScrollToItem();
        }
    }
    public void notifyInlineSetCueItem() {
        if (myView!=null) {
            try {
                myView.inlineSetList.notifyInlineSetCueItem();
            } catch (Exception e) {
                Log.d(TAG,"Couldn't update inline set - might just not be shown currently");
            }
        }
    }


    @SuppressWarnings("ConstantConditions")
    public void updateInlineSetSortTitles() {
        if (myView!=null && myView.inlineSetList!=null && myView.inlineSetList.getChildCount()<=0) {
            try {
                myView.inlineSetList.setUseTitle(mainActivityInterface.getPreferences().getMyPreferenceBoolean("songMenuSortTitles", true));
                myView.inlineSetList.post(() -> {
                    if (myView!=null) {
                        myView.inlineSetList.notifyInlineSetUpdated();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "PerformanceFragment error", e);
            }
        }
    }


    // This stuff loads the song and prepares the views
    public void doSongLoad(String folder, String filename) {
        // Guard against overlapping/reentrant song loads that can cause the wrong song to appear.
        // The request is not lost: it is queued and run as soon as the current load finishes
        // (latest request wins), so fast song switching still ends on the right song.
        if (isLoadingSong) {
            pendingSongFolder = folder;
            pendingSongFilename = filename;
            Log.d(TAG, "doSongLoad queued: already loading");
            return;
        }
        isLoadingSong = true;
        // Reset autoscale when loading a new song (especially first song after set change)
        scaleFactor = 1.0f;
        try {
            if (myView != null) {
                myView.zoomLayout.resetLayout();
            }
        } catch (Exception ignored) {}
        processingTestView = false;
        // IV - Set a boolean indicating song change
        songChange = !mainActivityInterface.getSong().getFilename().equals(filename) ||
                !mainActivityInterface.getSong().getFolder().equals(folder) ||
                firstSongLoad ||
                mainActivityInterface.getForceReload();
        mainActivityInterface.setHighlightChangeAllowed(true);

        if (filename==null || filename.isEmpty() ||
                mainActivityInterface.getSong().getFilename()==null || mainActivityInterface.getSong().getFilename().isEmpty()) {
            songChange = true;
            filename = "Welcome to OpenSongApp";
            folder = mainfoldername;
            mainActivityInterface.getSong().setFilename("Welcome to OpenSongApp");
            mainActivityInterface.getSong().setFolder(mainfoldername);
        }

        boolean needToTryAgain = false;
        String keyInFilename;

        // If this is a key variation and it doesn't exist, we need to load the original
        if (mainActivityInterface.getVariations().getIsKeyVariation(folder,filename) &&
                !mainActivityInterface.getStorageAccess().uriExists(mainActivityInterface.getVariations().getKeyVariationUri(filename))) {
            String loadKey = mainActivityInterface.getSong().getKey();
            String[] newSongInfo = mainActivityInterface.getVariations().getPreVariationInfo(folder,filename,loadKey);
            folder = newSongInfo[0];
            filename = newSongInfo[1];
            keyInFilename = newSongInfo[2];
            mainActivityInterface.getSong().setKey(keyInFilename);
            // Try again and exit this first run
            needToTryAgain = true;
        }

        if (needToTryAgain) {
            String tryagainfolder = folder;
            String tryagainfilename = filename;
            firstSongLoad = true;
            mainActivityInterface.getMainHandler().post(() -> doSongLoad(tryagainfolder,tryagainfilename));
            // End this run now
            return;
        }

        // TODO - setSongChange as true to force the song to load every time
        songChange = true;

        // We only load a song if there is a change of song file, or we manually force it, or receive from the host
        if (myView != null && !processingTestView || mainActivityInterface.getForceReload()) {

            // Clear any force reload flags
            firstSongLoad = false;

            try {
                mainActivityInterface.setForceReload(false);
                myView.inlineSetList.setForceReload(false);
                mainActivityInterface.getTranspose().setForceReload(false);
                // nearby removed

                mainActivityInterface.setHighlightChangeAllowed(false);

                doSongLoadStartTime = System.currentTimeMillis();
                if (mainActivityInterface.getWhattodo()==null ||
                        !mainActivityInterface.getWhattodo().equals("pendingLoadSet")) {
                    mainActivityInterface.closeDrawer(true);
                }
                mainActivityInterface.setWhattodo("");

                mainActivityInterface.checkSetMenuItemHighlighted(mainActivityInterface.getCurrentSet().getPrevIndexSongInSet());

                // Make sure we only do this once (reset at the end of 'dealwithstuffafterready')
                if (!processingTestView) {
                    processingTestView = true;
                    // Loading the song is dealt with in this fragment as specific actions are required

                    // Remove capo
                    mainActivityInterface.updateOnScreenInfo("capoHide");

                                       // Stop any autoscroll if required, but not if it was activated
                    boolean autoScrollActivated = mainActivityInterface.getAutoscroll().getAutoscrollActivated();
                    mainActivityInterface.getAutoscroll().stopAutoscroll();
                    if (mainActivityInterface.getAutoscroll().getAutoscrollAutoStart()) {
                        mainActivityInterface.getAutoscroll().setAutoscrollActivated(autoScrollActivated);
                    } else {
                        mainActivityInterface.getAutoscroll().setAutoscrollActivated(false);
                    }

                    // Stop the highlighter autohide if required
                    autoHideHighlighterHandler.removeCallbacks(autoHideHighlighterRunnable);

                    String keyInSet = null;
                    boolean stillToCreateVariation;
                    int indexInSet = mainActivityInterface.getCurrentSet().getIndexSongInSet();

                    if (indexInSet==-1 || indexInSet>=mainActivityInterface.getCurrentSet().getCurrentSetSize()) {
                        indexInSet = mainActivityInterface.getSetActions().indexSongInSet(folder, filename, null);
                    }
                    if (indexInSet>-1) {
                        mainActivityInterface.getCurrentSet().setIndexSongInSet(indexInSet);
                        keyInSet = mainActivityInterface.getCurrentSet().getSetItemInfo(mainActivityInterface.getCurrentSet().getIndexSongInSet()).songkey;
                        // Compare with the indexed value
                        String keyInSong = mainActivityInterface.getSQLiteHelper().getKey(folder,filename);
                        if (keyInSong!=null && !keyInSong.isEmpty() && !keyInSet.isEmpty() && !keyInSong.equals(keyInSet)) {
                            // This is a key variation!
                            folder = mainActivityInterface.getVariations().getKeyVariationsFolder();
                            filename = mainActivityInterface.getVariations().getKeyVariationFilename(folder,filename,keyInSet);
                            // Check if the file needs to be created
                            Uri uriToCheck = mainActivityInterface.getVariations().getKeyVariationUri(filename);
                            stillToCreateVariation = !mainActivityInterface.getStorageAccess().uriExists(uriToCheck);
                        } else {
                            // Either we are in the same key, or the set didn't have a key!
                            stillToCreateVariation = false;
                        }
                    } else {
                        stillToCreateVariation = false;
                    }
                    logStage("setLookup");

                    // During the load song call, the song is cleared
                    // However it first extracts the folder and filename we've just set
                    mainActivityInterface.getSong().setFolder(folder);
                    mainActivityInterface.getSong().setFilename(filename);

                    final boolean stillToCreateVariationFinal = stillToCreateVariation;
                    final String keyInSetFinal = keyInSet;
                    mainActivityInterface.getThreadPoolExecutor().execute(() -> {
                        // TEMPORARY: elapsed-time checkpoint of the pre-stage
                        logStage("taskStart");
                        // Prepare the slide out and in animations based on swipe direction
                        setupSlideOut();
                        setupSlideIn();


                        // Now reset the song object (doesn't change what's already drawn on the screen)
                        Song songToUse;
                        if (stillToCreateVariationFinal) {
                            // If we are pointing to variation file check the file exists and if not, create it
                            boolean useExisting = true;
                            if (mainActivityInterface.getSong().getFolder().contains(mainActivityInterface.getVariations().getKeyVariationsFolder())) {
                                Uri uri = mainActivityInterface.getVariations().getKeyVariationUri(mainActivityInterface.getSong().getFilename());
                                if (!mainActivityInterface.getStorageAccess().uriExists(uri)) {
                                    String[] folderfilename = mainActivityInterface.getVariations().getPreVariationInfo(mainActivityInterface.getSong().getFolder(),mainActivityInterface.getSong().getFilename(),keyInSetFinal);
                                    mainActivityInterface.getSong().setFolder(folderfilename[0]);
                                    mainActivityInterface.getSong().setFilename(folderfilename[1]);
                                    useExisting = false;
                                }
                            }
                            if (useExisting) {
                                songToUse = mainActivityInterface.getLoadSong().doLoadSong(mainActivityInterface.getSong(), false);
                            } else {
                                songToUse = mainActivityInterface.getVariations().makeKeyVariation(mainActivityInterface.getLoadSong().doLoadSong(mainActivityInterface.getSong(), false), keyInSetFinal, true, true);
                            }
                        } else {
                            songToUse = mainActivityInterface.getLoadSong().doLoadSong(mainActivityInterface.getSong(),false);
                        }

                        long loadDuration = System.currentTimeMillis() - doSongLoadStartTime;
                        if (loadDuration >= 100) {
                            Log.d(TAG, "Song file load: " + loadDuration + "ms (" + mainActivityInterface.getSong().getFilename() + ")");
                        }
                        logStage("loaded");

                        // Set the main song with the loaded song, or the key variation song
                        mainActivityInterface.setSong(songToUse);

                        mainActivityInterface.getMainHandler().post(() -> {
                            logStage("post:bgColourToolbar");
                            if (myView != null) {
                                // Set the default color
                                myView.pageHolder.setBackgroundColor(mainActivityInterface.getMyThemeColors().getLyricsBackgroundColor());
                                // Update the toolbar with the song detail (null).
                                mainActivityInterface.updateToolbar(null);
                            }
                        });

                        // Clear any screenshot files
                        mainActivityInterface.setScreenshotFile(null);

                        mainActivityInterface.getSong().setCurrentSection(0);

                        // nearby removed

                        // Now slide out the song and after a delay start the next bit of the processing
                        if (myView != null) {
                            myView.recyclerView.post(() -> {
                                logStage("post:slideOutRecycler");
                                if (myView!=null) {
                                    try {
                                        if (myView.recyclerView.getVisibility() == View.VISIBLE) {
                                            myView.recyclerView.startAnimation(animSlideOut);
                                        }
                                    } catch (Exception e) {
                                        mainActivityInterface.getStorageAccess().updateCrashLog(e.toString());
                                    }
                                }
                            });
                            // NOTE: the page holder no longer slides out here. Doing it at the
                            // start of the load made the song disappear ~1s before the new one
                            // was ready (a long black screen). The old song now stays on screen
                            // until it is replaced, and only the slide-in animation is used.
                        }
                        if (getContext() != null) {
                            // Scroll to the item in the set menus
                            long perfNotifyStart = System.currentTimeMillis();
                            mainActivityInterface.notifySetFragment("scrollTo", mainActivityInterface.getCurrentSet().getIndexSongInSet());
                            long perfNotifyMs = System.currentTimeMillis() - perfNotifyStart;
                            if (perfNotifyMs >= 20) {
                                Log.d(TAG, "notifySetFragment: " + perfNotifyMs + "ms");
                            }
                            logStage("notifiedSet");

                            // If we want to show chord diagrams, we need to prepare them
                            if (mainActivityInterface.getPreferences().getMyPreferenceBoolean("displayChordDiagrams",false)) {
                                mainActivityInterface.getChordDisplayProcessing().initialiseArrays();

                                // Get the chords in the song
                                mainActivityInterface.getChordDisplayProcessing().findChordsInSong();

                                int chordFormat = mainActivityInterface.getPreferences().getMyPreferenceInt("chordFormat", 1);
                                if (!mainActivityInterface.getPreferences().getMyPreferenceBoolean("chordFormatUsePreferred",true)) {
                                    chordFormat = mainActivityInterface.getSong().getDetectedChordFormat();
                                }

                                // Get the song specific instrument if it is set.  If not, make sure it is empty
                                String instrument = mainActivityInterface.getSong().getPreferredInstrument();
                                if (instrument==null) {
                                    instrument = "";
                                }
                                // If it is empty, use the app wide preference
                                if (instrument.isEmpty()) {
                                    instrument = mainActivityInterface.getPreferences().getMyPreferenceString("chordInstrument","g");
                                }

                                // Set the fingerings for the chords used
                                mainActivityInterface.getChordDisplayProcessing().setFingerings(mainActivityInterface.getChordDirectory(),
                                        mainActivityInterface.getChordDisplayProcessing().getInstrumentFromPref(instrument),
                                        mainActivityInterface.getChordDisplayProcessing().getInstruments(), chordFormat);

                                float chordImageScale = mainActivityInterface.getPreferences().getMyPreferenceFloat("scaleChords",0.8f);

                                // The chord PNGs only depend on the global chord settings (instrument, format,
                                // scale and the chord/capo colours). Reuse them whenever those did not change:
                                // deleting and rewriting the whole folder for every song was a major source
                                // of load delay.
                                String chordSignature = buildChordImageSignature(instrument, chordFormat, chordImageScale);
                                boolean rebuildChordImages = !chordSignature.equals(lastChordImageSignature);

                                if (rebuildChordImages) {
                                    // Empty the chords folder - the settings changed, so all images are stale
                                    File chordsFolder = mainActivityInterface.getStorageAccess().getAppSpecificFile("Chords","","");
                                    if (chordsFolder!=null) {
                                        try {
                                            FileUtils.deleteDirectory(chordsFolder);
                                            Log.d(TAG,"Deleted chords folder");
                                        } catch (Exception e) {
                                            Log.d(TAG, "Error deleting chords folder");
                                        }
                                    }
                                }
                                // Prepare the standard chord images (only the ones that are missing)
                                mainActivityInterface.getChordDisplayProcessing().createChordImages(instrument, chordFormat, chordImageScale, false, !rebuildChordImages);

                                // If we have a capo specified in the song, also create the images for the capo chords
                                if (mainActivityInterface.getSong().getCapo()!=null && !mainActivityInterface.getSong().getCapo().isEmpty()) {
                                    mainActivityInterface.getTranspose().capoKeyTranspose(mainActivityInterface.getSong());
                                    mainActivityInterface.getChordDisplayProcessing().createChordImages(instrument, chordFormat, chordImageScale, true, !rebuildChordImages);
                                }

                                if (rebuildChordImages) {
                                    // New files mean the cached image dimensions are no longer valid
                                    mainActivityInterface.getProcessSong().clearChordImageSizeCache();
                                    lastChordImageSignature = chordSignature;
                                }

                            }

                            logStage("chordImagesDone");

                            // Now continue with the song prep
                            if (getContext()!=null && mainActivityInterface!=null) {
                                int delay = 50 + getContext().getResources().getInteger(R.integer.slide_out_time);
                                logStage("schedulePrepareViews+" + delay);
                                long pingStart = System.currentTimeMillis();
                                mainActivityInterface.getMainHandler().post(
                                        () -> Log.d(TAG, "mainPing: " + (System.currentTimeMillis() - pingStart) + "ms"));
                                mainActivityInterface.getMainHandler().postDelayed(this::prepareSongViews, delay);
                            }
                        }
                    });
                }

            } catch (Exception e) {
                mainActivityInterface.getStorageAccess().updateCrashLog(e.toString());
            }
        }
        logStage("doSongLoadEnd");
    }
    /**
     * Builds a signature describing everything the generated chord diagram PNGs depend on.
     * If it matches the previous song, the files are reused instead of being rebuilt.
     */
    private String buildChordImageSignature(String instrument, int chordFormat, float chordImageScale) {
        StringBuilder signature = new StringBuilder();
        signature.append(instrument).append('|')
                .append(chordFormat).append('|')
                .append(chordImageScale).append('|')
                .append(mainActivityInterface.getMyThemeColors().getLyricsChordsColor()).append('|')
                .append(mainActivityInterface.getMyThemeColors().getLyricsCapoColor());
        return signature.toString();
    }

    private void setupSlideOut() {
        // Set up the type of animate in
        if (getContext()!=null) {
            if (mainActivityInterface.getDisplayPrevNext().getSwipeDirection().equals("R2L")) {
                animSlideOut = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_left);
            } else {
                animSlideOut = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_right);
            }
        }
    }
    private void setupSlideIn() {
        // Set up the type of animate in
        if (getContext()!=null) {
            if (mainActivityInterface.getDisplayPrevNext().getSwipeDirection().equals("R2L")) {
                animSlideIn = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_right);
            } else {
                animSlideIn = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_left);
            }
        }
    }
    private void prepareSongViews() {
        if (myView!=null) {
            // This is called on the UI thread above via the handler from mainLooper()
            logStage("prepareSongViews");
            // Reset the song views
            mainActivityInterface.setSectionViews(null);
            long perfRemoveStart = System.currentTimeMillis();
            // Keep the previous song on screen while the new one is built: clearing it here
            // leaves the page blank for the whole build (the black flash on song changes)
            removeViews(true);
            long perfRemoveMs = System.currentTimeMillis() - perfRemoveStart;
            if (perfRemoveMs >= 20) {
                Log.d(TAG, "removeViews: " + perfRemoveMs + "ms");
            }
            logStage("removedViews");

            // Make sure we are scrolled to the top of the views
            myView.recyclerView.scrollToTop();
            myView.zoomLayout.stopFlingScroll();
            myView.zoomLayout.doScrollTo(0,0);
            if (myView.recyclerView.getLayoutManager()!=null) {
                myView.recyclerView.getLayoutManager().scrollToPosition(0);
                try {
                    ((LinearLayoutManager) myView.recyclerView.getLayoutManager()).scrollToPositionWithOffset(0, 0);
                } catch (Exception e) {
                    Log.e(TAG, "PerformanceFragment error", e);
                }
            }
            logStage("scrolled");

            int[] screenSizes = mainActivityInterface.getDisplayMetrics();
            int screenWidth = screenSizes[0];
            int screenHeight = screenSizes[1];

            int[] viewPadding = mainActivityInterface.getViewMargins();

            availableWidth = screenWidth - viewPadding[0] - viewPadding[1] - myView.inlineSetList.getInlineSetWidth();
            availableHeight = screenHeight - viewPadding[2] - viewPadding[3];

            mainActivityInterface.setAvailableSizes(availableWidth,availableHeight);

            widthBeforeScale = 0;
            heightBeforeScale = 0;

            // Depending on what we are doing, create the content.
            // Options are
            // - IMG        Image file.  Use the imageView (inside pageHolder, inside zoomLayout).  All sizes from the bitmap file.
            // - **Image    Custom image slide (multiple images).
            // - XML        OpenSong song.  Views need to be drawn and measured. Stage mode uses recyclerView, Performance, the pageHolder

            // Reset the recycler view to vertical by default
            if (recyclerLayoutManager!=null) {
                recyclerLayoutManager.setOrientation(RecyclerLayoutManager.VERTICAL);
            }

            if (mainActivityInterface.getSong()!=null && mainActivityInterface.getSong().getFiletype()==null) {
                mainActivityInterface.getSong().setFiletype(mainActivityInterface.getStorageAccess().tryToFixFileTypeFromNull(mainActivityInterface.getSong().getFilename()));
            }

            if (mainActivityInterface.getSong() != null &&
                    mainActivityInterface.getSong().getFiletype() != null &&
                    mainActivityInterface.getSong().getFolder() != null) {

                if (mainActivityInterface.getSong().getFiletype().equals("IMG")) {
                    prepareIMGView();

                } else if (mainActivityInterface.getSong().getFolder().contains("**Image")) {
                    prepareSlideImageView();

                } else if (mainActivityInterface.getSong().getFiletype().equals("XML")) {
                    prepareXMLView();
                } else {
                    endProcessing();
                }
            }
        }
    }

    private void prepareIMGView() {
        // We use the imageView inside the pageHolder, inside the zoomLayout
        myView.recyclerView.setVisibility(View.GONE);
        myView.pageHolder.setVisibility(View.INVISIBLE);
        myView.songView.setVisibility(View.GONE);
        myView.zoomLayout.setVisibility(View.VISIBLE);
        myView.imageView.setVisibility(View.VISIBLE);

        // Get a bmp from the image
        Bitmap bmp = mainActivityInterface.getProcessSong().getSongBitmap(mainActivityInterface.getSong().getFolder(),
                mainActivityInterface.getSong().getFilename());

        if (bmp!=null) {
            widthBeforeScale = bmp.getWidth();
            heightBeforeScale = bmp.getHeight();
        } else {
            widthBeforeScale = 1;
            heightBeforeScale = 1;
        }

        myView.zoomLayout.setPageSize(availableWidth, availableHeight);
        //IV - Reset zoom
        myView.zoomLayout.resetLayout();
        myView.zoomLayout.toggleScale();

        if (widthBeforeScale>0 && heightBeforeScale>0) {
            String scaleMethod = mainActivityInterface.getPreferences().getMyPreferenceString("songAutoScale", "W");
            switch (scaleMethod) {
                case "W":
                    scaleFactor = (float) availableWidth / (float) widthBeforeScale;
                    break;
                case "Y":
                    scaleFactor = Math.min((float) availableWidth / (float) widthBeforeScale, (float) availableHeight / (float) heightBeforeScale);
                    break;
                default:
                    scaleFactor = 1f;
            }
        } else {
            scaleFactor = 1f;
        }

        // Pass this scale factor to the zoom layout
        myView.zoomLayout.setCurrentScale(scaleFactor);

        widthAfterScale = (int) (widthBeforeScale*scaleFactor);
        heightAfterScale= (int) (heightBeforeScale*scaleFactor);

        myView.pageHolder.getLayoutParams().width = widthAfterScale;
        myView.pageHolder.getLayoutParams().height = heightAfterScale;
        myView.imageView.getLayoutParams().width = widthAfterScale;
        myView.imageView.getLayoutParams().height = heightAfterScale;

        if (getContext() != null) {
            myView.imageView.setImageBitmap(bmp);
        }
        myView.zoomLayout.setSongSize(widthAfterScale, heightAfterScale + (int)(mainActivityInterface.getSongSheetTitleLayout().getHeight()*scaleFactor));

        // Slide in
        myView.pageHolder.post(() -> {
            if (myView!=null) {
                myView.pageHolder.setVisibility(View.VISIBLE);
                myView.pageHolder.startAnimation(animSlideIn);
            }
        });

        // Deal with song actions to run after display (highlighter, notes, etc)
        dealWithStuffAfterReady(true);

    }
    private void prepareSlideImageView() {
        // An image slide.  Use the recyclerView with a new arrayAdapter
        myView.pageHolder.setVisibility(View.GONE);
        myView.songView.setVisibility(View.GONE);
        myView.zoomLayout.setVisibility(View.GONE);
        myView.recyclerView.setVisibility(View.INVISIBLE);
//        if (getContext()!=null) {
//            imageSlideAdapter = new ImageSlideAdapter(getContext(), mainActivityInterface, displayInterface,
//                    availableWidth, availableHeight);
//            try {
//                imageSlideAdapter.setInlineSetWidth(myView.inlineSetList.getInlineSetWidth());
//            } catch (Exception e) {
//                Log.e(TAG, "PerformanceFragment error", e);
//            }
//        }

        // If we have a time for each slide, set the song duration
        if (mainActivityInterface.getSong().getUser1()!=null && !mainActivityInterface.getSong().getUser1().isEmpty()) {
            int time;
            try {
                time = Integer.parseInt(mainActivityInterface.getSong().getUser1());
            } catch (Exception e) {
                time = 0;
            }
//            mainActivityInterface.getSong().setAutoscrolllength(String.valueOf((time*imageSlideAdapter.getItemCount())));
            mainActivityInterface.getSong().setAutoscrolldelay("0");
        }

        myView.recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                myView.recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
//                widthBeforeScale = imageSlideAdapter.getWidth();
                widthAfterScale = widthBeforeScale;
//                heightBeforeScale = imageSlideAdapter.getHeight();
                heightAfterScale = heightBeforeScale;
//                recyclerLayoutManager.setSizes(imageSlideAdapter.getWidths(), imageSlideAdapter.getHeights(), availableWidth, availableHeight,1f);
                myView.recyclerView.setMaxScrollY(heightAfterScale - availableHeight);
                //IV - Reset zoom
                myView.recyclerView.toggleScale();

                // Slide in
                myView.recyclerView.setVisibility(View.VISIBLE);
                myView.recyclerView.startAnimation(animSlideIn);

                // Deal with song actions to run after display (highlighter, notes, etc)
                dealWithStuffAfterReady(true);

                // Get a null screenshot
                getScreenshot(0,0,0);

            }
        });
//        myView.recyclerView.setAdapter(imageSlideAdapter);

    }
    private void prepareXMLView() {
        logStage("prepareXMLView");
        if (mainActivityInterface.getSong().getFiletype()==null) {
            mainActivityInterface.getSong().setFiletype(mainActivityInterface.getStorageAccess().tryToFixFileTypeFromNull(mainActivityInterface.getSong().getFilename()));
        }

        mainActivityInterface.setSectionViews(null);

        // Now prepare the song sections views so we can measure them for scaling
        ArrayList<View> sectionViews = mainActivityInterface.getProcessSong().
                setSongInLayout(mainActivityInterface.getSong(), false, false);
        mainActivityInterface.setSectionViews(sectionViews);

        // Measure the sections straight away. They are not attached to the window, so
        // no layout pass is needed (the old test pane laid the whole song out twice).
        mainActivityInterface.getProcessSong().measureSectionViews(sectionViews);
        logStage("measured");

        // Prepare the song sheet header if required, if not, make it null
        if (mainActivityInterface.getPreferences().getMyPreferenceBoolean("songSheet", false)) {
            mainActivityInterface.setSongSheetTitleLayout(mainActivityInterface.getSongSheetHeaders().getSongSheet(
                    mainActivityInterface.getSong(), mainActivityInterface.getProcessSong().getScaleComments(), mainActivityInterface.getMyThemeColors().getLyricsTextColor()));
        } else {
            mainActivityInterface.setSongSheetTitleLayout(null);
        }
        logStage("header");

        // Measure the header here, so nothing has to be attached to the window and
        // waited for. This used to cost a full layout pass + two handler round trips
        // on every song change.
        measureSongSheetHeader();

        // Everything needed to display the song is now measured
        setUpTestViewListener();
    }

    /**
     * Measures the song sheet header in place (without attaching it) so its height is
     * known when the columns are worked out.
     */
    private void measureSongSheetHeader() {
        View header = mainActivityInterface.getSongSheetTitleLayout();
        if (header == null || myView == null ||
                mainActivityInterface.getMode().equals(mode_presenter)) {
            return;
        }
        // The header can be measured while still attached; addViewsToScreen re-parents it
        int widthSpec = View.MeasureSpec.makeMeasureSpec(Math.max(availableWidth, 0),
                View.MeasureSpec.AT_MOST);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        header.measure(widthSpec, heightSpec);
        // layout() is needed because the column code reads getHeight()
        header.layout(0, 0, header.getMeasuredWidth(), header.getMeasuredHeight());
    }

    private void setUpTestViewListener() {
        logStage("testViewListener");
        if (myView == null) {
            return;
        }

        myView.songView.clearViews();
        myView.testPane.removeAllViews();

        if (mainActivityInterface.getSectionViews().isEmpty()) {
            // Nothing to display - preserve previous behaviour (no display update)
            return;
        }

        // The sections were measured as soon as they were created
        // (ProcessSong.measureSectionViews), so there is no need to attach them to a
        // hidden pane and wait for a layout pass before showing the song.
        songIsReadyToDisplay();
    }

    private void songIsReadyToDisplay() {
        logStage("readyToDisplay");
        if (myView!=null) {
            try {
                // Set the page holder to fullscreen for now
                myView.pageHolder.getLayoutParams().width = availableWidth;
                myView.pageHolder.getLayoutParams().height = availableHeight;
                myView.songSheetTitle.setVisibility(View.VISIBLE);

                myView.testPane.removeAllViews();

                // Decide which mode we are in to determine how the views are rendered
                if (mainActivityInterface.getMode().equals(mode_stage) || mainActivityInterface.getMode().equals(mode_hybrid)) {
                    // We are in Stage or Hybrid mode so use the recyclerView
                    myView.recyclerView.setVisibility(View.INVISIBLE);
                    myView.pageHolder.setVisibility(View.GONE);
                    myView.songView.setVisibility(View.GONE);
                    myView.zoomLayout.setVisibility(View.GONE);
                    myView.highlighterView.setVisibility(View.GONE);
                    if (getContext() != null) {
                        if (mainActivityInterface.getSong().getFiletype()==null) {
                            mainActivityInterface.getSong().setFiletype(mainActivityInterface.getStorageAccess().tryToFixFileTypeFromNull(mainActivityInterface.getSong().getFilename()));
                        }

                    }

                    myView.recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (myView!=null) {
                                myView.recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);


                                myView.recyclerView.setHasFixedSize(false);
                                myView.recyclerView.setMaxScrollY(heightAfterScale - availableHeight);
                                myView.recyclerView.setPadding(myView.inlineSetList.getInlineSetWidth(), 0, 0, 0);

                                endProcessing();

                                // Slide in
                                long QOSAdjustment = doSongLoadQOSTime - (System.currentTimeMillis() - doSongLoadStartTime);

                                myView.recyclerView.setVisibility(View.VISIBLE);
                                //IV - Reset zoom
                                myView.recyclerView.toggleScale();

                                mainActivityInterface.getMainHandler().postDelayed(() -> {
                                    if (myView!=null) {
                                        myView.recyclerView.startAnimation(animSlideIn);

                                        dealWithStuffAfterReady(false);

                                        // Get a null screenshot
                                        getScreenshot(0, 0, 0);
                                    }
                                }, Math.max(0, QOSAdjustment));
                            }
                        }
                    });


                } else {
                    // We are in Performance mode, so use the songView
                    // NOTE: pageHolder is left visible so the previous song stays on screen
                    // until the new one is inserted, instead of flashing to the background
                    myView.zoomLayout.setVisibility(View.VISIBLE);
                    myView.songView.setVisibility(View.VISIBLE);
                    myView.imageView.setVisibility(View.GONE);
                    myView.recyclerView.setVisibility(View.GONE);
                    myView.highlighterView.setVisibility(View.GONE);

                    myView.zoomLayout.setPageSize(availableWidth, availableHeight);
                    //IV - Reset zoom
                    myView.zoomLayout.resetLayout();
                    myView.zoomLayout.toggleScale();

                    myView.pageHolder.getLayoutParams().width = availableWidth;
                    myView.pageHolder.getLayoutParams().height = availableHeight;

                    float[] scaleInfo = mainActivityInterface.getProcessSong().addViewsToScreen(
                            mainActivityInterface.getSong(),
                            mainActivityInterface.getSectionViews(),
                            mainActivityInterface.getSectionWidths(), mainActivityInterface.getSectionHeights(),
                            myView.pageHolder, myView.songView, myView.songSheetTitle,
                            availableWidth, availableHeight, myView.songView.getCol1(), myView.songView.getCol2(),
                            myView.songView.getCol3(), false, getResources().getDisplayMetrics());

                    // Determine how many columns are scaled
                    heightAfterScale = 0;
                    if (scaleInfo[0] == 1) {
                    /*float[] {1,           // Number of columns
                    1    oneColumnScale,    // Overall best scale
                    2    col1_1Width,       // Column 1 max width
                    3    col1_1Height,      // Column 1 total height
                    4    sectionSpace}      // Section space per view except last column */
                        scaleFactor = scaleInfo[1];
                        widthBeforeScale = (int) scaleInfo[2];
                        heightBeforeScale = (int) scaleInfo[3];
                        widthAfterScale = (int) (widthBeforeScale * scaleFactor);
                        heightAfterScale = (int) (heightBeforeScale * scaleFactor);
                        myView.pageHolder.getLayoutParams().width = widthAfterScale;
                    } else if (scaleInfo[0] == 2) {
                    /*float[]{2,             // Number of columns
                    1    twoColumnScale,     // Overall best scale
                    2    columnBreak2,       // Break point
                    3    col1_2ScaleBest,    // Best col 1 scale
                    4    col1_2Width,        // Column 1 max width
                    5    col1_2Height,       // Column 1 total height
                    6    col2_2ScaleBest,    // Best col 2 scale
                    7    col2_2Width,        // Column 2 max width
                    8    col2_2Height,       // Column 2 total height
                    9    sectionSpace}       // Section space per view except last column */
                        scaleFactor = Math.max(scaleInfo[3], scaleInfo[6]);
                        widthBeforeScale = availableWidth;
                        heightBeforeScale = (int) Math.max(scaleInfo[5], scaleInfo[8]);
                        widthAfterScale = availableWidth;
                        heightAfterScale = (int) Math.max(scaleInfo[3] * scaleInfo[5], scaleInfo[6] * scaleInfo[8]);
                        myView.pageHolder.getLayoutParams().width = availableWidth;
                        myView.songView.getLayoutParams().width = availableWidth;
                    } else if (scaleInfo[0] == 3) {
                    /*float[]{3,             // Number of columns
                    1    threeColumnScale,   // Overall best scale
                    2    columnBreak3_a,     // Break point 1
                    3    columnBreak3_b,     // Break point 2
                    4    col1_3ScaleBest,    // Best col 1 scale
                    5    col1_3Width,        // Column 1 max width
                    6    col1_3Height,       // Column 1 total height
                    7    col2_3ScaleBest,    // Best col 2 scale
                    8    col2_3Width,        // Column 2 max width
                    9    col2_3Height,       // Column 2 total height
                    10   col3_3ScaleBest,    // Best col 3 scale
                    11   col3_3Width,        // Column 3 max width
                    12   col3_3Height,       // Column 3 total height
                    13   sectionSpace};      // Section space per view except last in column */
                        widthBeforeScale = availableWidth;
                        heightBeforeScale = (int) Math.max(scaleInfo[6], Math.max(scaleInfo[9], scaleInfo[12]));
                        widthAfterScale = availableWidth;
                        scaleFactor = Math.max(scaleInfo[4], Math.max(scaleInfo[7], scaleInfo[10]));
                        heightAfterScale = (int) Math.max(scaleInfo[4] * scaleInfo[6], Math.max(scaleInfo[7] * scaleInfo[9], scaleInfo[10] * scaleInfo[12]));
                        myView.pageHolder.getLayoutParams().width = availableWidth;
                        myView.songView.getLayoutParams().width = availableWidth;
                    }

                    heightAfterScale = heightAfterScale + mainActivityInterface.getSongSheetTitleLayout().getHeight();
                    myView.pageHolder.getLayoutParams().height = heightAfterScale;
                    myView.songView.getLayoutParams().height = heightAfterScale;

                    // Pass this scale factor to the zoom layout as the new minimum scale
                    myView.zoomLayout.setCurrentScale(scaleFactor);
                    myView.zoomLayout.setSongSize(widthAfterScale, heightAfterScale);
                    logStage("songSize");

                    endProcessing();

                    // TEMPORARY: how long until the song is actually drawn on screen
                    final long displayStartTime = System.currentTimeMillis();
                    myView.pageHolder.getViewTreeObserver().addOnPreDrawListener(
                            new ViewTreeObserver.OnPreDrawListener() {
                                @Override
                                public boolean onPreDraw() {
                                    if (myView == null) {
                                        return true;
                                    }
                                    myView.pageHolder.getViewTreeObserver().removeOnPreDrawListener(this);
                                    long drawMs = System.currentTimeMillis() - displayStartTime;
                                    if (drawMs >= 50) {
                                        Log.d(TAG, "Layout + first draw: " + drawMs + "ms");
                                    }
                                    return true;
                                }
                            });

                    // Slide in
                    long QOSAdjustment = doSongLoadQOSTime - (System.currentTimeMillis() - doSongLoadStartTime);

                    myView.zoomLayout.postDelayed(() -> {
                        try {
                            // IV - We need to request layout to get songsheet information added ahead of animate in. Odd!
                            if (myView != null) {
                                myView.pageHolder.requestLayout();
                                myView.pageHolder.setVisibility(View.VISIBLE);
                                myView.pageHolder.startAnimation(animSlideIn);

                                // TEMPORARY: time spent after the song is ready
                                long dealStartTime = System.currentTimeMillis();
                                dealWithStuffAfterReady(false);
                                long dealMs = System.currentTimeMillis() - dealStartTime;
                                if (dealMs >= 20) {
                                    Log.d(TAG, "dealWithStuffAfterReady: " + dealMs + "ms");
                                }

                                // NOTE: the old full-page screenshot used to be rendered here.
                                // It was never read anywhere in the app, but drawing the whole
                                // song into a large bitmap (plus PNG compression to disk) on every
                                // song change was expensive and caused dropped frames. Removed.
                            }
                        } catch (Exception e) {
                            mainActivityInterface.getStorageAccess().updateCrashLog(e.toString());
                            Log.e(TAG, "PerformanceFragment error", e);
                        }
                    }, Math.max(0, QOSAdjustment));
                }

            } catch (Exception e) {
                mainActivityInterface.getStorageAccess().updateCrashLog(e.toString());
                Log.e(TAG, "PerformanceFragment error", e);
            }
        }
    }

    private void endProcessing () {
        // Set the load status to the song (used to enable nearby section change  listener)
        mainActivityInterface.getSong().setCurrentlyLoading(false);

        // Set the previous/next
        long perfPrevNextStart = System.currentTimeMillis();
        mainActivityInterface.getDisplayPrevNext().setPrevNext();
        long perfPrevNextMs = System.currentTimeMillis() - perfPrevNextStart;
        if (perfPrevNextMs >= 20) {
            Log.d(TAG, "setPrevNext: " + perfPrevNextMs + "ms");
        }

        // Total time from the song load request until the song is ready to display
        if (doSongLoadStartTime > 0) {
            long totalDuration = System.currentTimeMillis() - doSongLoadStartTime;
            if (totalDuration >= 150) {
                Log.d(TAG, "Song switch total: " + totalDuration + "ms");
            }
        }

        // Release the processing lock
        processingTestView = false;

        // Safety: always release the load guard, even on paths that never reach
        // dealWithStuffAfterReady (e.g. unsupported file types)
        isLoadingSong = false;

        // If the user requested another song while this one was loading, honour it now
        runPendingSongLoadIfAny();
    }

    // TEMPORARY: elapsed-time checkpoints of the song switch pipeline
    private void logStage(String stage) {
        if (doSongLoadStartTime > 0) {
            Log.d(TAG, "stage " + stage + ": +" + (System.currentTimeMillis() - doSongLoadStartTime) + "ms");
        }
    }

    /**
     * Runs a song load that was requested while another load was in progress.
     * Only the most recent request is kept.
     */
    private void runPendingSongLoadIfAny() {
        if (pendingSongFilename == null) {
            return;
        }
        String folder = pendingSongFolder;
        String filename = pendingSongFilename;
        pendingSongFolder = null;
        pendingSongFilename = null;
        if (mainActivityInterface == null) {
            return;
        }
        if (filename.equals(mainActivityInterface.getSong().getFilename()) &&
                folder != null && folder.equals(mainActivityInterface.getSong().getFolder())) {
            // Already showing this song
            return;
        }
        mainActivityInterface.getMainHandler().post(() -> doSongLoad(folder, filename));
    }

    // This stuff deals with running song actions
    @SuppressWarnings("ConstantConditions")
    private void dealWithStuffAfterReady(boolean callEndProcessing) {
        isLoadingSong = false;
        if (callEndProcessing) {
            endProcessing();
        }


        mainActivityInterface.setHighlightChangeAllowed(true);

        // Update the secondary display (if present)
        displayInterface.updateDisplay("newSongLoaded");
        displayInterface.updateDisplay("setSongInfo");
        displayInterface.updateDisplay("initialiseInfoBarRequired");
        if (mainActivityInterface.getSong().getFiletype()==null) {
            mainActivityInterface.getSong().setFiletype(mainActivityInterface.getStorageAccess().tryToFixFileTypeFromNull(mainActivityInterface.getSong().getFilename()));
        }
        if (mainActivityInterface.getMode().equals(mode_stage) &&
                mainActivityInterface.getSong().getFiletype().equals("XML") &&
                !mainActivityInterface.getSong().getFolder().contains("**Image") &&
                !mainActivityInterface.getSong().getFolder().contains("**"+image_string)) {
            displayInterface.updateDisplay("setSongContent");
        } else if (!mainActivityInterface.getSong().getFiletype().equals("XML") ||
                mainActivityInterface.getSong().getFolder().contains("**Image")) {

            displayInterface.updateDisplay("showSection");
        } else {
            displayInterface.updateDisplay("setSongContent");
        }

        // Now deal with the highlighter file
        if (mainActivityInterface.getMode().equals(mode_performance) ||
                mainActivityInterface.getMode().equals(mode_hybrid)) {
            // The highlighter is already part of the scaling if it isn't an xml
            if (mainActivityInterface.getSong().getFiletype()==null ||
                    mainActivityInterface.getSong().getFiletype().isEmpty() ||
                    mainActivityInterface.getSong().getFiletype().equals("XML")) {
                if (myView.inlineSetList!=null && myView.inlineSetList.getWidth()>0) {
                    int actualAvailableWidth = availableWidth + myView.inlineSetList.getWidth();
                    float rescale = (float) actualAvailableWidth / widthBeforeScale;
                    int newWidth = (int)(widthAfterScale * rescale);
                    int newHeight = (int)(heightAfterScale * rescale);
                    dealWithHighlighterFile(newWidth, newHeight);
                } else {
                    dealWithHighlighterFile(widthAfterScale, heightAfterScale);
                }
            } else {
                dealWithHighlighterFile(widthBeforeScale, heightBeforeScale);
            }
        }


        // Show the audio recorder if we requested it from the settings menu
        mainActivityInterface.displayAudioRecorder();



        mainActivityInterface.moveToSongInSongMenu();

        // Run this only when the user has stopped on a song after 2s.
        // This is important for pad use - the pad will not change while the user rapidly changes songs.
        // This is important for rapid song changes - we only run autoscroll, metronome etc. for the last song.
        // For pads, once settled on a song the user has 2s grace to prep to play the song before cross fade.
        // A good time to change capo!
        dealWithExtraStuffOnceSettledHandler.removeCallbacks((dealWithExtraStuffOnceSettledRunnable));
        dealWithExtraStuffOnceSettledHandler.postDelayed(dealWithExtraStuffOnceSettledRunnable, graceTime);

        // If we are in a set, send that info to the inline set custom view to see if it should draw
        myView.inlineSetList.checkVisibility();
        mainActivityInterface.getMainHandler().postDelayed(() -> {
            if (myView!=null && myView.inlineSetList.getChildCount()>=0) {
                myView.inlineSetList.notifyInlineSetHighlight();
                // Showcase what this is
                if (myView.inlineSetList.getVisibility() == View.VISIBLE) {
                    // Just in case it is empty!
                    try {
                        if (myView != null) {
                            myView.inlineSetList.postDelayed(() -> {
                                if (myView!=null && myView.inlineSetList!=null) {
                                    try {
                                        mainActivityInterface.getShowCase().singleShowCase(
                                                (Activity) mainActivityInterface,
                                                myView.inlineSetList.getChildAt(0), null,
                                                inline_set_string, true, "inline_set");
                                    } catch (Exception e) {
                                        mainActivityInterface.getStorageAccess().updateCrashLog(e.toString());
                                        Log.e(TAG, "PerformanceFragment error", e);
                                    }
                                }
                            },800);
                        }
                    } catch (Exception e) {
                        mainActivityInterface.getStorageAccess().updateCrashLog(e.toString());
                        Log.e(TAG, "PerformanceFragment error", e);
                    }
                }
            }
        },800);
    }

    private void dealWithExtraStuffOnceSettled() {
        if (getContext()!=null && myView!=null) {
            // Create temp files to debug
            mainActivityInterface.getHotZones().checkScrollButtonOn(myView.zoomLayout,myView.recyclerView);
            // Send the autoscroll information (if required)
            mainActivityInterface.getAutoscroll().initialiseSongAutoscroll(widthAfterScale, heightAfterScale, availableWidth, availableHeight);
            if (mainActivityInterface.getAutoscroll().getShouldAutostart()) {
                mainActivityInterface.getAutoscroll().startAutoscroll();
            }


            // Deal with capo information (if required)
            mainActivityInterface.updateOnScreenInfo("capoShow");
            mainActivityInterface.dealWithCapo();

            // Start the pad (if the pads are activated and the pad is valid)
            mainActivityInterface.getPad().autoStartPad();

            // Update any midi commands (if any)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (
                        mainActivityInterface.getMidi().getMidiSendAuto()) {

                    int delay = 0;


                    if (mainActivityInterface.getMidi().getMidiSendAuto()) {
                        mainActivityInterface.getMainHandler().postDelayed(() -> {
                            // These are addition to beatbuddy, so sent afterwards
                            mainActivityInterface.getMidi().buildSongMidiMessages();
                            mainActivityInterface.getMidi().sendSongMessages();
                        }, delay);
                    }

                }
            }

            // Check the set index
            mainActivityInterface.checkSetMenuItemHighlighted(mainActivityInterface.getCurrentSet().getIndexSongInSet());
            mainActivityInterface.notifySetFragment("scrollTo",mainActivityInterface.getCurrentSet().getIndexSongInSet());
            mainActivityInterface.getDisplayPrevNext().setPrevNext();


            // Update the view log usage
            mainActivityInterface.getStorageAccess().updateFileUsageLog(mainActivityInterface.getSong());
            mainActivityInterface.getAnalyticsHelper().incrementViewCount(mainActivityInterface.getSong().getUuid());

            // If we opened the app with and intent/file, check if we need to import
            tryToImportIntent();

            mainActivityInterface.updateOnScreenInfo("showhide");
        }
    }

    private void dealWithHighlighterFile(int w, int h) {
        try {
            if (getContext()!=null) {
                String highlighterFilename = mainActivityInterface.getProcessSong().
                        getHighlighterFilename(mainActivityInterface.getSong(),
                                getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT,-1);
                Uri highlighterUri = mainActivityInterface.getStorageAccess().getUriForItem("Highlighter","",highlighterFilename);

                if (myView!=null && mainActivityInterface.getStorageAccess().uriExists(highlighterUri)) {
                    // Set the highlighter image view to match
                    myView.highlighterView.setVisibility(View.INVISIBLE);
                    // Once the view is ready at the required size, deal with it
                    ViewTreeObserver highlighterVTO = myView.highlighterView.getViewTreeObserver();
                    highlighterVTO.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            try {
                                if (getContext() != null) {
                                    if (myView != null && myView.highlighterView.getViewTreeObserver() != null) {
                                        myView.highlighterView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                        // Load in the bitmap with these dimensions
                                        // v5 used portrait and landscape views.  However, now if we only have one
                                        // column, we will always load the portrait view
                                        // landscape is now for columns
                                        Bitmap highlighterBitmap = null;
                                        try {
                                            highlighterBitmap = mainActivityInterface.getProcessSong().
                                                    getHighlighterFile(0, 0);
                                        } catch (Exception e) {
                                            Log.d(TAG,"This is the error catch");
                                        }

                                        if (highlighterBitmap == null) {
                                            // Clear the previous highlighter image
                                            myView.highlighterView.setImageDrawable(null);
                                        } else if (mainActivityInterface.getPreferences().getMyPreferenceBoolean("drawingAutoDisplay", true)) {

                                            // If the bitmap doesn't match the view, scale it
                                            float bmpXScale = (float) w / (float) highlighterBitmap.getWidth();
                                            float bmpYScale = (float) h / (float) highlighterBitmap.getHeight();

                                            myView.highlighterView.setVisibility(View.VISIBLE);
                                            ViewGroup.LayoutParams rlp = myView.highlighterView.getLayoutParams();
                                            rlp.width = (int) ((float) w * bmpXScale);
                                            rlp.height = (int) ((float) h * bmpYScale);

                                            myView.highlighterView.setLayoutParams(rlp);
                                            myView.highlighterView.setImageBitmap(highlighterBitmap);

                                            myView.highlighterView.setPivotX(0f);
                                            myView.highlighterView.setPivotY(0f);

                                            // Hide after a certain length of time
                                            int timetohide = mainActivityInterface.getPreferences().getMyPreferenceInt("timeToDisplayHighlighter", 0);
                                            if (timetohide != 0) {
                                                autoHideHighlighterHandler.postDelayed(
                                                        autoHideHighlighterRunnable, timetohide * 1000L);
                                            }
                                        } else {
                                            myView.highlighterView.post(() -> {
                                                if (myView != null) {
                                                    try {
                                                        myView.highlighterView.setVisibility(View.GONE);
                                                    } catch (Exception e) {
                                                        mainActivityInterface.getStorageAccess().updateCrashLog(e.toString());
                                                        Log.e(TAG, "PerformanceFragment error", e);
                                                    }
                                                }
                                            });
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                mainActivityInterface.getStorageAccess().updateCrashLog(e.toString());
                                Log.e(TAG, "PerformanceFragment error", e);
                            }
                        }
                    });
                    myView.highlighterView.post(() -> {
                        if (myView!=null) {
                            try {
                                myView.highlighterView.requestLayout();
                            } catch (Exception e) {
                                mainActivityInterface.getStorageAccess().updateCrashLog(e.toString());
                                Log.e(TAG, "PerformanceFragment error", e);
                            }
                        }
                    });
                } else if (myView!=null) {
                    myView.highlighterView.post(() -> {
                        if (myView!=null && getContext()!=null) {
                            try {
                                // Clear the previous highlighter image
                                myView.highlighterView.setImageDrawable(null);

                                myView.highlighterView.setVisibility(View.GONE);
                            } catch (Exception e) {
                                mainActivityInterface.getStorageAccess().updateCrashLog(e.toString());
                                Log.e(TAG, "PerformanceFragment error", e);
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            mainActivityInterface.getStorageAccess().updateCrashLog(e.toString());
            Log.e(TAG, "PerformanceFragment error", e);
        }
    }
    private void getScreenshot(int w, int h, int topPadding) {
        if (!mainActivityInterface.getPreferences().
                getMyPreferenceString("songAutoScale","W").equals("N")
                && w!=0 && h!=0) {
            try {
                Bitmap bitmap = Bitmap.createBitmap(w, h + topPadding, bmpFormat);
                Canvas canvas = new Canvas(bitmap);
                if (myView != null) {
                    myView.songView.layout(0, topPadding, w, h + topPadding);
                    myView.songView.draw(canvas);
                    Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, topPadding, w, h);
                    bitmap.recycle();
                    mainActivityInterface.getThreadPoolExecutor().execute(() -> {
                        mainActivityInterface.setScreenshotFile(croppedBitmap);
                        croppedBitmap.recycle();
                    });
                }
            } catch (OutOfMemoryError e) {
                Log.d(TAG,"Out of memory:");
                Log.e(TAG, "PerformanceFragment error", e);
                // Change the resolution of the bitmap to a lower option
                bmpFormat = Bitmap.Config.RGB_565;
                mainActivityInterface.setScreenshotFile(null);
                mainActivityInterface.getStorageAccess().updateCrashLog(e.toString());

            } catch (Exception e) {
                Log.e(TAG, "PerformanceFragment error", e);
                mainActivityInterface.setScreenshotFile(null);
                mainActivityInterface.getStorageAccess().updateCrashLog(e.toString());

            }
        }
    }

    // The scale and gesture bits of the code
    @SuppressLint("ClickableViewAccessibility")
    private void setGestureListeners(){
        // get the gesture detector
        if (getContext()!=null) {
            gestureDetector = new GestureDetector(getContext(), new GestureListener(mainActivityInterface,
                    swipeMinimumDistance, swipeMaxDistanceYError, swipeMinimumVelocity));

            // Any interaction with the screen should trigger the display prev/next (if required)
            // It should also show the action bar
            myView.zoomLayout.setOnTouchListener((view, motionEvent) -> {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    mainActivityInterface.getDisplayPrevNext().showAndHide();
                    mainActivityInterface.updateOnScreenInfo("showhide");
                    mainActivityInterface.showActionBar();
                    mainActivityInterface.getHotZones().checkScrollButtonOn(myView.zoomLayout,myView.recyclerView);
                    // nearby removed
                }
                return gestureDetector.onTouchEvent(motionEvent);
            });

            myView.recyclerView.setGestureDetector(gestureDetector);
        }
    }

    public void toggleScale() {
        // IV - View may no longer be valid
        if (myView!=null && myView.recyclerView.getVisibility() == View.VISIBLE) {
            // Resets the zoom
            myView.recyclerView.toggleScale();
        } else {
            if (myView != null) {
                // Toggles between different zooms
                myView.zoomLayout.toggleScale();
            }
        }
    }

    public void doNearbyScrollBy(float proportionScroll) {
        // We received from nearby host, so attempt to scroll by this proportion in the zoomLayout
        if (myView.zoomLayout.getVisibility()==View.VISIBLE &&
                !mainActivityInterface.getPreferences().getMyPreferenceString("songAutoScale","W").equals("Y")) {
            myView.zoomLayout.animateScrollBy(
                    Math.abs(proportionScroll),proportionScroll>0);
        } else if (myView.recyclerView.getVisibility()==View.VISIBLE) {
            int height = (int)(proportionScroll*myView.recyclerView.getHeight());
            myView.recyclerView.smoothScrollBy(0,height);
        }
    }

    public void doNearbyScrollTo(float proportionScroll) {
        // We received from nearby host, so attempt to scroll this position (as ratio of height)
        if (myView.zoomLayout.getVisibility()==View.VISIBLE &&
                !mainActivityInterface.getPreferences().getMyPreferenceString("songAutoScale","W").equals("Y")) {
            myView.zoomLayout.doScrollTo(0,(int)(myView.zoomLayout.getMaxScrollY()*proportionScroll));
        } else if (myView.recyclerView.getVisibility()==View.VISIBLE) {
            int height = (int)(proportionScroll*myView.recyclerView.getHeight());
            myView.recyclerView.scrollTo(0,height);
        }
    }

    public void updateSizes(int width, int height) {
        myView.zoomLayout.setSongSize(width,height);
    }

    // Received from MainActivity after a user clicked on a pdf page or a Stage Mode section
    private boolean alreadyChoosingSections = false;
    public void performanceShowSection(int position) {
        if (!alreadyChoosingSections) {
            alreadyChoosingSections = true;
            // Scroll the recyclerView to the position as long as we aren't in an autoscroll or in hybrid mode
            if (myView != null && recyclerLayoutManager != null) {
                mainActivityInterface.getMode();
            }
        }
        // Allow scroll to section in 500ms
        mainActivityInterface.getMainHandler().postDelayed(() -> alreadyChoosingSections = false, 500);
    }

    // If a nearby host initiated a section change
    public void selectSection(int position) {
        // nearby removed
    }

    // Get the width of the song display (to check it fits)
    public int getSongWidth() {
        if (myView!=null) {
            return myView.songView.getWidth() - myView.songView.getPaddingStart();
        } else {
            return 0;
        }
    }

    // abc removed

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.clear();
        super.onSaveInstanceState(outState);
    }

}
