package adco.helixsongview.abcnotation;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;

import adco.helixsongview.interfaces.MainActivityInterface;

public class ABCWebViewJSInterface {

    // This is triggered from the abc.html file after the WebView has drawn and measured
    private final MainActivityInterface mainActivityInterface;
    @SuppressWarnings({"unused","FieldCanBeLocal"})
    private final String TAG = "ABCWebViewJSInterface";

    public ABCWebViewJSInterface(Context c) {
        mainActivityInterface = (MainActivityInterface) c;
    }

}
