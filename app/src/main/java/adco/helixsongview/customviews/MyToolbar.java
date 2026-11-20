package adco.helixsongview.customviews;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.drawable.DrawableCompat;

import adco.helixsongview.MainActivity;
import adco.helixsongview.R;
import adco.helixsongview.interfaces.MainActivityInterface;
import adco.helixsongview.songprocessing.SongDetailsBottomSheet;

import java.util.ArrayList;

// Moved away from MaterialToolbar due to FireHD crashes
public class MyToolbar extends Toolbar {

    // This holds references to the items in the Toolbar (except the battery)
    // Battery changes get sent via the mainactivityInterface
    // The toolbar is set as the supportActionbar received as actionBar, so can called via that as well

    private Context c;
    private MainActivityInterface mainActivityInterface;
    private ActionBar actionBar;
    private final FrameLayout batteryholder;
    private MyMaterialSimpleTextView title;
    private MyMaterialSimpleTextView author;
    private MyMaterialSimpleTextView key;
    private MyMaterialSimpleTextView capo;
    private final TextClock clock;
    private MyMaterialSimpleTextView tempo;
    private ImageView setIcon;
    private final ImageView batteryimage;
    private ImageView btnTransportRt0, playPauseIndicator, btnTransportPrevArrow, btnTransportNextArrow;
    private MyMaterialSimpleTextView btnTransportPrevTitle, btnTransportNextTitle;
    private LinearLayout btnTransportPrevContainer, btnTransportNextContainer, songandauthor, btnTransportPlayContainer;
    private View transportBar;
    private LinearLayout songSourceButtons;
    private TextView songSourceAll, songSourceHelix, songSourceLocal, screenTitle;
    private final MyMaterialSimpleTextView batterycharge;
    private MyMaterialSimpleTextView playPauseStatusText;
    private Handler delayActionBarHide;
    private Runnable hideActionBarRunnable;
    private int actionBarHideTime = 1200, additionalTopPadding = 0;
    private float clockTextSize;
    private boolean clock24hFormat, clockOn, hideActionBar, clockSeconds, performanceMode, actionBarTempo;
    @SuppressWarnings({"unused","FieldCanBeLocal"})
    private final String TAG = "MyToolbar";
    private String capoString = "";
    private String keyString = "";

    // Set up the view and view items
    public void initialiseToolbar(Context c, ActionBar actionBar) {
        this.c = c;
        mainActivityInterface = (MainActivityInterface) c;
        View bottomTransport = mainActivityInterface.getMyActivity().findViewById(R.id.transportBar);
        if (bottomTransport != null) {
            bindTransportBar(bottomTransport);
        }
        songSourceButtons = findViewById(R.id.songSourceButtons);
        songSourceAll = findViewById(R.id.songSourceAll);
        songSourceHelix = findViewById(R.id.songSourceHelix);
        songSourceLocal = findViewById(R.id.songSourceLocal);
        //screenTitle = findViewById(R.id.screenTitle);
        if (songSourceAll != null) songSourceAll.setOnClickListener(view -> mainActivityInterface.openSongMenuPage(0));
        if (songSourceHelix != null) songSourceHelix.setOnClickListener(view -> mainActivityInterface.openSongMenuPage(1));
        if (songSourceLocal != null) songSourceLocal.setOnClickListener(view -> mainActivityInterface.openSongMenuPage(2));
        setSongSourceIcon(songSourceAll, R.drawable.music_note);
        setSongSourceIcon(songSourceHelix, R.drawable.midi);
        setSongSourceIcon(songSourceLocal, R.drawable.inline_set);
        styleSongSourceButtons();
        changeTheme();
        actionBar.setElevation(0);
        this.actionBar = actionBar;
        delayActionBarHide = new Handler();
        /*hideActionBarRunnable = () -> {
            if (actionBar.isShowing() && !mainActivityInterface.needActionBar()) {
                actionBar.hide();
            }
        };*/
        hideActionBarRunnable = () -> {
            if (this.getVisibility() == View.VISIBLE && !mainActivityInterface.needActionBar()) {
                this.setVisibility(View.GONE);
                if (getParent() instanceof View) {
                    ((View) getParent()).setVisibility(View.GONE);
                }
                if (mainActivityInterface instanceof MainActivity) {
                    ((MainActivity) mainActivityInterface).updateMargins();
                }
            }
        };
        updateClock();
    }
    public MyToolbar(@NonNull Context context) {
        this(context, null);
    }

