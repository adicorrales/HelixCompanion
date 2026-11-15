package adco.helixsongview.pdf;

import android.content.Context;
import adco.helixsongview.interfaces.MainActivityInterface;
import adco.helixsongview.interfaces.DisplayInterface;
import java.util.ArrayList;

public class PDFPageAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<PDFPageViewHolder> {

    public float getPdfHorizontalScale() { return 1f; }
    public int getWidth() { return 0; }
    public int getHeight() { return 0; }
    public ArrayList<Float> getWidths() { return new ArrayList<>(); }

    @Override
    public PDFPageViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) { return null; }
    @Override
    public void onBindViewHolder(PDFPageViewHolder holder, int position) {}
    @Override
    public int getItemCount() { return 0; }
}
