package com.garethevans.church.opensongtablet.pdf;

import android.content.Context;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.print.PrintAttributes;
import android.view.View;
import android.widget.LinearLayout;
import com.garethevans.church.opensongtablet.songprocessing.Song;
import java.io.File;
import java.util.ArrayList;

public class MakePDF {
    public MakePDF(Context c) {}
    public boolean getForceSinglePage() { return false; }
    public void setForceSinglePage(boolean f) {}
    public float getMaxPDFScaling() { return 1f; }
    public void setMaxPDFScaling(float s) {}
    public void setIsSetListPrinting(boolean p) {}
    public boolean getIsSetListPrinting() { return false; }
    public void setSong(Song s) {}
    public void setHeaderHeight(int h) {}
    public void setExportingSongList(boolean e) {}
public void getColumns(ArrayList<View> v1, ArrayList<Integer> i1, ArrayList<Integer> i2) {}
  public Uri createTextPDF(ArrayList<View> v1, ArrayList<Integer> i1, ArrayList<Integer> i2, LinearLayout l, int w, int h, String name, Object o) { return Uri.EMPTY; }
    public void createBlankPDFDoc(String name, PrintAttributes a) {}
    public void addCurrentItemToPDF(ArrayList<View> v, ArrayList<Integer> i1, ArrayList<Integer> i2, LinearLayout l) {}
    public void addCurrentItemToPDF(ArrayList<View> v, ArrayList<Integer> i1, ArrayList<Integer> i2, LinearLayout l, int w, int h) {}
    public String getExportFilename() { return ""; }
    public Uri getPDFFile(String name) { return Uri.EMPTY; }
    public PdfDocument getPdfDocument() { return null; }
    public void setPreferedAttributes() {}
    public PrintAttributes getPrintAttributes() { return null; }
}
