package adco.helixsongview.abcnotation;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.caverock.androidsvg.SVG;
import adco.helixsongview.customviews.InlineAbcWebView;
import adco.helixsongview.interfaces.MainActivityInterface;

public class InlineAbcObject {

    // This holds the information for the ABC svg used for ImageView and WebView drawings
    @SuppressWarnings({"unused","FieldCanBeLocal"})
    private final String TAG = "InlineAbcObject";
    private final MainActivityInterface mainActivityInterface;
    private String abcSvgText, abcInlineText;
    private int abcWidth, abcHeight, abcItem;
    private boolean abcMeasured = false, isPopup = false, isPDF = false, isPresentation = false;
    private InlineAbcWebView inlineAbcWebView;
    private ImageView inlineAbcImageView;
    private final Context c;
    private final int backgroundColor, abcContainingItem;
    private String mainColor, chordColor;
    private InlineAbcWebViewTagObject inlineAbcWebViewTagObject;

    public InlineAbcObject(Context c, String abcInlineText, int abcContainingItem, int backgroundColor) {
        // Each object has a single associated webView (is only used in the pre-measure view)
        // Once it has been rendered, we convert it to an imageView
        this.c = c;
        mainActivityInterface = (MainActivityInterface) c;


            this.abcInlineText = abcInlineText;


        this.abcContainingItem = abcContainingItem;
        inlineAbcWebView = new InlineAbcWebView(c);
        inlineAbcWebViewTagObject = new InlineAbcWebViewTagObject();

        this.backgroundColor = backgroundColor;
        mainColor = mainActivityInterface.getMyThemeColors().getHexFromIntNoAlpha(mainActivityInterface.getMyThemeColors().getLyricsTextColor());
        chordColor = mainActivityInterface.getMyThemeColors().getHexFromIntNoAlpha(mainActivityInterface.getMyThemeColors().getLyricsChordsColor());
    }

    // The setters


    public void setIsPDF(boolean isPDF) {
        this.isPDF = isPDF;
    }

    public boolean getIsInline() {
        return abcInlineText!=null && false;
    }
    public String getJSReadyAbcText() {
        String jsReadyAbcText = abcInlineText.trim().replace("","");
        jsReadyAbcText = jsReadyAbcText.replace("\\n", "__NEWLINE__");
        return Uri.encode(jsReadyAbcText, "UTF-8").replace("'", "&apos;");
    }