    public MyToolbar(@NonNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs) {
        super(context, attrs);

        View v = inflate(context,R.layout.view_toolbar_constraint,this);
        v.setLayoutParams(new Toolbar.LayoutParams(
            Toolbar.LayoutParams.MATCH_PARENT,
            Toolbar.LayoutParams.MATCH_PARENT
        ));

        this.setContentInsetsRelative(0,0);
        this.setContentInsetStartWithNavigation(0);
        this.setContentInsetEndWithActions(0);

        this.setPadding(0,0,0,0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.setElevation(0f);
        }

        setIcon = v.findViewById(R.id.setIcon);
        title = v.findViewById(R.id.songtitle_ab);
        key = v.findViewById(R.id.songkey_ab);
        capo = v.findViewById(R.id.songcapo_ab);
        //author = v.findViewById(R.id.songauthor_ab);
        tempo = v.findViewById(R.id.songtempo_ab);
        batteryholder = v.findViewById(R.id.batteryholder);
        batteryimage = v.findViewById(R.id.batteryimage);
        batterycharge = v.findViewById(R.id.batterycharge);
        songandauthor = v.findViewById(R.id.songandauthor);
        clock = v.findViewById(R.id.digitalclock);


        btnTransportPrevContainer = v.findViewById(R.id.btnTransportPrevContainer);
        btnTransportNextContainer = v.findViewById(R.id.btnTransportNextContainer);
        btnTransportRt0 = v.findViewById(R.id.btnTransportRt0);
        playPauseIndicator = v.findViewById(R.id.playPauseIndicator);
        playPauseStatusText = v.findViewById(R.id.playPauseStatusText);
        btnTransportPlayContainer = v.findViewById(R.id.btnTransportPlayContainer);
        btnTransportPrevArrow = v.findViewById(R.id.btnTransportPrevArrow);
        btnTransportNextArrow = v.findViewById(R.id.btnTransportNextArrow);
        btnTransportPrevTitle = v.findViewById(R.id.btnTransportPrevTitle);
        btnTransportNextTitle = v.findViewById(R.id.btnTransportNextTitle);

        if (btnTransportPrevContainer != null) {
            btnTransportPrevContainer.setOnClickListener(v1 -> mainActivityInterface.onTransportPrev());
        }
        if (btnTransportRt0 != null) {
            btnTransportRt0.setOnClickListener(v1 -> mainActivityInterface.onTransportRt0());
        }
        if (songandauthor != null) {
            songandauthor.setOnClickListener(v1 -> openDetails());
        }
        if (btnTransportPlayContainer != null) {
            btnTransportPlayContainer.setOnClickListener(v1 -> mainActivityInterface.onTransportPlay());
        }
        if (btnTransportNextContainer != null) {
            btnTransportNextContainer.setOnClickListener(v1 -> mainActivityInterface.onTransportNext());
        }

        batteryholder.setOnClickListener(v1 -> {
            mainActivityInterface.showActionBar();
            mainActivityInterface.navigateToFragment(c.getString(R.string.deeplink_actionbar), 0);
        });
    }

    private void bindTransportBar(View root) {
        transportBar = root;
        setIcon = root.findViewById(R.id.setIcon);
        title = root.findViewById(R.id.songtitle_ab);
        key = root.findViewById(R.id.songkey_ab);
        capo = root.findViewById(R.id.songcapo_ab);
        //author = root.findViewById(R.id.songauthor_ab);
        tempo = root.findViewById(R.id.songtempo_ab);
        songandauthor = root.findViewById(R.id.songandauthor);
        btnTransportPrevContainer = root.findViewById(R.id.btnTransportPrevContainer);
        btnTransportNextContainer = root.findViewById(R.id.btnTransportNextContainer);
        btnTransportRt0 = root.findViewById(R.id.btnTransportRt0);
        playPauseIndicator = root.findViewById(R.id.playPauseIndicator);
        playPauseStatusText = root.findViewById(R.id.playPauseStatusText);
        btnTransportPlayContainer = root.findViewById(R.id.btnTransportPlayContainer);
        btnTransportPrevArrow = root.findViewById(R.id.btnTransportPrevArrow);
        btnTransportNextArrow = root.findViewById(R.id.btnTransportNextArrow);
        btnTransportPrevTitle = root.findViewById(R.id.btnTransportPrevTitle);
        btnTransportNextTitle = root.findViewById(R.id.btnTransportNextTitle);

        btnTransportPrevContainer.setOnClickListener(view -> mainActivityInterface.onTransportPrev());
        btnTransportRt0.setOnClickListener(view -> mainActivityInterface.onTransportRt0());
        songandauthor.setOnClickListener(view -> openDetails());
        btnTransportPlayContainer.setOnClickListener(view -> mainActivityInterface.onTransportPlay());
        btnTransportNextContainer.setOnClickListener(view -> mainActivityInterface.onTransportNext());
    }

