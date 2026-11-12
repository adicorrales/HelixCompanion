package com.garethevans.church.opensongtablet.setmenu;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.garethevans.church.opensongtablet.R;
import com.garethevans.church.opensongtablet.databinding.MenuSetsBinding;
import com.garethevans.church.opensongtablet.interfaces.MainActivityInterface;

public class SelectSetFragment extends Fragment {

    private final String TAG = "SelectSetFragment";
    private MenuSetsBinding myView;
    private LinearLayoutManager llm;
    private MainActivityInterface mainActivityInterface;
    private SelectSetAdapter selectSetAdapter;

    @Override
    public void onAttach(@NonNull Context context) {
        mainActivityInterface = (MainActivityInterface) context;
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (selectSetAdapter != null) {
            selectSetAdapter.prepareSets();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = MenuSetsBinding.inflate(inflater, container, false);
        myView.getRoot().setBackgroundColor(mainActivityInterface.getPalette().background);
        mainActivityInterface.getMyThemeColors().tintProgressBar(myView.progressBar);

        if (getContext() != null) {
            mainActivityInterface = (MainActivityInterface) getContext();
            mainActivityInterface.registerFragment(this, "SelectSetFragment");
        }

        setupAdapter();
        setListeners();

        myView.setTitle.setText("Select Set");
        myView.saveSetButton.setVisibility(View.GONE);

        return myView.getRoot();
    }

    private void setupAdapter() {
        if (getContext() != null && myView != null) {
            selectSetAdapter = new SelectSetAdapter(getContext(), myView.myRecyclerView, this);
            myView.myRecyclerView.post(() -> {
                llm = new LinearLayoutManager(getContext());
                llm.setOrientation(RecyclerView.VERTICAL);
                myView.myRecyclerView.setLayoutManager(llm);
                enableDragAndDrop();
                myView.myRecyclerView.setAdapter(selectSetAdapter);
                myView.myRecyclerView.setItemAnimator(null);
            });
        }
    }

    private void enableDragAndDrop() {
        SelectSetItemCallback callback = new SelectSetItemCallback(getContext(), selectSetAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        selectSetAdapter.setTouchHelper(itemTouchHelper);
        itemTouchHelper.attachToRecyclerView(myView.myRecyclerView);
    }

    private void setListeners() {
        myView.setMasterFAB.post(() -> myView.setMasterFAB.setOnClickListener(v -> {
            if (getActivity() != null) {
                SetMenuBottomSheet setMenuBottomSheet = new SetMenuBottomSheet();
                setMenuBottomSheet.show(getActivity().getSupportFragmentManager(), "setMenuBottomSheet");
            }
        }));
    }

    public void notifySetsChanged() {
        if (selectSetAdapter != null) {
            mainActivityInterface.getMainHandler().post(() -> selectSetAdapter.prepareSets());
        }
    }
}
