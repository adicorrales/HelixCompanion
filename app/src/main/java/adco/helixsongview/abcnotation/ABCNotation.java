package adco.helixsongview.abcnotation;

import android.content.Context;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;

import adco.helixsongview.R;
import adco.helixsongview.customviews.InlineAbcWebView;
import adco.helixsongview.export.ExportFragment;
import adco.helixsongview.interfaces.MainActivityInterface;
import adco.helixsongview.songprocessing.DrumCalculations;
import adco.helixsongview.songprocessing.Song;

import java.util.ArrayList;
import java.util.Locale;

public class ABCNotation {

    @SuppressWarnings({"unused","FieldCanBeLocal"})
    private final String TAG = "ABCNotation";



    private float abcPopupWidth=0.5f;
    private boolean abcAutoTranspose;
    private boolean autoshowMusicScore;
    private boolean abcIncludeTab;
    private boolean abcWebViewsDrawn = false;
    private final MainActivityInterface mainActivityInterface;
    private final String guitar, guitar_drop_d, guitar_open_g, banjo4, banjo5, mandolin, cavaquinho, ukulele, bass4, bass5, violin, fiddle;
    private int abcInlineWidth;
    @SuppressWarnings("FieldCanBeLocal")
    private final String inlineAbcLineIndicator = ";#:";
    private final Context c;
    private ArrayList<InlineAbcObject> inlineAbcObjects = new ArrayList<>();
    private ExportFragment exportFragment;
    public ABCNotation(Context c) {
        this.c = c;
        mainActivityInterface = (MainActivityInterface) c;
        guitar = c.getString(R.string.guitar);
        guitar_drop_d = c.getString(R.string.guitar_drop_d);
        guitar_open_g = c.getString(R.string.guitar_open_g);
        banjo4 = c.getString(R.string.banjo4);
        banjo5 = c.getString(R.string.banjo5);
        mandolin = c.getString(R.string.mandolin);
        cavaquinho = c.getString(R.string.cavaquinho);
        ukulele = c.getString(R.string.ukulele);
        bass4 = c.getString(R.string.bass4);
        bass5 = c.getString(R.string.bass5);
        violin = c.getString(R.string.violin);
        fiddle = c.getString(R.string.fiddle);
        getUpdatedPreferences();
    }

    // If we change load in a profile, this is called
    public void getUpdatedPreferences() {
        abcAutoTranspose = mainActivityInterface.getPreferences().getMyPreferenceBoolean("abcAutoTranspose",true);
        abcPopupWidth = mainActivityInterface.getPreferences().getMyPreferenceFloat("abcPopupWidth",0.5f);
        autoshowMusicScore = mainActivityInterface.getPreferences().getMyPreferenceBoolean("autoshowMusicScore",false);
        abcIncludeTab = mainActivityInterface.getPreferences().getMyPreferenceBoolean("abcIncludeTab",false);
        abcInlineWidth = mainActivityInterface.getPreferences().getMyPreferenceInt("abcInlineWidth",480);
    }

    public void setWebView(InlineAbcWebView inlineAbcWebView) {
        inlineAbcWebView.post(new Runnable() {
            @Override
            public void run() {
                inlineAbcWebView.setInitialScale(1);
                inlineAbcWebView.getSettings().setDomStorageEnabled(true);
                inlineAbcWebView.getSettings().setLoadWithOverviewMode(true);
                inlineAbcWebView.getSettings().setUseWideViewPort(true);
                inlineAbcWebView.getSettings().setSupportZoom(true);
                inlineAbcWebView.getSettings().setBuiltInZoomControls(true);
                inlineAbcWebView.getSettings().setDisplayZoomControls(false);
                inlineAbcWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
                inlineAbcWebView.setScrollbarFadingEnabled(false);
                inlineAbcWebView.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                        return super.onConsoleMessage(consoleMessage);
                    }
                });
                inlineAbcWebView.loadUrl("file:///android_asset/ABC/abc.html");

            }
        });
    }

    public int getAbcInlineWidth() {
        return abcInlineWidth;
    }
    public String getInlineAbcLineIndicator() {
        return inlineAbcLineIndicator;
    }



    public int countInlineAbcObjects() {
        return inlineAbcObjects.size();
    }

    public void addInlineAbcObject(InlineAbcObject inlineAbcObject) {
        int item = countInlineAbcObjects();
    }

    public ArrayList<InlineAbcObject> getInlineAbcObjects() {
        return inlineAbcObjects;
    }

    public void setAbcWebViewsDrawn(boolean abcWebViewsDrawn) {
        this.abcWebViewsDrawn = abcWebViewsDrawn;
    }

    public boolean getAbcWebViewsDrawn() {
        return abcWebViewsDrawn;
    }

    public ExportFragment getExportFragment() {
        return exportFragment;
    }
}
