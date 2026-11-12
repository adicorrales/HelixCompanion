package com.garethevans.church.opensongtablet.setmenu;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.garethevans.church.opensongtablet.R;
import com.garethevans.church.opensongtablet.interfaces.MainActivityInterface;
import com.garethevans.church.opensongtablet.interfaces.SetItemTouchInterface;

import java.util.ArrayList;

public class SelectSetAdapter extends RecyclerView.Adapter<SelectSetViewHolder> implements SetItemTouchInterface {

    private final MainActivityInterface mainActivityInterface;
    private final SelectSetFragment selectSetFragment;
    private final ArrayList<String> setFilenames = new ArrayList<>();
    private ItemTouchHelper touchHelper;
    private final Object lock = new Object();

    public SelectSetAdapter(Context context, RecyclerView recyclerView, SelectSetFragment selectSetFragment) {
        this.mainActivityInterface = (MainActivityInterface) context;
        this.selectSetFragment = selectSetFragment;
        prepareSets();
    }

    public void setTouchHelper(ItemTouchHelper touchHelper) {
        this.touchHelper = touchHelper;
    }

    public void prepareSets() {
        mainActivityInterface.getThreadPoolExecutor().execute(() -> {
            ArrayList<String> allSets = mainActivityInterface.getSetActions().getAllSets();
            if (allSets == null) {
                allSets = new ArrayList<>();
            }

            // Retrieve saved order from preferences
            String savedOrder = mainActivityInterface.getPreferences().getMyPreferenceString("selectSetOrder", "");
            ArrayList<String> orderedSets = new ArrayList<>();
            if (!savedOrder.isEmpty()) {
                String[] parts = savedOrder.split(",");
                for (String part : parts) {
                    if (allSets.contains(part)) {
                        orderedSets.add(part);
                    }
                }
            }

            // Add any new sets not present in saved order
            for (String set : allSets) {
                if (!orderedSets.contains(set)) {
                    orderedSets.add(set);
                }
            }

            synchronized (lock) {
                setFilenames.clear();
                setFilenames.addAll(orderedSets);
            }

            mainActivityInterface.getMainHandler().post(this::notifyDataSetChanged);
        });
    }

    private void saveOrder() {
        StringBuilder sb = new StringBuilder();
        synchronized (lock) {
            for (int i = 0; i < setFilenames.size(); i++) {
                sb.append(setFilenames.get(i));
                if (i < setFilenames.size() - 1) {
                    sb.append(",");
                }
            }
        }
        mainActivityInterface.getPreferences().setMyPreferenceString("selectSetOrder", sb.toString());
    }

    @Override
    public int getItemCount() {
        synchronized (lock) {
            return setFilenames.size() + 1;
        }
    }

    @NonNull
    @Override
    public SelectSetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_select_set_item, parent, false);
        return new SelectSetViewHolder(itemView, mainActivityInterface, touchHelper, this);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectSetViewHolder holder, int position) {
        int actualPosition = holder.getAbsoluteAdapterPosition();
        if (actualPosition == RecyclerView.NO_POSITION) {
            actualPosition = position;
        }

        holder.numberView.setText(actualPosition + ":");
        int textColor = mainActivityInterface.getPalette().textColor;
        holder.numberView.setTextColor(textColor);
        holder.titleView.setTextColor(textColor);
        holder.folderView.setTextColor(textColor);

        int inactiveColor = mainActivityInterface.getMyThemeColors().getSetInactiveColor();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.cardView.setCardBackgroundColor(new ColorStateList(
                    new int[][]{new int[0]},
                    new int[]{inactiveColor}
            ));
        } else {
            holder.cardView.setBackgroundColor(inactiveColor);
        }

        if (actualPosition == 0) {
            holder.titleView.setText("Song Library");
            holder.folderView.setText("");
        } else {
            int setIndex = actualPosition - 1;
            String filename;
            synchronized (lock) {
                if (setIndex < setFilenames.size()) {
                    filename = setFilenames.get(setIndex);
                } else {
                    return;
                }
            }
            String niceName = mainActivityInterface.getSetActions().getNiceSetNameFromFile(filename);
            String[] bits = mainActivityInterface.getSetActions().getSetCategoryAndName(filename);
            holder.titleView.setText(niceName);
            holder.folderView.setText(bits[0]);
        }
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        if (fromPosition == 0 || toPosition == 0) {
            return;
        }

        int fromIndex = fromPosition - 1;
        int toIndex = toPosition - 1;

        synchronized (lock) {
            if (fromIndex >= 0 && fromIndex < setFilenames.size() && toIndex >= 0 && toIndex < setFilenames.size()) {
                String item = setFilenames.remove(fromIndex);
                setFilenames.add(toIndex, item);
            }
        }

        notifyItemMoved(fromPosition, toPosition);
        int min = Math.min(fromPosition, toPosition);
        int max = Math.max(fromPosition, toPosition);
        notifyItemRangeChanged(min, max - min + 1);

        saveOrder();
    }

    @Override
    public void onItemClicked(MainActivityInterface mainActivityInterface, int position) {
        if (position == 0) {
            mainActivityInterface.getThreadPoolExecutor().execute(() -> {
                mainActivityInterface.getCurrentSet().initialiseTheSet();
                mainActivityInterface.getCurrentSet().setSetCurrent("");
                mainActivityInterface.getCurrentSet().setSetCurrentBeforeEdits("");
                mainActivityInterface.getCurrentSet().setSetCurrentLastName("Song Library");
                mainActivityInterface.getMainHandler().post(() -> {
                    mainActivityInterface.updateSetList();
                    mainActivityInterface.chooseMenu(true);
                });
            });
        } else {
            int setIndex = position - 1;
            String filename;
            synchronized (lock) {
                if (setIndex < setFilenames.size()) {
                    filename = setFilenames.get(setIndex);
                } else {
                    return;
                }
            }

            mainActivityInterface.getThreadPoolExecutor().execute(() -> {
                android.net.Uri uri = mainActivityInterface.getStorageAccess().getUriForItem("Sets", "", filename);
                ArrayList<android.net.Uri> setUris = new ArrayList<>();
                setUris.add(uri);
                mainActivityInterface.getSetActions().loadSets(setUris, mainActivityInterface.getCurrentSet(), filename);
                mainActivityInterface.getMainHandler().post(() -> mainActivityInterface.chooseMenu(true));
            });
        }
    }

    @Override
    public void onRowSelected(SetListItemViewHolder myViewHolder) {
        // Not used with SelectSetViewHolder
    }
}
