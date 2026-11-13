package adco.helixsongview.tags;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import adco.helixsongview.R;
import adco.helixsongview.customviews.MyMaterialTextView;
import com.google.android.material.card.MaterialCardView;

public class TagViewHolder extends RecyclerView.ViewHolder {

    public MyMaterialTextView songInfo;
    public MaterialCardView cardView;

    public TagViewHolder(@NonNull View itemView) {
        super(itemView);
        songInfo = itemView.findViewById(R.id.songInfo);
        cardView = itemView.findViewById(R.id.cardView);
    }
}
