package adco.helixsongview.interfaces;

import adco.helixsongview.setmenu.SetListItemViewHolder;

public interface SetItemTouchInterface {
    void onItemMoved(int fromPosition, int toPosition);
    void onItemClicked(MainActivityInterface mainActivityInterface, int position);
    void onRowSelected(SetListItemViewHolder myViewHolder);
}
