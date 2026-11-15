package adco.helixsongview.songprocessing;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import adco.helixsongview.R;
import adco.helixsongview.appdata.InformationBottomSheet;
import adco.helixsongview.customviews.MyExtendedFloatingActionButton;
import adco.helixsongview.databinding.EditSongBinding;
import adco.helixsongview.interfaces.EditSongFragmentInterface;
import adco.helixsongview.interfaces.MainActivityInterface;
import com.google.android.material.tabs.TabLayoutMediator;

// When we edit a song, we create a write the current song to tempSong in MainActivity
// We compare the two objects to look for changes and save if requested
// This way changes are temporary until specifically saved

public class EditSongFragment extends Fragment implements EditSongFragmentInterface {

    private EditSongBinding myView;
    private MainActivityInterface mainActivityInterface;
    private ViewPager2.OnPageChangeCallback callback;
    @SuppressWarnings("unused")
    private final String TAG = "EditSongFragment";
    private String edit_string="";
    private String website_edit_song_string="";
    private String information_string="";
    private String okay_string="";
    private String edit_song_variation_temp_string="";
    private String edit_song_variation_string="";
    private String website_edit_song_main_string="";
    private String website_edit_song_features_string="";
    private String lyrics_string="";
    private String mainfoldername_string="";
    private String song_features_string="";
    private String not_saved_filename_string="";
    private String not_saved_folder_string="";
    private String not_saved_string="";
    private String webAddress;

