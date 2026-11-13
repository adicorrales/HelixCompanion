package adco.helixsongview.pdf;

import android.content.Context;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.print.PrintAttributes;
import android.view.View;
import android.widget.LinearLayout;
import adco.helixsongview.interfaces.MainActivityInterface;
import adco.helixsongview.songprocessing.Song;
import java.util.ArrayList;

public class MakePDF {
    private final MainActivityInterface mainActivityInterface;
    public MakePDF(Context c) {
        mainActivityInterface = (MainActivityInterface) c;
    }
    public boolean getForceSinglePage() { return false; }
    public void setForceSinglePage(boolean force) {}
    public float getMaxPDFScaling() { return 1f; }
    public void setMaxPDFScaling(float scale) {}
    public void setIsSetListPrinting(boolean printing) {}
    public void setExportingSongList(boolean exporting) {}
    public void setSong(Song song) {}
    public void getColumns(ArrayList<View> v, ArrayList<Integer> i1, ArrayList<Integer> i2, int w, int h) {}
    public void getColumns(ArrayList<View> v, ArrayList<Integer> i1, ArrayList<Integer> i2) {}
    public Uri createTextPDF(ArrayList<View> v1, ArrayList<Integer> i1, ArrayList<Integer> i2, LinearLayout l, int w, int h, String name, Object o) { return Uri.EMPTY; }
    public void createBlankPDFDoc(String name, PrintAttributes a) {}
    public void addCurrentItemToPDF(ArrayList<View> v, ArrayList<Integer> i1, ArrayList<Integer> i2, LinearLayout l, int w, int h) {}
    public String getExportFilename() { return ""; }
    public Uri getPDFFile(String filename) { return Uri.EMPTY; }
    public PdfDocument getPdfDocument() { return null; }
    public boolean getIsSetListPrinting() { return false; }
    public void setPreferedAttributes() {}
    public PrintAttributes getPrintAttributes() { return new PrintAttributes.Builder().build(); }
    public void setHeaderHeight(int h) {}
}
