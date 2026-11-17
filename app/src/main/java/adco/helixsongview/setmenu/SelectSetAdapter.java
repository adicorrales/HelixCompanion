package adco.helixsongview.setmenu;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import adco.helixsongview.R;
import adco.helixsongview.interfaces.MainActivityInterface;
import adco.helixsongview.interfaces.SetItemTouchInterface;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SelectSetAdapter extends RecyclerView.Adapter<SelectSetViewHolder> implements SetItemTouchInterface {

    private static final AtomicInteger SET_FILE_GENERATION = new AtomicInteger();
    private static final AtomicLong SET_LOAD_REQUEST = new AtomicLong();
    private static final Object SET_LOAD_LOCK = new Object();
    private static final Object SET_FILES_CACHE_LOCK = new Object();
    private static ArrayList<String> cachedSetFiles;
    private static int cachedSetFilesGeneration = -1;

    public interface OnSetSelectedListener {
        void onSetSelected(int position, String filename);
    }

    private final MainActivityInterface mainActivityInterface;
    private final boolean isHelixSetMode;
    private final OnSetSelectedListener onSetSelectedListener;
    private final ArrayList<String> setFilenames = new ArrayList<>();
    private ItemTouchHelper touchHelper;
    private final Object lock = new Object();
    private int selectedPosition = 0;
    private boolean preparingSets;
    private boolean initialSelectionRestored;
    private int loadedSetFileGeneration = -1;

    public SelectSetAdapter(Context context, RecyclerView recyclerView, boolean isHelixSetMode, OnSetSelectedListener listener) {
        this.mainActivityInterface = (MainActivityInterface) context;
        this.isHelixSetMode = isHelixSetMode;
        this.onSetSelectedListener = listener;
        this.selectedPosition = mainActivityInterface.getPreferences().getMyPreferenceInt("selectedSelectSetPosition", 0);
        prepareSets();
    }

    public void setTouchHelper(ItemTouchHelper touchHelper) {
        this.touchHelper = touchHelper;
    }

    public void prepareSets() {
        int generation = SET_FILE_GENERATION.get();
        synchronized (lock) {
            if (preparingSets || loadedSetFileGeneration == generation) {
                maybeRestoreInitialSelection();
                return;
            }
            preparingSets = true;
        }

        mainActivityInterface.getThreadPoolExecutor().execute(() -> {
            ArrayList<String> rawSets = getCachedSetFiles(generation);

            ArrayList<String> filteredSets = new ArrayList<>();
            for (String setFile : rawSets) {
                if (setFile.equalsIgnoreCase("Song Library") || setFile.equalsIgnoreCase("Song Library.ost") || setFile.equalsIgnoreCase("Helix__Song Library")) {
                    continue; // Handled at slot 0 for Helix Sets tab
                }

                boolean isHelix = setFile.startsWith("Helix__");
                boolean isLocal = setFile.startsWith("Local__");

                if (isHelixSetMode && isHelix) {
                    filteredSets.add(setFile);
                } else if (!isHelixSetMode && isLocal) {
                    filteredSets.add(setFile);
                }
            }

            // Retrieve saved order from preferences
            String orderPrefKey = isHelixSetMode ? "selectSetOrderHelix" : "selectSetOrderApp";
            String savedOrder = mainActivityInterface.getPreferences().getMyPreferenceString(orderPrefKey, "");
            ArrayList<String> orderedSets = new ArrayList<>();
            if (!savedOrder.isEmpty()) {
                String[] parts = savedOrder.split(",");
                for (String part : parts) {
                    if (filteredSets.contains(part)) {
                        orderedSets.add(part);
                    }
                }
            }

            for (String set : filteredSets) {
                if (!orderedSets.contains(set)) {
                    orderedSets.add(set);
                }
            }

            synchronized (lock) {
                setFilenames.clear();
                setFilenames.addAll(orderedSets);
            }

            mainActivityInterface.getMainHandler().post(() -> {
                synchronized (lock) {
                    preparingSets = false;
                    loadedSetFileGeneration = generation;
                }
                notifyDataSetChanged();
                if (generation != SET_FILE_GENERATION.get()) {
                    prepareSets();
                } else {
                    maybeRestoreInitialSelection();
                }
            });
        });
    }

    public static void markSetFilesChanged() {
        SET_FILE_GENERATION.incrementAndGet();
    }

    private ArrayList<String> getCachedSetFiles(int generation) {
        synchronized (SET_FILES_CACHE_LOCK) {
            if (cachedSetFiles == null || cachedSetFilesGeneration != generation) {
                cachedSetFiles = mainActivityInterface.getSetActions().getAllSets();
                if (cachedSetFiles == null) {
                    cachedSetFiles = new ArrayList<>();
                }
                cachedSetFilesGeneration = generation;
            }
            return new ArrayList<>(cachedSetFiles);
        }
    }

    private void maybeRestoreInitialSelection() {
        if (!initialSelectionRestored && loadedSetFileGeneration >= 0 &&
                isHelixSetMode == mainActivityInterface.isHelixSet()) {
            initialSelectionRestored = true;
            if (mainActivityInterface.getCurrentSet().getCurrentSetSize() == 0) {
                loadSelectedSet(selectedPosition);
            }
        }
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
        String orderPrefKey = isHelixSetMode ? "selectSetOrderHelix" : "selectSetOrderApp";
        mainActivityInterface.getPreferences().setMyPreferenceString(orderPrefKey, sb.toString());
    }

    @Override
    public int getItemCount() {
        synchronized (lock) {
            if (isHelixSetMode) {
                return setFilenames.size() + 1; // +1 for Song Library at index 0
            } else {
                return setFilenames.size();
            }
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

        int inactiveColor = mainActivityInterface.getMyThemeColors().getSetInactiveColor();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            holder.cardView.setCardBackgroundColor(new ColorStateList(
                    new int[][]{new int[0]},
                    new int[]{inactiveColor}
            ));
        } else {
            holder.cardView.setBackgroundColor(inactiveColor);
        }

        boolean isHelixActive = mainActivityInterface.isHelixSet();
        String activeFilename = mainActivityInterface.getPreferences().getMyPreferenceString("selectedActiveSetFilename", "Song Library");

        boolean isChecked = false;
        if (isHelixSetMode && isHelixActive) {
            if (actualPosition == 0 && (activeFilename.equalsIgnoreCase("Song Library") || activeFilename.equalsIgnoreCase("Song Library.ost") || activeFilename.equalsIgnoreCase("Helix__Song Library"))) {
                isChecked = true;
            } else if (actualPosition > 0 && (actualPosition - 1) < setFilenames.size()) {
                isChecked = setFilenames.get(actualPosition - 1).equalsIgnoreCase(activeFilename);
            }
        } else if (!isHelixSetMode && !isHelixActive) {
            if (actualPosition >= 0 && actualPosition < setFilenames.size()) {
                isChecked = setFilenames.get(actualPosition).equalsIgnoreCase(activeFilename);
            }
        }

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(isChecked);
        holder.checkBox.setClickable(false);
        holder.checkBox.setFocusable(false);

        int finalActualPosition = actualPosition;
        View.OnClickListener clickListener = v -> onSetItemClicked(finalActualPosition);
        holder.cardView.setOnClickListener(clickListener);
        holder.checkBox.setOnClickListener(clickListener);
        holder.itemView.setOnClickListener(clickListener);

        if (isHelixSetMode && actualPosition == 0) {
            holder.titleView.setText("Song Library");
        } else {
            int setIndex = isHelixSetMode ? (actualPosition - 1) : actualPosition;
            String filename;
            synchronized (lock) {
                if (setIndex >= 0 && setIndex < setFilenames.size()) {
                    filename = setFilenames.get(setIndex);
                } else {
                    return;
                }
            }
            String niceName = filename;
            if (filename.contains("__")) {
                String[] bits = filename.split("__");
                niceName = bits[bits.length - 1];
            }
            holder.titleView.setText(niceName);
        }
    }

    private void onSetItemClicked(int position) {
        if (position == RecyclerView.NO_POSITION) {
            return;
        }

        String filename = "Song Library";
        if (isHelixSetMode && position == 0) {
            selectedPosition = 0;
            filename = "Song Library";
        } else {
            int setIndex = isHelixSetMode ? (position - 1) : position;
            synchronized (lock) {
                if (setIndex >= 0 && setIndex < setFilenames.size()) {
                    filename = setFilenames.get(setIndex);
                }
            }
            selectedPosition = position;
        }

        boolean alreadyActive = mainActivityInterface.isHelixSet() == isHelixSetMode &&
                filename.equalsIgnoreCase(mainActivityInterface.getPreferences()
                        .getMyPreferenceString("selectedActiveSetFilename", "Song Library"));

        mainActivityInterface.setHelixSet(isHelixSetMode);
        mainActivityInterface.getPreferences().setMyPreferenceString("selectedActiveSetFilename", filename);
        mainActivityInterface.getPreferences().setMyPreferenceInt("selectedSelectSetPosition", selectedPosition);

        notifyDataSetChanged();
        if (mainActivityInterface.getSetMenuFragment() != null) {
            mainActivityInterface.getSetMenuFragment().refreshAdapters();
        }
        if (mainActivityInterface.getSelectSetFragment() != null) {
            mainActivityInterface.getSelectSetFragment().refreshAdapters();
        }

        if (!alreadyActive || mainActivityInterface.getCurrentSet().getCurrentSetSize() == 0) {
            loadSelectedSet(selectedPosition, filename);
        }
    }

    public void loadSelectedSet(int position) {
        String filename = getFilenameForPosition(position);
        if (filename != null) {
            loadSelectedSet(position, filename);
        }
    }

    private String getFilenameForPosition(int position) {
        if (isHelixSetMode && position == 0) {
            return "Song Library";
        }
        int setIndex = isHelixSetMode ? position - 1 : position;
        synchronized (lock) {
            return setIndex >= 0 && setIndex < setFilenames.size()
                    ? setFilenames.get(setIndex)
                    : null;
        }
    }

    private void loadSelectedSet(int position, String filename) {
        mainActivityInterface.resetLastSentMidiSetIdx();
        long requestId = SET_LOAD_REQUEST.incrementAndGet();
        mainActivityInterface.getThreadPoolExecutor().execute(() -> {
            synchronized (SET_LOAD_LOCK) {
                if (requestId != SET_LOAD_REQUEST.get()) {
                    return;
                }

                ArrayList<android.net.Uri> setUris = new ArrayList<>();
                android.net.Uri uri = mainActivityInterface.getStorageAccess()
                        .getUriForItem("Sets", "", filename);
                if (mainActivityInterface.getStorageAccess().uriExists(uri)) {
                    setUris.add(uri);
                }
                // loadSets replaces the current contents and saves the result once; do not
                // clear the shared CurrentSet outside this serialized operation.
                mainActivityInterface.getSetActions().loadSets(
                        setUris, mainActivityInterface.getCurrentSet(), filename);
            }

            if (requestId != SET_LOAD_REQUEST.get()) {
                return;
            }

            final String loadedFilename = filename;
            mainActivityInterface.getMainHandler().post(() -> {
                if (requestId != SET_LOAD_REQUEST.get()) {
                    return;
                }
                mainActivityInterface.setHelixSet(isHelixSetMode);
                mainActivityInterface.getPreferences().setMyPreferenceString("selectedActiveSetFilename", loadedFilename);
                mainActivityInterface.getPreferences().setMyPreferenceInt("selectedSelectSetPosition", position);
                if (isHelixSetMode) {
                    mainActivityInterface.sendHelixMidiSetAndSong(position, 0);
                }
                mainActivityInterface.notifyToInsertAllInlineSet();
                mainActivityInterface.updateInlineSetVisibility();
                mainActivityInterface.getCurrentSet().updateSetTitleView();
                if (onSetSelectedListener != null) {
                    onSetSelectedListener.onSetSelected(position, loadedFilename);
                }
                if (mainActivityInterface.getSetMenuFragment() != null) {
                    mainActivityInterface.getSetMenuFragment().refreshAdapters();
                }
                if (mainActivityInterface.getSelectSetFragment() != null) {
                    mainActivityInterface.getSelectSetFragment().refreshSelectSetAdapter();
                }
                mainActivityInterface.updateSetList();
            });
        });
    }

    @Override
    public void onItemMoved(int fromPosition, int toPosition) {
        if (isHelixSetMode && (fromPosition == 0 || toPosition == 0)) {
            return; // Song library cannot be moved
        }

        int fromIndex = isHelixSetMode ? (fromPosition - 1) : fromPosition;
        int toIndex = isHelixSetMode ? (toPosition - 1) : toPosition;

        synchronized (lock) {
            if (fromIndex >= 0 && fromIndex < setFilenames.size() && toIndex >= 0 && toIndex < setFilenames.size()) {
                String item = setFilenames.remove(fromIndex);
                setFilenames.add(toIndex, item);
            }
        }

        if (selectedPosition == fromPosition) {
            selectedPosition = toPosition;
        } else if (fromPosition < selectedPosition && toPosition >= selectedPosition) {
            selectedPosition--;
        } else if (fromPosition > selectedPosition && toPosition <= selectedPosition) {
            selectedPosition++;
        }
        mainActivityInterface.getPreferences().setMyPreferenceInt("selectedSelectSetPosition", selectedPosition);

        notifyItemMoved(fromPosition, toPosition);
        int min = Math.min(fromPosition, toPosition);
        int max = Math.max(fromPosition, toPosition);
        notifyItemRangeChanged(min, max - min + 1);

        saveOrder();
    }

    @Override
    public void onItemClicked(MainActivityInterface mainActivityInterface, int position) {
        if (position != RecyclerView.NO_POSITION) {
            onSetItemClicked(position);
        }
    }

    @Override
    public void onRowSelected(SetListItemViewHolder myViewHolder) {
    }
}
