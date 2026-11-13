package adco.helixsongview.pdf;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PDFPageViewHolder extends RecyclerView.ViewHolder {
    public View v;
    public TextView pdfPageNumText;
    public ImageView pdfPageImage;
    public ImageView pdfPageHighlight;

    public PDFPageViewHolder(@NonNull View itemView) {
        super(itemView);
        this.v = itemView;
    }
}
