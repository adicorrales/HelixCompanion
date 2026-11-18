package adco.helixsongview.screensetup;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import adco.helixsongview.R;
import adco.helixsongview.databinding.SettingsDisplayBinding;
import adco.helixsongview.interfaces.MainActivityInterface;

public class DisplayMenuFragment extends Fragment {

    private MainActivityInterface mainActivityInterface;
    private SettingsDisplayBinding myView;
    private String display_string="", deeplink_inlineset_string="", deeplink_margins_string="";

    @Override
    public void onResume() {
        super.onResume();
        mainActivityInterface.updateToolbar(display_string);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivityInterface = (MainActivityInterface) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myView = SettingsDisplayBinding.inflate(inflater,container,false);

        myView.getRoot().setBackgroundColor(mainActivityInterface.getPalette().background);

        prepareStrings();

        // Set defaults
        setDefaults();

        // Set menu listeners
        setListeners();

        // Check if we need to automatically click an item
        if (mainActivityInterface.getWhattodo().equals("hardwareAcceleration")) {
            mainActivityInterface.setWhattodo("");
            myView.hardwareAcceleration.setChecked(!myView.hardwareAcceleration.getChecked());
        }

        return myView.getRoot();
    }

    private void prepareStrings() {
        if (getContext()!=null) {
            display_string = getString(R.string.display);
            deeplink_inlineset_string = getString(R.string.deeplink_inlineset);
            deeplink_margins_string = getString(R.string.deeplink_margins);
        }
    }
    private void setDefaults() {
        myView.hardwareAcceleration.setChecked(
                mainActivityInterface.getPreferences().getMyPreferenceBoolean("hardwareAcceleration", true));
    }

    private void setListeners() {
        myView.fontButton.setOnClickListener(v -> mainActivityInterface.navigateToFragment(null,R.id.fontSetupFragment));
        myView.extraSettings.setOnClickListener(v -> mainActivityInterface.navigateToFragment(null,R.id.displayExtraFragment));
        myView.actionBarSettings.setOnClickListener(v -> mainActivityInterface.navigateToFragment(null,R.id.actionBarSettingsFragment));
        myView.menuSettings.setOnClickListener(v -> mainActivityInterface.navigateToFragment(null,R.id.menuSettingsFragment));
        myView.scalingButton.setOnClickListener(v -> mainActivityInterface.navigateToFragment(null,R.id.displayScalingFragment));
        myView.inlineSet.setOnClickListener(v -> mainActivityInterface.navigateToFragment(deeplink_inlineset_string,0));
        myView.margins.setOnClickListener(v -> mainActivityInterface.navigateToFragment(deeplink_margins_string,0));
        myView.hardwareAcceleration.setOnCheckedChangeListener((compoundButton, b) -> {
            // Save the preference
            mainActivityInterface.getPreferences().setMyPreferenceBoolean("hardwareAcceleration",b);
            // Update the setting in the main activity
            mainActivityInterface.recreateActivity();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        myView = null;
    }
}
