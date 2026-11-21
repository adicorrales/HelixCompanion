package adco.helixsongview.preferences;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import adco.helixcompanion.midi.TransportManager;
import adco.helixsongview.R;
import adco.helixsongview.customviews.ExposedDropDownArrayAdapter;
import adco.helixsongview.databinding.SettingsHelixBinding;
import adco.helixsongview.interfaces.MainActivityInterface;

public class HelixSettingsFragment extends Fragment {

    private SettingsHelixBinding myView;
    private MainActivityInterface mainActivityInterface;

    @Override
    public void onAttach(@NonNull Context context) {
        mainActivityInterface = (MainActivityInterface) context;
        super.onAttach(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        myView = SettingsHelixBinding.inflate(inflater, container, false);
        myView.getRoot().setBackgroundColor(mainActivityInterface.getPalette().background);

        if (getContext() != null) {
            mainActivityInterface = (MainActivityInterface) getContext();
            mainActivityInterface.registerFragment(this, "HelixSettingsFragment");
        }

        setupViews();
        return myView.getRoot();
    }

    private void setupViews() {
        if (getContext() == null || myView == null) return;

        String[] channels = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"};

        // Song Mode: Cue / Skip
        ExposedDropDownArrayAdapter songModeAdapter = new ExposedDropDownArrayAdapter(
                getContext(), myView.helixSongMode, R.layout.view_exposed_dropdown_item, new String[]{"Cue", "Skip"}
        );
        myView.helixSongMode.setAdapter(songModeAdapter);
        myView.helixSongMode.setText(TransportManager.currentSongMode(), false);
        myView.helixSongMode.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                TransportManager.setSongMode(s.toString());
            }
        });

        // Play Mode: Play / Load
        ExposedDropDownArrayAdapter playModeAdapter = new ExposedDropDownArrayAdapter(
                getContext(), myView.helixPlayMode, R.layout.view_exposed_dropdown_item, new String[]{"Play", "Load"}
        );
        myView.helixPlayMode.setAdapter(playModeAdapter);
        myView.helixPlayMode.setText(TransportManager.currentPlayMode(), false);
        myView.helixPlayMode.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                TransportManager.setPlayMode(s.toString());
            }
        });

        // Global MIDI Channel (default 1). Owned by TransportManager so the Song
        // View and the Matrix mixer always read the same value.
        ExposedDropDownArrayAdapter channelAdapter = new ExposedDropDownArrayAdapter(
                getContext(), myView.helixGlobalMidiChannel, R.layout.view_exposed_dropdown_item, channels
        );
        myView.helixGlobalMidiChannel.setAdapter(channelAdapter);
        myView.helixGlobalMidiChannel.setText(String.valueOf(TransportManager.currentChannel()), false);
        myView.helixGlobalMidiChannel.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    TransportManager.setChannel(Integer.parseInt(s.toString()));
                } catch (NumberFormatException ignored) {
                    // Not a channel selection; leave the stored value alone.
                }
            }
        });

        // Bypass / Ctrl (default 2)
        setupChannelDropdown(myView.helixChannelBypassCtrl, "helixChannelBypassCtrl", "2", channels);
        // Matrix 1/4" (default 3)
        setupChannelDropdown(myView.helixChannelMatrixQuarter, "helixChannelMatrixQuarter", "3", channels);
        // Matrix XLR (default 4)
        setupChannelDropdown(myView.helixChannelMatrixXlr, "helixChannelMatrixXlr", "4", channels);
        // Matrix Phones (default 5)
        setupChannelDropdown(myView.helixChannelMatrixPhones, "helixChannelMatrixPhones", "5", channels);
    }

    private void setupChannelDropdown(adco.helixsongview.customviews.ExposedDropDown dropdown, String prefKey, String defaultVal, String[] channels) {
        ExposedDropDownArrayAdapter adapter = new ExposedDropDownArrayAdapter(
                getContext(), dropdown, R.layout.view_exposed_dropdown_item, channels
        );
        dropdown.setAdapter(adapter);
        dropdown.setText(mainActivityInterface.getPreferences().getMyPreferenceString(prefKey, defaultVal), false);
        dropdown.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                mainActivityInterface.getPreferences().setMyPreferenceString(prefKey, s.toString());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        myView = null;
    }
}
