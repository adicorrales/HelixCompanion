package adco.helixsongview.songprocessing;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import adco.helixsongview.R;
import adco.helixsongview.customviews.ExposedDropDownArrayAdapter;
import adco.helixsongview.databinding.EditSongMainBinding;
import adco.helixsongview.interfaces.EditSongFragmentInterface;
import adco.helixsongview.interfaces.MainActivityInterface;
import adco.helixsongview.preferences.TextInputBottomSheet;


import java.util.ArrayList;

public class EditSongFragmentMain extends Fragment  {

    // The variable used in this fragment
    private EditSongMainBinding myView;
    private MainActivityInterface mainActivityInterface;
    @SuppressWarnings({"unused","FieldCanBeLocal"})
    private final String TAG = "EditSongMain";
    private EditSongFragmentInterface editSongFragmentInterface;
    private String newFolder, new_folder_add_string="", new_folder_string="", new_folder_name_string="";
    private TextInputBottomSheet textInputBottomSheet;
    private ArrayList<String> folders;
    private ExposedDropDownArrayAdapter arrayAdapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivityInterface = (MainActivityInterface) context;
        editSongFragmentInterface = (EditSongFragmentInterface) context;
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivityInterface.getThreadPoolExecutor().execute(() -> {
            prepareStrings();


            // Initialise the views
            setupValues();

            // Set listeners
            setUpListeners();

            if (folders == null || folders.isEmpty()) {
                getFoldersFromStorage();
                mainActivityInterface.getMainHandler().post(this::updateExposedDropdown);
            }
        });
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        myView = EditSongMainBinding.inflate(inflater, container, false);
        return myView.getRoot();
    }

    private void prepareStrings() {
        if (getContext()!=null) {
            new_folder_add_string = getString(R.string.new_folder_add);
            new_folder_string = getString(R.string.new_folder);
            new_folder_name_string = getString(R.string.new_folder_name);
        }
    }

    // Initialise the views
    private void setupValues() {
        if (mainActivityInterface.getTempSong()==null) {
            mainActivityInterface.setTempSong(mainActivityInterface.getSong());
        }
        if (mainActivityInterface.getTempSong()!=null) {
            mainActivityInterface.getMainHandler().post(() -> {
                if (myView!=null) {
                    myView.title.setText(mainActivityInterface.getTempSong().getTitle());
                    myView.author.setText(mainActivityInterface.getTempSong().getAuthor());
                    myView.filename.setText(mainActivityInterface.getTempSong().getFilename());
                }
            });

            newFolder = "+ " + new_folder_add_string;

            textInputBottomSheet = new TextInputBottomSheet(this, "EditSongFragmentMain", new_folder_string, new_folder_name_string, null, "", "", true);

            // Resize the bottom padding to the soft keyboard height or half the screen height for the soft keyboard (workaround)
            mainActivityInterface.getMainHandler().post(() -> mainActivityInterface.getWindowFlags().adjustViewPadding(mainActivityInterface, myView.resizeForKeyboardLayout));
        }
    }

    // Sor the view visibility, listeners, etc.
    private void setUpListeners() {
        mainActivityInterface.getMainHandler().post(() -> {
            if (myView!=null) {
                myView.title.addTextChangedListener(new MyTextWatcher("title"));
                myView.folder.addTextChangedListener(new MyTextWatcher("folder"));
                myView.filename.addTextChangedListener(new MyTextWatcher("filename"));
                myView.author.addTextChangedListener(new MyTextWatcher("author"));
                // Scroll listener
                myView.mainScrollView.setExtendedFabToAnimate(editSongFragmentInterface.getSaveButton());
            }
        });
    }

    private void updateExposedDropdown() {
        folders.add(newFolder);

        if (getContext() != null) {
            arrayAdapter = new ExposedDropDownArrayAdapter(getContext(), myView.folder, R.layout.view_exposed_dropdown_item, folders);
            mainActivityInterface.getMainHandler().post(() -> {
                if (myView != null) {
                    myView.folder.setAdapter(arrayAdapter);
                    myView.folder.setText(mainActivityInterface.getTempSong().getFolder());
                }
            });
        }
    }

    private class MyTextWatcher implements TextWatcher {

        String what;
        String value = "";

        MyTextWatcher(String what) {
            this.what = what;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s != null) {
                value = s.toString();
                if (what.equals("filename")) {
                    value = mainActivityInterface.getStorageAccess().safeFilename(value);
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (what.equals("folder") && value.equals(newFolder) && getActivity()!=null) {
                myView.folder.setText(mainActivityInterface.getTempSong().getFolder());
                showFolderInput();
            } else {
                updateTempSong();
            }
        }

        public void updateTempSong() {
            switch (what) {
                case "folder":
                    if (!what.equals(newFolder)) {
                        mainActivityInterface.getTempSong().setFolder(value);
                    }
                    break;
                case "filename":
                    mainActivityInterface.getTempSong().setFilename(value);
                    break;
                case "title":
                    mainActivityInterface.getTempSong().setTitle(value);
                    break;
                case "copyright":
                    mainActivityInterface.getTempSong().setCopyright(value);
                    break;
                case "author":
                    mainActivityInterface.getTempSong().setAuthor(value);
                    break;
                case "notes":
                    mainActivityInterface.getTempSong().setNotes(value);
                    break;
            }
        }
    }


    public void updateValue(String value) {
        Log.d(TAG, "value:" + value);
        // New folder name given.  If it isn't null/empty try to create it and select it
        if (value != null && !value.isEmpty()) {
            mainActivityInterface.getThreadPoolExecutor().execute(() -> {
                boolean success = mainActivityInterface.getStorageAccess().createFolder("Songs", "", value, true);
                if (success) {
                    mainActivityInterface.getMainHandler().post(() -> {
                        getFoldersFromStorage();
                        newFolder = "+ " + new_folder_add_string;
                        folders.add(newFolder);
                        if (getContext() != null) {
                            arrayAdapter = new ExposedDropDownArrayAdapter(getContext(), myView.folder, R.layout.view_exposed_dropdown_item, folders);
                            myView.folder.setAdapter(arrayAdapter);
                            myView.folder.setText(value);
                        }
                    });
                }
            });
        }
    }

    private void getFoldersFromStorage() {
        ArrayList<String> songIds = mainActivityInterface.getStorageAccess().listSongs(false);
        mainActivityInterface.getStorageAccess().writeSongIDFile(songIds);
        folders = mainActivityInterface.getStorageAccess().getSongFolders(songIds,true,null);
    }

    private void showFolderInput() {
        // Check if the fragment is already there by tag
        Fragment existing = getParentFragmentManager().findFragmentByTag("TextInputBottomSheet");

        if (existing == null) {
            if (textInputBottomSheet == null) {
                textInputBottomSheet = new TextInputBottomSheet();
            }

            // Only show if it's not currently added
            if (!textInputBottomSheet.isAdded()) {
                textInputBottomSheet.show(getParentFragmentManager(), "TextInputBottomSheet");
            }
        }
    }

    public void commitChanges() {
        if (myView != null && mainActivityInterface != null && mainActivityInterface.getTempSong() != null) {
            Song temp = mainActivityInterface.getTempSong();
            if (myView.title != null) temp.setTitle(myView.title.getText().toString());
            if (myView.author != null) temp.setAuthor(myView.author.getText().toString());
            if (myView.filename != null) temp.setFilename(myView.filename.getText().toString());
            if (myView.folder != null) temp.setFolder(myView.folder.getText().toString());
        }
    }

    // Finished with this view
    @Override
    public void onDetach() {
        super.onDetach();
        myView = null;
        mainActivityInterface = null;
        editSongFragmentInterface = null;
    }
}
