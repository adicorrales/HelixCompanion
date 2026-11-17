package adco.helixsongview.setmenu;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import adco.helixsongview.R;
import adco.helixsongview.databinding.MenuSetsBinding;
import adco.helixsongview.interfaces.MainActivityInterface;
import adco.helixsongview.preferences.AreYouSureBottomSheet;
import com.google.android.material.snackbar.Snackbar;

public class SetMenuFragment extends Fragment {

    @SuppressWarnings({"unused","FieldCanBeLocal"})
    private final String TAG = "SetMenuFragment";
    protected MenuSetsBinding myView;
    protected LinearLayoutManager llm;
    private String deeplink_sets_manage_string="", save_changes_string="", overwrite_string="",
            set_manage_click_string="", set_help_string="", set_manage_swipe_string="",
            set_item_removed_string="", undo_string="";
    protected MainActivityInterface mainActivityInterface;
    protected SetAdapter setAdapter;
    protected SelectSetAdapter selectSetAdapter;

    protected boolean isHelixSetMode() {
        return true;
    }

    protected String getFragmentName() {
        return "SetMenuFragment";
    }

    @Override
    public void onAttach(@NonNull Context context) {
        mainActivityInterface = (MainActivityInterface) context;
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        prepareStrings();
        setListeners();
        runSetShowcase();
        bindSetTitleViews();
        if (selectSetAdapter != null) {
            selectSetAdapter.prepareSets();
        }
        if (setAdapter != null) {
            setAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        myView = MenuSetsBinding.inflate(inflater, container, false);

        myView.getRoot().setBackgroundColor(mainActivityInterface.getPalette().background);

        // Tint the progressBar as the secondary color
        mainActivityInterface.getMyThemeColors().tintProgressBar(myView.progressBar);

        if (getContext()!=null) {
            mainActivityInterface = (MainActivityInterface) getContext();
            mainActivityInterface.registerFragment(this, getFragmentName());
        }

        // Set column header label according to mode
        if (isHelixSetMode()) {
            myView.setsColumnHeader.setText(getString(R.string.helix_sets));
        } else {
            myView.setsColumnHeader.setText(getString(R.string.local_sets));
        }

        // Prepare the strings
        prepareStrings();

        // Set up the adapters for left and right columns
        setupAdapters();

        // Set up listeners
        setListeners();

        // Initialise the set title by passing reference to the views
        bindSetTitleViews();

        return myView.getRoot();
    }

    public void bindSetTitleViews() {
        if (myView != null && mainActivityInterface != null && mainActivityInterface.getCurrentSet() != null) {
            if (mainActivityInterface.isHelixSet() == isHelixSetMode()) {
                mainActivityInterface.getCurrentSet().initialiseSetTitleViews(myView.setTitle.getImageView(), myView.setTitle, myView.saveSetButton);
            }
        }
    }

    private void prepareStrings() {
        if (getContext()!=null) {
            deeplink_sets_manage_string = getString(R.string.deeplink_sets_manage);
            save_changes_string = getString(R.string.save_changes);
            overwrite_string = getString(R.string.overwrite);
            set_manage_click_string = getString(R.string.set_manage_click);
            set_help_string = getString(R.string.set_help);
            set_manage_swipe_string = getString(R.string.set_manage_swipe);
            set_item_removed_string = getString(R.string.set_item_removed);
            undo_string = getString(R.string.undo);
        }
    }

    private void setupAdapters() {
        if (getContext()!=null && myView!=null) {
            selectSetAdapter = new SelectSetAdapter(getContext(), myView.setsRecyclerView, isHelixSetMode(), (position, filename) -> {
                if (setAdapter != null) {
                    setAdapter.notifyDataSetChanged();
                    updateKeys();
                    updateHighlight();
                }
            });

            myView.setsRecyclerView.post(() -> {
                LinearLayoutManager llmSets = new LinearLayoutManager(getContext());
                llmSets.setOrientation(RecyclerView.VERTICAL);
                myView.setsRecyclerView.setLayoutManager(llmSets);
                enableSetDragAndDrop();
                myView.setsRecyclerView.setAdapter(selectSetAdapter);
                myView.setsRecyclerView.setItemAnimator(null);
            });

            setAdapter = new SetAdapter(getContext(), myView.myRecyclerView, isHelixSetMode());
            myView.myRecyclerView.post(() -> {
                llm = new LinearLayoutManager(getContext());
                llm.setOrientation(RecyclerView.VERTICAL);
                myView.myRecyclerView.setLayoutManager(llm);
                enableSwipeToDeleteAndUndo();
                myView.myRecyclerView.setAdapter(setAdapter);
                myView.myRecyclerView.setItemAnimator(null);
            });

            mainActivityInterface.getCurrentSet().updateSetTitleView();
        }
    }

    public void refreshSelectSetAdapter() {
        if (selectSetAdapter != null && mainActivityInterface != null) {
            mainActivityInterface.getMainHandler().post(() -> {
                bindSetTitleViews();
                selectSetAdapter.notifyDataSetChanged();
            });
        }
    }

    public void refreshAdapters() {
        if (mainActivityInterface != null) {
            mainActivityInterface.getMainHandler().post(() -> {
                bindSetTitleViews();
                if (setAdapter != null) {
                    setAdapter.notifyDataSetChanged();
                }
                if (selectSetAdapter != null) {
                    selectSetAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    private void enableSetDragAndDrop() {
        SelectSetItemCallback callback = new SelectSetItemCallback(getContext(), selectSetAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        selectSetAdapter.setTouchHelper(itemTouchHelper);
        itemTouchHelper.attachToRecyclerView(myView.setsRecyclerView);
    }

    private void enableSwipeToDeleteAndUndo() {
        SetListItemCallback setListItemCallback = new SetListItemCallback(getContext(),setAdapter) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

                final int position = viewHolder.getAbsoluteAdapterPosition();
                final SetItemInfo item = mainActivityInterface.getCurrentSet().getSetItemInfo(position);

                setAdapter.removeItem(position, true);

                Snackbar snackbar = Snackbar.make(myView.coordinatorLayout, set_item_removed_string, Snackbar.LENGTH_LONG);

                snackbar.setAction(undo_string, view -> {
                    setAdapter.restoreItem(item, position);
                    myView.myRecyclerView.scrollToPosition(position);
                });

                if (getContext()!=null) {
                    try {
                        snackbar.setBackgroundTintList(new ColorStateList(
                                new int[][]{new int[0]},
                                new int[]{mainActivityInterface.getPalette().secondary}
                        ));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    snackbar.setActionTextColor(mainActivityInterface.getPalette().textColor);
                    snackbar.setTextColor(mainActivityInterface.getPalette().textColor);
                }
                snackbar.show();
            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(setListItemCallback);
        setAdapter.setTouchHelper(itemTouchhelper);
        setAdapter.setSetListItemCallback(setListItemCallback);
        itemTouchhelper.attachToRecyclerView(myView.myRecyclerView);
    }

    private void setListeners() {
        myView.saveSetButton.post(() -> myView.saveSetButton.setOnClickListener(v -> {
            String currentSetName = mainActivityInterface.getCurrentSet().getSetCurrentLastName();
            if (currentSetName==null || currentSetName.isEmpty()) {
                mainActivityInterface.setWhattodo("saveset");
                mainActivityInterface.navigateToFragment(deeplink_sets_manage_string,0);
            } else {
                String message = save_changes_string + ": " + currentSetName + "\n\n" + overwrite_string;
                AreYouSureBottomSheet areYouSureBottomSheet = new AreYouSureBottomSheet("saveset",
                        message,null, getFragmentName(),
                        SetMenuFragment.this,null);
                areYouSureBottomSheet.show(mainActivityInterface.getMyFragmentManager(),"areYouSure");
            }
        }));
        myView.setMasterFAB.post(() -> myView.setMasterFAB.setOnClickListener(v -> {
            if (getActivity()!=null) {
                SetMenuBottomSheet setMenuBottomSheet = new SetMenuBottomSheet();
                setMenuBottomSheet.show(getActivity().getSupportFragmentManager(), "setMenuBottomSheet");
            }
        }));
        myView.myRecyclerView.post(() -> myView.myRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    myView.setMasterFAB.show();
                } else {
                    myView.setMasterFAB.hide();
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        }));
    }

    public void runSetShowcase() {
        if (getActivity()!=null) {
            try {
                String info = set_manage_click_string + "\n" + set_help_string +
                        "\n" + set_manage_swipe_string;
                myView.myRecyclerView.post(() -> mainActivityInterface.getShowCase().singleShowCase(getActivity(),
                        myView.setTitle, null, info, true, "setFragment"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void changeVisibility(boolean visible) {
        if (myView!=null) {
            myView.myRecyclerView.post(() -> myView.myRecyclerView.setVisibility(visible ? View.VISIBLE:View.INVISIBLE));
            myView.progressBar.post(() -> myView.progressBar.setVisibility(visible ? View.GONE:View.VISIBLE));
        }
    }

    public void scrollToItem() {
        if (mainActivityInterface!=null && mainActivityInterface.getCurrentSet().getIndexSongInSet()>-1 &&
                mainActivityInterface.getCurrentSet().getIndexSongInSet() < mainActivityInterface.getCurrentSet().getCurrentSetSize()) {
            myView.myRecyclerView.postDelayed(() -> {
                int position = mainActivityInterface.getCurrentSet().getIndexSongInSet();
                if (position<0 && mainActivityInterface.getCurrentSet().getCurrentSetSize()>0) {
                    position = 0;
                }
                if (llm!=null && position>=0) {
                    try {
                        int firstVisible = llm.findFirstVisibleItemPosition();
                        int lastVisible = llm.findLastVisibleItemPosition();

                        if (position < firstVisible || position > lastVisible) {
                            llm.scrollToPositionWithOffset(position, 0);
                        }
                        mainActivityInterface.notifyInlineSetScrollToItem();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            },100);
        }
    }

    public void scrollMenu(int height) {
        try {
            myView.myRecyclerView.smoothScrollBy(0, height);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateKeys() {
        if (mainActivityInterface.getSetActions().getMissingKeyPositions()!=null &&
                setAdapter!=null &&
                mainActivityInterface.getCurrentSet().getSetItemInfos()!=null &&
                mainActivityInterface.getCurrentSet().getCurrentSetSize()>0) {
            setAdapter.updateKeys();
        }
    }

    public void notifyItemRangeRemoved(int from, int count) {
        if (setAdapter!=null && mainActivityInterface!=null) {
            mainActivityInterface.getMainHandler().post(() -> setAdapter.notifyItemRangeRemoved(from, count));
        }
    }

    public void notifyItemRangeChanged(int from, int count) {
        if (setAdapter!=null && mainActivityInterface!=null) {
            mainActivityInterface.getMainHandler().post(() -> setAdapter.notifyItemRangeChanged(from, count));
        }
    }

    public void notifyItemChanged(int position) {
        if (setAdapter!=null && mainActivityInterface!=null) {
            mainActivityInterface.getMainHandler().post(() -> {
                setAdapter.notifyItemChanged(position);
                mainActivityInterface.notifyInlineSetChanged(position);
            });
        }
    }

    public void notifyItemInserted() {
        if (setAdapter!=null && mainActivityInterface.getCurrentSet().getCurrentSetSize()>0) {
            mainActivityInterface.getMainHandler().post(() -> setAdapter.insertItem());
        }
    }

    public void notifyItemCueNext() {
        if (setAdapter!=null && mainActivityInterface.getCurrentSet().getCurrentSetSize()>0) {
            mainActivityInterface.getMainHandler().post(() -> setAdapter.notifyInlineSetCueItem());
        }
    }

    public void notifyItemRemoved(int position) {
        if (setAdapter!=null && mainActivityInterface.getCurrentSet().getCurrentSetSize()>position) {
            mainActivityInterface.getMainHandler().post(() -> setAdapter.removeItem(position,false));
        }
    }

    public void updateItem(int position) {
        if (setAdapter!=null) {
            setAdapter.updateItem(position);
            mainActivityInterface.getCurrentSet().setSetCurrent(mainActivityInterface.getSetActions().getSetAsPreferenceString());
        }
    }

    public void updateAdapterPrefs() {
        if (setAdapter!=null) {
            setAdapter.getUpdatedPreferences();
        }
    }

    public void updateHighlight() {
        if (setAdapter!=null && mainActivityInterface.getSong()!=null &&
                !setAdapter.getUpdatingHighlight() && setAdapter.getHighlightChangeAllowed()) {
            int selectedPosition = mainActivityInterface.getCurrentSet().getIndexSongInSet();
            int previousPosition = mainActivityInterface.getCurrentSet().getPrevIndexSongInSet();

            if (previousPosition>-1) {
                setAdapter.removeHighlight(previousPosition);
            }
            if (selectedPosition>-1) {
                mainActivityInterface.getMainHandler().postDelayed(() -> setAdapter.updateHighlight(selectedPosition),500);
            }
        }
    }

    public void removeHighlight() {
        if (setAdapter!=null && mainActivityInterface.getCurrentSet().getPrevIndexSongInSet()>-1 &&
                !setAdapter.getRemovingHighlight() && setAdapter.getHighlightChangeAllowed()) {
            setAdapter.removeHighlight(mainActivityInterface.getCurrentSet().getPrevIndexSongInSet());
        }
    }

    public void setHighlightChangeAllowed(boolean highlightChangeAllowed) {
        if (setAdapter!=null) {
            setAdapter.setHighlightChangeAllowed(highlightChangeAllowed);
        }
    }
}
