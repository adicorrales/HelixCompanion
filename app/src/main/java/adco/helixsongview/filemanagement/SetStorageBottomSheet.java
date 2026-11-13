package adco.helixsongview.filemanagement;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import adco.helixsongview.customviews.BottomSheetCommon;
import adco.helixsongview.databinding.BottomSheetStorageChooseBinding;

public class SetStorageBottomSheet extends BottomSheetCommon {

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        BottomSheetStorageChooseBinding myView = BottomSheetStorageChooseBinding.inflate(inflater, container, false);

        // Initialise the 'close' floatingactionbutton
        myView.dialogHeading.setClose(this);

        return myView.getRoot();
    }
}
