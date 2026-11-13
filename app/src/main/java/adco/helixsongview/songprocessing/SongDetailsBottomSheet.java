package adco.helixsongview.songprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import adco.helixsongview.R;
import adco.helixsongview.customviews.BottomSheetCommon;
import adco.helixsongview.databinding.BottomSheetSongDetailsBinding;
import adco.helixsongview.interfaces.MainActivityInterface;

public class SongDetailsBottomSheet extends BottomSheetCommon {

    private BottomSheetSongDetailsBinding myView;
    private MainActivityInterface mainActivityInterface;
    private String not_available_string="";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mainActivityInterface = (MainActivityInterface) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myView = BottomSheetSongDetailsBinding.inflate(inflater, container, false);

        myView.dialogHeading.setText(getString(R.string.information));
        myView.dialogHeading.setClose(this);

        // Tint the progressBar as the secondary color
        mainActivityInterface.getMyThemeColors().tintProgressBar(myView.progressBar);

        // Set up views
        setupViews();

        // Set up listeners
        setupListeners();

        return myView.getRoot();
    }

    private void setupViews() {
        if (getContext()!=null) {
            not_available_string = getString(R.string.not_allowed);
        }
        myView.songSheet.removeAllViews();
        LinearLayout linearLayout = mainActivityInterface.getSongSheetHeaders().getSongSheet(mainActivityInterface.getSong(),0.9f,getResources().getColor(R.color.vlightgrey));
        if (linearLayout!=null) {
            myView.songSheet.addView(mainActivityInterface.getSongSheetHeaders().getSongSheet(
                    mainActivityInterface.getSong(), 0.9f, getResources().getColor(R.color.vlightgrey)));
        }
        String val;
        val = mainActivityInterface.getSong().getCcli();
        if (val==null || val.isEmpty()) {
            myView.ccli.setVisibility(View.GONE);
        } else {
            myView.ccli.setHint(val);
        }
        val = mainActivityInterface.getSong().getPresentationorder();
        if (val==null || val.isEmpty()) {
            myView.presentationOrder.setVisibility(View.GONE);
        } else {
            myView.presentationOrder.setHint(val);
        }
        val = mainActivityInterface.getSong().getHymnnum();
        if (val==null || val.isEmpty()) {
            myView.hymnnum.setVisibility(View.GONE);
        } else {
            myView.hymnnum.setHint(val);
        }
        val = mainActivityInterface.getSong().getNotes();
        if (val==null || val.isEmpty()) {
            myView.notes.setVisibility(View.GONE);
        } else {
            myView.notes.setHint(val);
        }
        if (getContext()!=null) {
            myView.lyrics.setHintMonospace(getContext());
        }
        myView.lyrics.setHint(mainActivityInterface.getSong().getLyrics());

        if (mainActivityInterface.getSong().getFiletype()==null) {
            mainActivityInterface.getSong().setFiletype(mainActivityInterface.getStorageAccess().tryToFixFileTypeFromNull(mainActivityInterface.getSong().getFilename()));
        }

        if ((mainActivityInterface.getSong()!=null && mainActivityInterface.getSong().getFiletype()!=null) &&
                ((mainActivityInterface.getSong().getFiletype().equals("PDF") && Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) ||
                mainActivityInterface.getSong().getFiletype().equals("IMG"))) {
            myView.textExtract.setVisibility(View.VISIBLE);
        } else {
            myView.textExtract.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        myView.textExtract.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.LOLLIPOP) {

            } else {
                mainActivityInterface.getShowToast().doIt(not_available_string);
            }
        });
        myView.edit.setOnClickListener(v -> {
            mainActivityInterface.navigateToFragment(getString(R.string.deeplink_edit),0);
            dismiss();
        });
    }

}
