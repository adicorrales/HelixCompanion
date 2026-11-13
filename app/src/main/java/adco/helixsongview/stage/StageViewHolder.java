package adco.helixsongview.stage;

import android.view.View;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

import adco.helixsongview.R;
import adco.helixsongview.interfaces.MainActivityInterface;

public class StageViewHolder extends RecyclerView.ViewHolder {

    MainActivityInterface mainActivityInterface;
    View v;
    FrameLayout sectionView;

    public StageViewHolder(MainActivityInterface mainActivityInterface, View v) {
        super(v);
        this.mainActivityInterface = mainActivityInterface;
        this.v = v;
        sectionView = v.findViewById(R.id.sectionView);
    }
}
