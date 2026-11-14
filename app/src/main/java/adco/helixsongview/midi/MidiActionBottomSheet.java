package adco.helixsongview.midi;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import adco.helixsongview.R;
import adco.helixsongview.customviews.BottomSheetCommon;
import adco.helixsongview.databinding.BottomSheetMidiActionBinding;
import adco.helixsongview.interfaces.MainActivityInterface;
import adco.helixsongview.preferences.TextInputBottomSheet;

public class MidiActionBottomSheet extends BottomSheetCommon {

    private MainActivityInterface mainActivityInterface;
    private BottomSheetMidiActionBinding myView;
    @SuppressWarnings({"unused","FieldCanBeLocal"})
    private final String TAG = "MidiActionBottomSheet";
    private String website_midi_actions="", nearby_message_string="",currentCode;
    private int on, off, which;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivityInterface = (MainActivityInterface) context;
    }

    @Nullable
    @org.jetbrains.annotations.Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable @org.jetbrains.annotations.Nullable ViewGroup container,
                             @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        myView = BottomSheetMidiActionBinding.inflate(inflater,container,false);

        prepareStrings();

        myView.dialogHeading.setClose(this);
        myView.dialogHeading.setWebHelp(mainActivityInterface, website_midi_actions);

        setWhich(1);
        changeHighlight();

        myView.currentCode.setHint(mainActivityInterface.getMidi().getMidiAction(1));


        return myView.getRoot();
    }

    private void prepareStrings() {
        if (getContext()!=null) {
            website_midi_actions = getString(R.string.website_midi_actions);
            nearby_message_string = getString(R.string.nearby_message);
            // Also set the colours
            on = mainActivityInterface.getPalette().secondary;
            off = mainActivityInterface.getPalette().primaryVariant;
        }
    }



    private void setWhich(int which) {
        this.which = which;
    }

    private void changeHighlight() {
        //ViewCompat.setBackgroundTintList(myView.action1, ColorStateList.valueOf(which==1 ? on:off));
        ViewCompat.setBackgroundTintList(myView.action1, new ColorStateList(
                new int[][]{new int[0]},
                new int[]{which==1 ? on:off}
        ));
        //ViewCompat.setBackgroundTintList(myView.action2, ColorStateList.valueOf(which==2 ? on:off));
        ViewCompat.setBackgroundTintList(myView.action2, new ColorStateList(
                new int[][]{new int[0]},
                new int[]{which==2 ? on:off}
        ));
        //ViewCompat.setBackgroundTintList(myView.action3, ColorStateList.valueOf(which==3 ? on:off));
        ViewCompat.setBackgroundTintList(myView.action3, new ColorStateList(
                new int[][]{new int[0]},
                new int[]{which==3 ? on:off}
        ));
        //ViewCompat.setBackgroundTintList(myView.action4, ColorStateList.valueOf(which==4 ? on:off));
        ViewCompat.setBackgroundTintList(myView.action4, new ColorStateList(
                new int[][]{new int[0]},
                new int[]{which==4 ? on:off}
        ));
        //ViewCompat.setBackgroundTintList(myView.action5, ColorStateList.valueOf(which==5 ? on:off));
        ViewCompat.setBackgroundTintList(myView.action5, new ColorStateList(
                new int[][]{new int[0]},
                new int[]{which==5 ? on:off}
        ));
        //ViewCompat.setBackgroundTintList(myView.action6, ColorStateList.valueOf(which==6 ? on:off));
        ViewCompat.setBackgroundTintList(myView.action6, new ColorStateList(
                new int[][]{new int[0]},
                new int[]{which==6 ? on:off}
        ));
        //ViewCompat.setBackgroundTintList(myView.action7, ColorStateList.valueOf(which==7 ? on:off));
        ViewCompat.setBackgroundTintList(myView.action7, new ColorStateList(
                new int[][]{new int[0]},
                new int[]{which==7 ? on:off}
        ));
        //ViewCompat.setBackgroundTintList(myView.action8, ColorStateList.valueOf(which==8 ? on:off));
        ViewCompat.setBackgroundTintList(myView.action8, new ColorStateList(
                new int[][]{new int[0]},
                new int[]{which==8 ? on:off}
        ));
    }

    private class MyOnClick implements View.OnClickListener {

        int whichButton;
        MyOnClick(int whichButton) {
            this.whichButton = whichButton;
        }

        @Override
        public void onClick(View view) {
            Log.d(TAG,"whichButton:"+whichButton);
            setWhich(whichButton);
            view.post(MidiActionBottomSheet.this::changeHighlight);
            currentCode = mainActivityInterface.getMidi().getMidiAction(whichButton);
            myView.currentCode.setHint(currentCode);
            myView.nearbyMessage.setText(nearby_message_string+" "+whichButton);
        }
    }

    public void updateAction(String newCode) {
        currentCode = newCode;
        // Update the preference
        mainActivityInterface.getMidi().setMidiAction(which,newCode);
        // Update the views
        myView.currentCode.setHint(mainActivityInterface.getMidi().getMidiAction(which));
    }


}
