package adco.helixsongview.pdf;

import android.content.Context;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import adco.helixsongview.interfaces.DisplayInterface;
import adco.helixsongview.interfaces.MainActivityInterface;
import java.util.ArrayList;

public class PDFPageAdapter extends RecyclerView.Adapter<PDFPageViewHolder> {
    public PDFPageAdapter(Context c, MainActivityInterface m, DisplayInterface d, String f, String fn, String ft) {}
    public PDFPageAdapter(Context c, MainActivityInterface m, DisplayInterface d, int w, int h, int x) {}
    public float getPdfHorizontalScale() { return 1f; }
    public int getWidth() { return 0; }
    public int getHeight() { return 0; }
    public ArrayList<Float> getWidths() { return new ArrayList<>(); }
    public ArrayList<Float> getHeights() { return new ArrayList<>(); }
    @NonNull @Override public PDFPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) { return null; }
    @Override public void onBindViewHolder(@NonNull PDFPageViewHolder holder, int position) {}
    @Override public int getItemCount() { return 0; }
    public void toggleHighlighter() {}
    public void sectionSelected(int p) {}
    public void clickOnSection(int p) {}
}
