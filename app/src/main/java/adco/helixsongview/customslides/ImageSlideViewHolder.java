package adco.helixsongview.customslides;

import android.view.View;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import adco.helixsongview.R;
import adco.helixsongview.customviews.MyMaterialSimpleTextView;
import adco.helixsongview.interfaces.MainActivityInterface;

public class ImageSlideViewHolder extends RecyclerView.ViewHolder {

    MainActivityInterface mainActivityInterface;
    View v;
    ImageView imageSlideImage;
    ImageView imageSlideHighlight;
    MyMaterialSimpleTextView imageSlideNumText;

    public ImageSlideViewHolder(MainActivityInterface mainActivityInterface, View v) {
        super(v);
        this.v = v;
        imageSlideImage = v.findViewById(R.id.pdfPageImage);
        imageSlideHighlight = v.findViewById(R.id.pdfPageHighlight);
        imageSlideNumText = v.findViewById(R.id.pdfPageNumText);
        this.mainActivityInterface = mainActivityInterface;
    }
}