    @Override
    public void onResume() {
        super.onResume();
        mainActivityInterface.updateToolbar(edit_string);
        mainActivityInterface.updateToolbarHelp(webAddress);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivityInterface = (MainActivityInterface) context;
        mainActivityInterface.registerFragment(this,"EditSongFragment");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mainActivityInterface!=null) {
            mainActivityInterface.registerFragment(null, "EditSongFragment");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = EditSongBinding.inflate(inflater, container, false);

        if (myView!=null && mainActivityInterface!=null) {
            myView.getRoot().setBackgroundColor(mainActivityInterface.getPalette().background);
        }

        prepareStrings();
        webAddress = website_edit_song_string;

        if (mainActivityInterface == null && getContext()!=null) {
            try {
                mainActivityInterface = (MainActivityInterface) getContext();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (mainActivityInterface!=null) {
            // Set up the updated song (fully loaded from storage to ensure title and filename match correctly)
            Song songToEdit = new Song(mainActivityInterface.getSong());
            songToEdit = mainActivityInterface.getLoadSong().doLoadSong(songToEdit, false);
            mainActivityInterface.setTempSong(songToEdit);
            mainActivityInterface.getTempSong().setLyricsUndosPos(0);
            mainActivityInterface.getTempSong().setLyricsUndos(0, mainActivityInterface.getTempSong().getLyrics());

            // Initialise views
            setUpTabs();

            // Set the save listener
            myView.saveChanges.setOnClickListener(v -> doSaveChanges());

            // Clear the back stack to the root editSongFragment (otherwise it includes the tabs)
            try {
                mainActivityInterface.popTheBackStack(R.id.editSongFragment, false);
            } catch (Exception e) {
                e.printStackTrace();
            }

            showWarning();
        }

        return myView.getRoot();
    }

    private void showWarning() {
        mainActivityInterface.getThreadPoolExecutor().execute(() -> {
            // If we tried to edit a temporary variation or an actual variation - let the user know
            InformationBottomSheet informationBottomSheet = null;
            if (mainActivityInterface.getWhattodo()!=null &&
                    mainActivityInterface.getWhattodo().equals("editTempVariation")) {
                informationBottomSheet = new InformationBottomSheet(information_string,
                        edit_song_variation_temp_string,okay_string,null);
            } else if (mainActivityInterface.getWhattodo()!=null &&
                    mainActivityInterface.getWhattodo().equals("editActualVariation")) {
                informationBottomSheet = new InformationBottomSheet(information_string,
                        edit_song_variation_string,okay_string,null);
            }

            if (informationBottomSheet!=null) {
                mainActivityInterface.setWhattodo("");
                informationBottomSheet.show(mainActivityInterface.getMyFragmentManager(),"Information");
            }
        });
    }

    private void prepareStrings() {
        if (getContext()!=null) {
            edit_string = getString(R.string.edit);
            website_edit_song_string = getString(R.string.website_edit_song);
            information_string = getString(R.string.information);
            okay_string = getString(R.string.okay);
            edit_song_variation_temp_string = getString(R.string.edit_song_variation_temp);
            edit_song_variation_string = getString(R.string.edit_song_variation);
            website_edit_song_main_string = getString(R.string.website_edit_song_main);
            website_edit_song_features_string = getString(R.string.website_edit_song_features);
            getString(R.string.website_edit_song_tag);
            lyrics_string = getString(R.string.lyrics);
            mainfoldername_string = getString(R.string.mainfoldername);
            song_features_string = getString(R.string.song_features);
            not_saved_filename_string = getString(R.string.not_saved_filename);
            not_saved_folder_string = getString(R.string.not_saved_folder);
            not_saved_string = getString(R.string.not_saved);
        }
    }
    private void setUpTabs() {
        if (getActivity()!=null) {
            mainActivityInterface.getThreadPoolExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    if (getActivity() != null) {
                        EditSongViewPagerAdapter adapter = new EditSongViewPagerAdapter(getActivity().getSupportFragmentManager(),
                                getActivity().getLifecycle());
                        adapter.createFragment(0);

                        mainActivityInterface.enableSwipe("edit", false);

                        callback = new ViewPager2.OnPageChangeCallback() {
                            @Override
                            public void onPageSelected(int position) {
                                super.onPageSelected(position);
                                switch (position) {
                                    case 0:
                                        webAddress = website_edit_song_string;
                                        break;
                                    case 1:
                                        webAddress = website_edit_song_main_string;
                                        break;
                                    case 2:
                                        webAddress = website_edit_song_features_string;
                                        break;
                                }
                                mainActivityInterface.updateToolbarHelp(webAddress);
                                mainActivityInterface.getWindowFlags().hideKeyboard();
                            }
                        };

                        mainActivityInterface.getMainHandler().post(() -> {
                            if (myView!=null) {
                                myView.viewpager.setAdapter(adapter);
                                myView.viewpager.setOffscreenPageLimit(1);
                                myView.viewpager.registerOnPageChangeCallback(callback);
                                myView.tabButtons.setBackgroundColor(mainActivityInterface.getPalette().background);
                                //myView.tabButtons.setTabTextColors(ColorStateList.valueOf(mainActivityInterface.getPalette().textColor));
                                myView.tabButtons.setTabTextColors(new ColorStateList(
                                        new int[][]{new int[0]},
                                        new int[]{mainActivityInterface.getPalette().textColor}
                                ));
                                myView.tabButtons.setSelectedTabIndicatorColor(mainActivityInterface.getPalette().secondary);
                                new TabLayoutMediator(myView.tabButtons, myView.viewpager, (tab, position) -> {
                                    switch (position) {
                                        case 1:
                                            tab.setText(mainfoldername_string);
                                            break;
                                        case 2:
                                            tab.setText(song_features_string);
                                            break;
                                        case 0:
                                        default:
                                            tab.setText(lyrics_string);
                                            break;

                                    }
                                }).attach();
                            }
                        });
                    }
                }
            });
        }
    }

    public void enableSwipe(boolean canSwipe) {
        if (myView!=null) {
            try {
                myView.viewpager.setUserInputEnabled(canSwipe);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean alreadySaving = false;
    private void doSaveChanges() {
        try {
            if (!alreadySaving) {
                alreadySaving = true;
                // Make sure the soft keyboard is closed
                mainActivityInterface.getWindowFlags().hideKeyboard();

                // Force commit any pending text changes from child fragments
                for (Fragment frag : getChildFragmentManager().getFragments()) {
                    if (frag instanceof EditSongFragmentLyrics) {
                        ((EditSongFragmentLyrics) frag).commitChanges();
                    } else if (frag instanceof EditSongFragmentMain) {
                        ((EditSongFragmentMain) frag).commitChanges();
                    }
                }

                // Send this off for processing in a new Thread
                mainActivityInterface.getThreadPoolExecutor().execute(() -> {
                    // If we were editing the lyrics as ChoPro, convert to OpenSong
                    if (mainActivityInterface.getTempSong().getEditingAsChoPro()) {
                        String lyrics = mainActivityInterface.getTempSong().getLyrics();
                        lyrics = mainActivityInterface.getConvertChoPro().fromChordProToOpenSong(lyrics);
                        mainActivityInterface.getTempSong().setLyrics(lyrics);
                        mainActivityInterface.getTempSong().setEditingAsChoPro(false);
                    }

                    // For a new song, check we have a folder/filename set
                    boolean oktoproceed = true;
                    if (mainActivityInterface.getTempSong().getFilename() == null || mainActivityInterface.getTempSong().getFilename().isEmpty()) {
                        mainActivityInterface.getShowToast().doIt(not_saved_filename_string);
                        oktoproceed = false;
                    } else if (mainActivityInterface.getTempSong().getFolder() == null || mainActivityInterface.getTempSong().getFolder().isEmpty()) {
                        mainActivityInterface.getShowToast().doIt(not_saved_folder_string);
                        oktoproceed = false;
                    } else if (mainActivityInterface.getTempSong().getTitle() == null || mainActivityInterface.getTempSong().getTitle().isEmpty()) {
                        // Ok to proceed, but copy the filename into the empty title
                        mainActivityInterface.getTempSong().setTitle(mainActivityInterface.getTempSong().getFilename());
                    }

                    if (oktoproceed && mainActivityInterface.getSaveSong().doSave(mainActivityInterface.getTempSong())) {
                        // If successful, go back to the home page.  Otherwise stay here and await user decision from toast
                        if (mainActivityInterface!=null) {
                            mainActivityInterface.getMainHandler().post(() -> {
                                if (mainActivityInterface != null) {
                                    mainActivityInterface.setForceReload(true);
                                    mainActivityInterface.navHome();
                                }
                                alreadySaving = false;
                            });
                        }
                    } else if (oktoproceed) {
                        if (mainActivityInterface!=null) {
                            mainActivityInterface.getMainHandler().post(() -> {
                                mainActivityInterface.getShowToast().doIt(not_saved_string);
                                alreadySaving = false;
                            });
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void moveSaveButtonUp(boolean moveUp) {
        // If we have our custom text ribbon, we need to move the save button up
        myView.saveChanges.setTranslationY(moveUp ? -48:0);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        myView.viewpager.unregisterOnPageChangeCallback(callback);
        myView = null;
        mainActivityInterface = null;
    }

    public MyExtendedFloatingActionButton getSaveButton() {
        if (myView!=null) {
            return myView.saveChanges;
        } else {
            return null;
        }
    }
}