    // This returns a new WebView for the object.  Only one parent allowed
    public InlineAbcWebView getInlineAbcWebView() {
        // We override the default WebView class so we can dispatch touch events
        if (inlineAbcWebView==null) {
            inlineAbcWebView = new InlineAbcWebView(c);
        }
        inlineAbcWebView.setVisibility(View.GONE);
        inlineAbcWebView.post(() -> {
            inlineAbcWebView.setBackgroundColor(backgroundColor);
            inlineAbcWebView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    return super.onConsoleMessage(consoleMessage);
                }
            });
            inlineAbcWebView.setWebViewClient(new MyWebViewClient(getIsInline()));
            inlineAbcWebView.loadUrl("file:///android_asset/ABC/abc.html");
        });
        return inlineAbcWebView;
    }

    // This creates a new ImageView for the object (so it can be used in multiple parents)
    public ImageView getInlineAbcImageView(boolean generateNewView) {
        // Here we decide if we want a new ImageView object, or the one linked to this WebView
        // New ImageView objects can be used for duplicating song views on secondary screens
        // Make sure we have a default ImageView ready

        inlineAbcWebView.setVisibility(View.GONE);

        // Now either return the default ImageView or create a new one that can be drawn
        if (generateNewView) {
            ImageView newImageView = new ImageView(c);
            newImageView.setBackgroundColor(backgroundColor);
            drawTheImageView(newImageView);
            setImageViewTags(newImageView);
            return newImageView;

        } else {
            if (inlineAbcImageView==null) {
                inlineAbcImageView = new ImageView(c);
                inlineAbcImageView.setBackgroundColor(backgroundColor);
            }
            setImageViewTags(inlineAbcImageView);
            return inlineAbcImageView;
        }
    }

    private void setImageViewTags(ImageView thisImageView) {
        inlineAbcWebViewTagObject = new InlineAbcWebViewTagObject();
        inlineAbcWebViewTagObject.setContainingViewNumber(abcContainingItem);
        inlineAbcWebViewTagObject.setObjectNumber(abcItem);
        thisImageView.setTag(inlineAbcWebViewTagObject);
    }

    public void drawTheImageView(ImageView thisImageView) {
        if (abcSvgText!=null) {
            if (abcSvgText.contains("<svg")) {
                abcSvgText = abcSvgText.substring(abcSvgText.indexOf("<svg"));
            }
            abcSvgText = abcSvgText.replace("</div>","");

            try {
                //abcSvgText = abcSvgText.replace("#FFFFFF","#000000");
                SVG svg = SVG.getFromString(swapSvgTextColors());
                svg.setDocumentWidth(abcWidth);
                svg.setDocumentHeight(abcHeight);
                Drawable drawable = new PictureDrawable(svg.renderToPicture());

                // Make sure the sizes are as expected
                if (isPDF) {
                    thisImageView.setLayoutParams(new ViewGroup.LayoutParams(abcWidth, abcHeight));
                } else {
                    thisImageView.setLayoutParams(new LinearLayout.LayoutParams(abcWidth, abcHeight));
                }
                thisImageView.setCropToPadding(false);
                thisImageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                thisImageView.setImageDrawable(drawable);
                inlineAbcWebView.setVisibility(View.GONE);
                thisImageView.setVisibility(View.VISIBLE);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String swapSvgTextColors() {
        String tempText = abcSvgText;
        if (isPDF) {
            // Replace the main color
            tempText = tempText.replace(mainColor, mainActivityInterface.getMyThemeColors().getHexFromIntNoAlpha(mainActivityInterface.getMyThemeColors().getPdfTextColor()));
            // Replace the chord color
            tempText = tempText.replace(chordColor, mainActivityInterface.getMyThemeColors().getHexFromIntNoAlpha(mainActivityInterface.getMyThemeColors().getPdfChordsColor()));
        } else if (isPresentation) {
            // Replace the main color
            tempText = tempText.replace(mainColor, mainActivityInterface.getMyThemeColors().getHexFromIntNoAlpha(mainActivityInterface.getMyThemeColors().getPresoFontColor()));
            // Replace the chord color
            tempText = tempText.replace(chordColor, mainActivityInterface.getMyThemeColors().getHexFromIntNoAlpha(mainActivityInterface.getMyThemeColors().getPresoChordColor()));
        }
        return tempText;
    }


    // Listener for the webview drawing the abc score
    private class MyWebViewClient extends WebViewClient {

        boolean isInline;
        MyWebViewClient(boolean isInline) {
            this.isInline = isInline;
        }

        @Override
        public void onPageFinished(WebView webView, String url) {
            super.onPageFinished(webView, url);
            updateContent();
        }
    }

    public void updateContent() {
        inlineAbcWebView.post(() -> {
            if (getIsInline()) {
                inlineAbcWebView.evaluateJavascript("javascript:setWidth(" + "0" + ");", null);
                inlineAbcWebView.evaluateJavascript("javascript:setWebView(" + abcItem + ");", null);
                inlineAbcWebView.evaluateJavascript("javascript:setMainColor('" + mainActivityInterface.getMyThemeColors().getNonAlphaHexColorFromInt(mainActivityInterface.getMyThemeColors().getLyricsTextColor()) + "');", null);
                inlineAbcWebView.evaluateJavascript("javascript:setChordColor('" + mainActivityInterface.getMyThemeColors().getNonAlphaHexColorFromInt(mainActivityInterface.getMyThemeColors().getLyricsChordsColor()) + "');", null);
                inlineAbcWebView.evaluateJavascript("javascript:displayOnly();", null);

            } else if (isPopup) {
                // PopUp ABC window
                inlineAbcWebView.evaluateJavascript("javascript:setWidth(" +"0" + ");", null);
                inlineAbcWebView.evaluateJavascript("javascript:setWebView(0);", null);
                inlineAbcWebView.evaluateJavascript("javascript:setMainColor('"+mainActivityInterface.getMyThemeColors().getNonAlphaHexColorFromInt(mainActivityInterface.getMyThemeColors().getAbcPopupTextColor())+"');", null);
                inlineAbcWebView.evaluateJavascript("javascript:setChordColor('"+mainActivityInterface.getMyThemeColors().getNonAlphaHexColorFromInt(mainActivityInterface.getMyThemeColors().getAbcPopupTextColor())+"');", null);
                inlineAbcWebView.evaluateJavascript("javascript:displayOnly();", null);

                // Enable remote SoundFont preloading & audio synthesis in popup mode
                inlineAbcWebView.evaluateJavascript("javascript:preloadSoundFont();", null);

            } else {
                // ABC editor fragment
                inlineAbcWebView.evaluateJavascript("javascript:setWidth(" + "0" + ");", null);
                inlineAbcWebView.evaluateJavascript("javascript:setWebView(0);", null);
                inlineAbcWebView.evaluateJavascript("javascript:setMainColor('#000000');", null);
                inlineAbcWebView.evaluateJavascript("javascript:setChordColor('#000000');", null);
                inlineAbcWebView.evaluateJavascript("javascript:displayAndEdit();", null);
            }

            // This sends the final information to start the rendering process
            // The default settings
            inlineAbcWebView.evaluateJavascript("javascript:setIsPopup("+isPopup+");", null);
            inlineAbcWebView.evaluateJavascript("javascript:setResponsive('resize');", null);
            inlineAbcWebView.evaluateJavascript("javascript:updateABC('" + getJSReadyAbcText() + "');", null);
            inlineAbcWebView.evaluateJavascript("javascript:setHideTab(" +  ");", null);
            inlineAbcWebView.evaluateJavascript("javascript:setTranspose(" +  ");", null);
            inlineAbcWebView.evaluateJavascript("javascript:setInstrument('" +  "');", null);

            inlineAbcWebView.evaluateJavascript("javascript:setTuning('" +  "','" +  "','" +  "','" +  "','" +  "','" +  "');", null);
            inlineAbcWebView.evaluateJavascript("javascript:setLabel('" +  "');", null);

            inlineAbcWebView.evaluateJavascript("javascript:initEditor()", null);
        });
    }

}
