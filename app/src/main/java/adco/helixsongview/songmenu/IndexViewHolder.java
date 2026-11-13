package adco.helixsongview.songmenu;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import adco.helixsongview.R;
import adco.helixsongview.customviews.MyMaterialSimpleTextView;

public class IndexViewHolder extends RecyclerView.ViewHolder {
    MyMaterialSimpleTextView textView;

    public IndexViewHolder(View v) {
        super(v);
        textView = v.findViewById(R.id.alphalist);
    }
}
