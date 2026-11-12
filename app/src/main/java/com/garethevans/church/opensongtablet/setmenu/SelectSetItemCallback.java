package com.garethevans.church.opensongtablet.setmenu;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.garethevans.church.opensongtablet.interfaces.SetItemTouchInterface;

public class SelectSetItemCallback extends ItemTouchHelper.Callback {

    private final SetItemTouchInterface setItemTouchInterface;

    public SelectSetItemCallback(Context context, SelectSetAdapter adapter) {
        this.setItemTouchInterface = adapter;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAbsoluteAdapterPosition();
        if (position == 0) {
            return makeMovementFlags(0, 0);
        }
        int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        return makeMovementFlags(dragFlags, 0);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        int from = viewHolder.getAbsoluteAdapterPosition();
        int to = target.getAbsoluteAdapterPosition();
        if (from == 0 || to == 0) {
            return false;
        }
        setItemTouchInterface.onItemMoved(from, to);
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
    }
}