    private void styleSongSourceButtons() {
        styleSongSourceButton(songSourceAll);
        styleSongSourceButton(songSourceHelix);
        styleSongSourceButton(songSourceLocal);
    }

    private void setSongSourceIcon(TextView button, int drawableId) {
        if (button == null) return;
        Drawable drawable = AppCompatResources.getDrawable(getContext(), drawableId);
        if (drawable == null) return;
        Drawable icon = DrawableCompat.wrap(drawable.mutate());
        int iconSize = (int) (16 * getResources().getDisplayMetrics().density);
        icon.setBounds(0, 0, iconSize, iconSize);
        DrawableCompat.setTint(icon, android.graphics.Color.parseColor("#20A050"));
        button.setCompoundDrawablePadding((int) (4 * getResources().getDisplayMetrics().density));
        button.setCompoundDrawablesRelative(icon, null, null, null);
    }

    private void styleSongSourceButton(TextView button) {
        if (button == null) return;
        button.setBackgroundResource(R.drawable.transport_btn_bg);
        button.setTextColor(android.graphics.Color.parseColor("#E2E8F0"));
    }

    public void updatePlayButton(boolean isPlaying) {
        if (playPauseIndicator != null && c != null) {
            if (isPlaying) {
                playPauseIndicator.setImageResource(R.drawable.pause);
                playPauseIndicator.setColorFilter(android.graphics.Color.parseColor("#4CAF50"));
            } else {
                playPauseIndicator.setImageResource(R.drawable.play);
                if (mainActivityInterface != null && mainActivityInterface.getPalette() != null) {
                    playPauseIndicator.setColorFilter(mainActivityInterface.getPalette().textColor);
                }
            }
        }
        if (btnTransportPlayContainer != null) {
            btnTransportPlayContainer.setBackgroundResource(R.drawable.transport_btn_bg);
        }
        syncTransportManager();
    }

    // Deal with the preferences used for the actionbar
    private void updateActionBarPrefs() {
        clockTextSize = mainActivityInterface.getPreferences().getMyPreferenceFloat("clockTextSize",9.0f);
        clock24hFormat = mainActivityInterface.getPreferences().getMyPreferenceBoolean("clock24hFormat",true);
        clockOn = mainActivityInterface.getPreferences().getMyPreferenceBoolean("clockOn",true);
        clockSeconds = mainActivityInterface.getPreferences().getMyPreferenceBoolean("clockSeconds",false);
        hideActionBar = mainActivityInterface.getPreferences().getMyPreferenceBoolean("hideActionBar",false);
        actionBarHideTime = mainActivityInterface.getPreferences().getMyPreferenceInt("actionBarHideTime",1200);
        actionBarTempo = mainActivityInterface.getPreferences().getMyPreferenceBoolean("actionBarTempo",false);
    }
    public void updateActionBarSettings(String prefName, float value, boolean isvisible) {

        switch (prefName) {
            case "batteryDialOn":
                mainActivityInterface.getBatteryStatus().setBatteryDialOn(isvisible);
                break;
            case "batteryDialThickness":
                mainActivityInterface.getBatteryStatus().setBatteryDialThickness((int)value);
                mainActivityInterface.getBatteryStatus().setBatteryImage();
                break;
            case "batteryTextOn":
                mainActivityInterface.getBatteryStatus().setBatteryTextOn(isvisible);
                break;
            case "batteryTextSize":
                mainActivityInterface.getBatteryStatus().setBatteryTextSize(value);
                break;
            case "clockOn":
                clockOn = isvisible;
                if (!mainActivityInterface.getSettingsOpen()) {
                    hideView(clock, !isvisible);
                }
                break;
            case "clock24hFormat":
                clock24hFormat = isvisible;
                updateClock();
                break;
            case "clockSeconds":
                clockSeconds = isvisible;
                updateClock();
                break;
            case "clockTextSize":
                clock.setTextSize(value);
                break;
            case "songTitleSize":
                title.setTextSize(value);
                key.setTextSize(value);
                capo.setTextSize(value);
                break;
            case "songAuthorSize":
                author.setTextSize(value);
                break;
            case "hideActionBar":
                setHideActionBar(!isvisible);
                break;
            case "actionBarHideTime":
                actionBarHideTime = (int)value;
                break;
            case "showBatteryHolder":
                // GE The battery holder is now just the clickable area
                // This is called to stop clicks while changing the settings
                batteryholder.setVisibility(isvisible ? View.VISIBLE : View.GONE);
                break;
            case "actionBarTempo":
                actionBarTempo = isvisible;
                break;
            case "actionBarSettingsMinimise":
                Log.d(TAG,"minimise actionbar icons toggled");
                break;
        }
    }

    public void setHideActionBar(boolean hideActionBar) {
        this.hideActionBar = hideActionBar;
    }
    public boolean getHideActionBar() {
        return hideActionBar;
    }

    // Update the text in the actionbar to either be a song info, or menu title
    public void setActionBar(Context c, String newtitle) {
        if (mainActivityInterface==null) {
            mainActivityInterface = (MainActivityInterface) c;
        }
        updateSurfaceMode(newtitle);
        // If changing, reset help
        if (title!=null && !title.getText().equals(newtitle)) {
            // By default hide help (can be shown later)
            mainActivityInterface.updateToolbarHelp("");
        }

        if (newtitle == null) {
            // We are in the Performance/Stage mode
            hideView(btnTransportPrevContainer, false);
            hideView(btnTransportRt0, false);
            hideView(btnTransportNextContainer, false);
            hideView(btnTransportPlayContainer, !performanceMode);
            updatePrevNextTitles();

            float mainsize = mainActivityInterface.getPreferences().getMyPreferenceFloat("songTitleSize",15.0f);

            if (title != null && mainActivityInterface.getSong().getTitle() != null) {
                // Songs have the gravity bottom for the content
                title.setGravity(Gravity.BOTTOM);
                title.setTextSize(mainsize);
                String text = mainActivityInterface.getSong().getTitle();
                if (mainActivityInterface.getVariations().getIsNormalOrKeyVariation(
                        mainActivityInterface.getSong())) {
                    text = "*" + mainActivityInterface.getVariations().getPreVariationInfo(
                            mainActivityInterface.getSong().getFolder(),mainActivityInterface.getSong().getFilename(),mainActivityInterface.getSong().getKey())[1];
                }
                title.setText(text);
                mainActivityInterface.checkSetMenuItemHighlighted(mainActivityInterface.getCurrentSet().getIndexSongInSet());
                int positionInSet = mainActivityInterface.getCurrentSet().getIndexSongInSet();
                // If we are in a set, show the icon
                if (positionInSet<0) {
                    try {
                        positionInSet = mainActivityInterface.getCurrentSet().getIndexSongInSet();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (positionInSet>-1) {
                    // Check the set menu fragment to see if we need to highlight
                    // This happens if we just loaded a song (not clicking on the set item in the menu)
                    mainActivityInterface.checkSetMenuItemHighlighted(positionInSet);
                    mainActivityInterface.getCurrentSet().setIndexSongInSet(positionInSet);
                    hideView(setIcon, text.isEmpty());
                } else {
                    mainActivityInterface.getCurrentSet().setIndexSongInSet(-1);
                    hideView(setIcon,true);
                }
                hideView(title, false);
            } else {
                hideView(setIcon,true);
                hideView(title, true);
            }
            if (author != null && mainActivityInterface.getSong().getAuthor() != null &&
                    !mainActivityInterface.getSong().getAuthor().isEmpty()) {
                author.setTextSize(mainActivityInterface.getPreferences().getMyPreferenceFloat("songAuthorSize",12.0f));
                author.setText(mainActivityInterface.getSong().getAuthor());
                hideView(author, false);
                if (title!=null) {
                    title.setGravity(Gravity.BOTTOM|Gravity.START);
                    key.setGravity(Gravity.BOTTOM|Gravity.START);
                    capo.setGravity(Gravity.BOTTOM|Gravity.START);
                    tempo.setGravity(Gravity.BOTTOM|Gravity.START);
                    setIcon.setScaleType(ImageView.ScaleType.FIT_END);
                }
            } else if (author!=null) {
                hideView(author, true);
                if (title!=null) {
                    title.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                    key.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
                    capo.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
                    tempo.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
                    setIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                }
            }
            if (key != null && mainActivityInterface.getSong().getKey() != null &&
                    !mainActivityInterface.getSong().getKey().isEmpty()) {
                keyString = " (" + mainActivityInterface.getSong().getKey() + ")";
                key.setTextSize(Math.max(11f, mainsize - 2f));
                capo.setTextSize(Math.max(11f, mainsize - 2f));
                key.setText(keyString);
                hideView(key, false);
            } else if (key!=null) {
                hideView(key, true);
            }
            if (capo != null && mainActivityInterface.getSong().getCapo() !=null &&
                    !mainActivityInterface.getSong().getCapo().isEmpty()) {
                capoString = mainActivityInterface.getChordDisplayProcessing().getCapoPosition();
            } else {
                capoString = "";
            }

            if (!capoString.isEmpty() && !keyString.isEmpty()) {
                capoString += (" (" + mainActivityInterface.getTranspose().capoKeyTranspose(mainActivityInterface.getSong()) + ")").replace(" ()","");
            }
            String thisCapoString = "";
            if (!capoString.isEmpty()) {
                thisCapoString = " ["+capoString+"]";
            }
            if (thisCapoString.isEmpty() && capo!=null) {
                hideView(capo,true);
            } else if (capo!=null) {
                capo.setText(thisCapoString);
                hideView(capo,false);
            }

            if (actionBarTempo && tempo!=null && mainActivityInterface.getSong().getTempo() != null &&
                !mainActivityInterface.getSong().getTempo().isEmpty()) {
                String tempoString = " " + mainActivityInterface.getSong().getTempo() + " bpm";
                tempo.setTextSize(Math.max(11f, mainsize - 3f));
                tempo.setText(tempoString);
                hideView(tempo, false);
            } else if (tempo!=null) {
                tempo.setText("");
                hideView(tempo, true);
            }

            if (title!=null) {
                title.setOnClickListener(v -> openDetails());
                title.setOnLongClickListener(view -> {
                    editSong();
                    return true;
                });
            }
            if (author!=null) {
                author.setOnClickListener(v -> openDetails());
                author.setOnLongClickListener(view -> {
                    editSong();
                    return true;
                });
            }
            if (key!=null) {
                key.setOnClickListener(v -> openDetails());
                key.setOnLongClickListener(view -> {
                    editSong();
                    return true;
                });
            }
            if (songandauthor!=null) {
                songandauthor.setOnClickListener(v -> openDetails());
                songandauthor.setOnLongClickListener(view -> {
                    editSong();
                    return true;
                });
            }

        } else {
            // We are in a different fragment, so hide the song info stuff
            hideView(setIcon,true);
            if (title != null) {
                title.setOnClickListener(null);
                title.setOnLongClickListener(null);
                // Settings titles move to center gravity
                title.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
                title.setTextSize(18.0f);
                title.setText(newtitle);
            }
            hideView(title, false);
            hideView(author, true);
            hideView(key, true);
            hideView(capo,true);
            hideView(tempo,true);
            hideView(btnTransportPrevContainer, true);
            hideView(btnTransportRt0, true);
            hideView(btnTransportNextContainer, true);
            hideView(btnTransportPlayContainer, true);
            if (songandauthor!=null) {
                songandauthor.setOnClickListener(null);
                songandauthor.setOnLongClickListener(null);
            }
        }
        syncTransportManager();
    }

    private void updateSurfaceMode(String newtitle) {
        boolean isPerformanceScreen = newtitle == null && performanceMode;
        if (transportBar != null) {
            int visibility = isPerformanceScreen ? View.VISIBLE : View.GONE;
            if (transportBar.getVisibility() != visibility) {
                transportBar.setVisibility(visibility);
                mainActivityInterface.updateMargins();
            }
        }
        if (songSourceButtons != null) {
            songSourceButtons.setVisibility(newtitle == null ? View.VISIBLE : View.GONE);
        }
//        if (screenTitle != null) {
//            screenTitle.setText(newtitle == null ? "" : newtitle);
//            screenTitle.setVisibility(newtitle == null ? View.GONE : View.VISIBLE);
//        }
        styleSongSourceButtons();
    }

    public void updatePrevNextTitles() {
        if (mainActivityInterface == null) return;
        try {
            int setPosition = mainActivityInterface.getCurrentSet().getIndexSongInSet();
            if (setPosition < 0) {
                setPosition = mainActivityInterface.getSetActions().indexSongInSet(mainActivityInterface.getSong());
            }

            if (setPosition < 0) {
                if (btnTransportPrevContainer != null) {
                    btnTransportPrevContainer.setVisibility(View.GONE);
                }
                if (btnTransportNextContainer != null) {
                    btnTransportNextContainer.setVisibility(View.GONE);
                }
                return;
            }

            int prevIndex = -1;
            int currentPos = setPosition;
            while (currentPos > 0) {
                currentPos--;
                String songfolder = mainActivityInterface.getCurrentSet().getSetItemInfo(currentPos).songfolder;
                if (!songfolder.contains("Divider") && !songfolder.contains("divider")) {
                    prevIndex = currentPos;
                    break;
                }
            }

            int nextIndex = -1;
            currentPos = setPosition;
            while (currentPos < mainActivityInterface.getCurrentSet().getCurrentSetSize() - 1) {
                currentPos++;
                String songfolder = mainActivityInterface.getCurrentSet().getSetItemInfo(currentPos).songfolder;
                if (!songfolder.contains("Divider") && !songfolder.contains("divider")) {
                    nextIndex = currentPos;
                    break;
                }
            }

            String prevTitle = "";
            if (prevIndex >= 0 && prevIndex < mainActivityInterface.getCurrentSet().getCurrentSetSize()) {
                prevTitle = mainActivityInterface.getCurrentSet().getSetItemInfo(prevIndex).songtitle;
                if (prevTitle == null || prevTitle.isEmpty()) {
                    prevTitle = mainActivityInterface.getCurrentSet().getSetItemInfo(prevIndex).songfilename;
                }
            }

            String nextTitle = "";
            if (nextIndex >= 0 && nextIndex < mainActivityInterface.getCurrentSet().getCurrentSetSize()) {
                nextTitle = mainActivityInterface.getCurrentSet().getSetItemInfo(nextIndex).songtitle;
                if (nextTitle == null || nextTitle.isEmpty()) {
                    nextTitle = mainActivityInterface.getCurrentSet().getSetItemInfo(nextIndex).songfilename;
                }
            }

            MyMaterialSimpleTextView prevTv = btnTransportPrevTitle;
            MyMaterialSimpleTextView nextTv = btnTransportNextTitle;

            if (prevTv != null) {
                prevTv.setText(prevTitle);
            }
            if (btnTransportPrevContainer != null) {
                btnTransportPrevContainer.setVisibility(prevTitle.isEmpty() ? View.GONE : View.VISIBLE);
            }
            if (nextTv != null) {
                nextTv.setText(nextTitle);
            }
            if (btnTransportNextContainer != null) {
                btnTransportNextContainer.setVisibility(nextTitle.isEmpty() ? View.GONE : View.VISIBLE);
            }
            syncTransportManager();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // The onscreen capo display can grab this rather than reprocessing it
    public String getCapoString() {
        return capoString;
    }

    // Clicking on the song title/author/etc. opens up the song details bottom sheet
    private void openDetails() {
        if (mainActivityInterface.getSong()!=null && mainActivityInterface.getSong().getTitle()!=null &&
                !mainActivityInterface.getSong().getTitle().equals("Welcome to OpenSongApp")) {
            SongDetailsBottomSheet songDetailsBottomSheet = new SongDetailsBottomSheet();
            songDetailsBottomSheet.show(mainActivityInterface.getMyFragmentManager(), "songDetailsBottomSheet");
        }
    }

    // Allow editing the song by long pressing on the title/author/etc. if not null
    private void editSong() {
        if (mainActivityInterface.getSong()!=null && mainActivityInterface.getSong().getTitle()!=null &&
                !mainActivityInterface.getSong().getTitle().equals("Welcome to OpenSongApp")) {
            mainActivityInterface.navigateToFragment(c.getString(R.string.deeplink_edit), 0);
        }
    }

    // This is used to show/hide various parts of the toolbar text (author, copyright, etc)
    public void hideView(View v, boolean hide) {
        if (v!=null) {
            v.setVisibility(hide ? View.GONE : View.VISIBLE);
            v.requestLayout();
        }
    }

    public void hideSongDetails(boolean hide) {
        hideView(setIcon,hide);
        hideView(title,hide);
        hideView(author,hide);
        hideView(key,hide);
        hideView(capo,hide);
    }

    // Set when entering/exiting performance mode as this is used to determine if we can autohide actionbar
    public void setPerformanceMode(boolean inPerformanceMode) {
        performanceMode = inPerformanceMode;
    }

    // Show/hide the actionbar
    /*public void showActionBar(boolean menuOpen) {
        // Remove any existing callbacks to hide the actionbar
        try {
            if (delayActionBarHide!=null) {
                delayActionBarHide.removeCallbacks(hideActionBarRunnable);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Now show the action bar
        if (actionBar!=null) {

            mainActivityInterface.getMainHandler().post(() -> {
                try {
                    actionBar.show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // If we need to hide the actionbar again, set a runnable, as long as the menu isn't open
        if (hideActionBar && performanceMode && !menuOpen) {
            try {
                if (delayActionBarHide!=null) {
                    delayActionBarHide.postDelayed(hideActionBarRunnable, actionBarHideTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
*/
    public void showActionBar(boolean menuOpen) {
        try {
            if (delayActionBarHide != null) {
                delayActionBarHide.removeCallbacks(hideActionBarRunnable);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mainActivityInterface.getMainHandler().post(() -> {
            this.setVisibility(View.VISIBLE);
            // Ensure the parent layout container becomes visible too!
            if (getParent() instanceof View) {
                ((View) getParent()).setVisibility(View.VISIBLE);
            }
            if (mainActivityInterface instanceof MainActivity) {
                // Force layout update so getBottom() calculates correctly next pass
                ((MainActivity) mainActivityInterface).updateMargins();
            }
        });

        if (hideActionBar && performanceMode && !menuOpen) {
            try {
                if (delayActionBarHide != null) {
                    delayActionBarHide.postDelayed(hideActionBarRunnable, actionBarHideTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void removeCallBacks() {
        delayActionBarHide.removeCallbacks(hideActionBarRunnable);
    }


    // Get the actionbar height - fakes a height of 0 if autohiding
    /*public int getActionBarHeight(boolean forceShown) {

        if (hideActionBar && performanceMode && !forceShown) {
            return mainActivityInterface.getWindowFlags().getCustomMarginTop();
        } else {
            if (getHeight()==0) {
                // Just in case there was a call before drawn
                return 168;
            } else {
                return getHeight();
            }
        }
    }*/
    public int getActionBarHeight(boolean forceShown) {
        if (hideActionBar && performanceMode && !forceShown) {
            return 0; // Underlying view fills the screen when hidden
        } else {
            int height = getHeight();
            if (height <= 0) {
                // Fallback to a safe default standard height (~56dp converted to pixels)
                return (int) (56 * getResources().getDisplayMetrics().density);
            }
            return height;
        }
    }


    public void setAdditionalTopPadding(int additionalTopPadding) {
        this.additionalTopPadding = additionalTopPadding;
    }
    public int getAdditionalTopPadding() {
        return additionalTopPadding;
    }

    public void updateClock() {
        // IV - Refresh from preferences as may have changed
        updateActionBarPrefs();
        mainActivityInterface.getTimeTools().setFormat(clock, mainActivityInterface.getSettingsOpen(),
                clockTextSize, clockOn, clock24hFormat, clockSeconds);
    }

    public void batteryholderVisibility(boolean visible, boolean clickable) {
        batteryholder.setVisibility(visible ? View.VISIBLE:View.GONE);
        batteryholder.setClickable(clickable);
        showClock(visible);
    }

    public FrameLayout getBatteryholder() {
        return batteryholder;
    }
    public ImageView getBatteryimage() {
        return batteryimage;
    }
    public TextClock getClock() {
        return clock;
    }

    public MyMaterialSimpleTextView getBatterycharge() {
        return batterycharge;
    }

    public void showClock(boolean show) {
        // Only show if that is our preference
        clock.setTextSize(clockTextSize);
        clock.setVisibility(show && clockOn ? View.VISIBLE:View.GONE);
        clock.requestLayout();
    }

    @Override
    public void setNavigationOnClickListener(OnClickListener listener) {
        super.setNavigationOnClickListener(listener);
    }

    @Override
    public void setOnMenuItemClickListener(OnMenuItemClickListener listener) {
        super.setOnMenuItemClickListener(listener);
    }

    public boolean isHidden() {
        return isShown();
    }



    public void changeTheme() {
        this.setTitleTextColor(mainActivityInterface.getPalette().textColor);
        this.setSubtitleTextColor(mainActivityInterface.getPalette().textColor);
        this.setBackgroundColor(android.graphics.Color.parseColor("#192724"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.setElevation(0);
        }
        title.setTextColor(mainActivityInterface.getPalette().textColor);
        //author.setTextColor(mainActivityInterface.getPalette().textColor);
        key.setTextColor(mainActivityInterface.getPalette().textColor);
        capo.setTextColor(mainActivityInterface.getPalette().textColor);
        tempo.setTextColor(mainActivityInterface.getPalette().textColor);
        clock.setTextColor(mainActivityInterface.getPalette().textColor);
        int setIconColor = android.graphics.Color.parseColor("#4CAF50");
        if (mainActivityInterface == null || !mainActivityInterface.hasValidMidiConnection()) {
            setIconColor = mainActivityInterface != null && mainActivityInterface.getPalette() != null ? mainActivityInterface.getPalette().textColor : android.graphics.Color.WHITE;
        }
        setIcon.setColorFilter(setIconColor);
        if (btnTransportPrevArrow != null && mainActivityInterface.getPalette() != null) {
            btnTransportPrevArrow.setColorFilter(mainActivityInterface.getPalette().textColor);
        }
        if (btnTransportNextArrow != null && mainActivityInterface.getPalette() != null) {
            btnTransportNextArrow.setColorFilter(mainActivityInterface.getPalette().textColor);
        }
        if (btnTransportRt0 != null && mainActivityInterface.getPalette() != null) {
            btnTransportRt0.setColorFilter(mainActivityInterface.getPalette().textColor);
        }
        if (playPauseIndicator != null && mainActivityInterface.getPalette() != null) {
            playPauseIndicator.setColorFilter(mainActivityInterface.getPalette().textColor);
        }
        if (btnTransportPrevTitle != null && mainActivityInterface.getPalette() != null) {
            btnTransportPrevTitle.setTextColor(mainActivityInterface.getPalette().textColor);
        }
        if (btnTransportNextTitle != null && mainActivityInterface.getPalette() != null) {
            btnTransportNextTitle.setTextColor(mainActivityInterface.getPalette().textColor);
        }

        mainActivityInterface.refreshToolbarMenu();
    }

    public void setPlayStatus(String statusText, int color) {
        if (color == 0 && mainActivityInterface != null && mainActivityInterface.getPalette() != null) {
            color = mainActivityInterface.getPalette().textColor;
        }
        if (playPauseStatusText != null) {
            if (statusText == null || statusText.isEmpty()) {
                playPauseStatusText.setVisibility(View.GONE);
            } else {
                playPauseStatusText.setVisibility(View.VISIBLE);
                playPauseStatusText.setText(statusText);
                playPauseStatusText.setTextColor(color);
            }
        }
        if (playPauseIndicator != null) {
            playPauseIndicator.setColorFilter(color);
        }
        syncTransportManager();
    }

    private void syncTransportManager() {
        if (mainActivityInterface instanceof MainActivity) {
            ((MainActivity) mainActivityInterface).updateTransportManagerSync();
        }
    }

    public TextView getPlayPauseStatusText() {
        return playPauseStatusText;
    }
    public String getSongTitleText() {
        return title != null && title.getText() != null ? title.getText().toString() : "";
    }
    public String getKeyText() {
        return key != null && key.getText() != null ? key.getText().toString() : "";
    }
    public String getCapoText() {
        return capo != null && capo.getText() != null ? capo.getText().toString() : "";
    }
    public String getTempoText() {
        return tempo != null && tempo.getText() != null ? tempo.getText().toString() : "";
    }
    public String getPrevTitleText() {
        return btnTransportPrevTitle != null && btnTransportPrevTitle.getText() != null ? btnTransportPrevTitle.getText().toString() : "";
    }
    public String getNextTitleText() {
        return btnTransportNextTitle != null && btnTransportNextTitle.getText() != null ? btnTransportNextTitle.getText().toString() : "";
    }

}
