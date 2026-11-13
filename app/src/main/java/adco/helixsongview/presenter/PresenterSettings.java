package adco.helixsongview.presenter;

import android.content.Context;
import android.net.Uri;
import adco.helixsongview.interfaces.MainActivityInterface;

public class PresenterSettings {
    private final MainActivityInterface mainActivityInterface;

    public PresenterSettings(Context c) {
        mainActivityInterface = (MainActivityInterface) c;
    }

    public boolean getLogoOn() { return false; }
    public void setLogoOn(boolean logoOn) {}
    public boolean getBlackscreenOn() { return false; }
    public void setBlackscreenOn(boolean blackscreenOn) {}
    public boolean getBlankscreenOn() { return false; }
    public void setBlankscreenOn(boolean blankscreenOn) {}
    public int getCurrentSection() { return -1; }
    public void setCurrentSection(int currentSection) {}
    public float getCastRotation() { return 0f; }
    public void setCastRotation(float r) {}
    public boolean getHideInfoBar() { return false; }
    public void setHideInfoBar(boolean hideInfoBar) {}
    public String getBackgroundToUse() { return "color"; }
    public Uri getChosenBackground() { return null; }
    public int getBackgroundColor() { return 0xFF000000; }
    public int getPresoTransitionTime() { return 500; }
    public void setPresoTransitionTime(int time) {}
    public float getPresoBackgroundAlpha() { return 1f; }
    public void setPresoBackgroundAlpha(float a) {}
    public float getLogoSize() { return 0.5f; }
    public void setLogoSize(float size) {}
    public Uri getLogo() { return null; }
    public void setLogo(Uri logo) {}
    public void setStartedProjection(boolean started) {}
    public void getAllPreferences() {}
    public void getImagePreferences() {}
    public void setBackgroundColor(int color) {}
    public void setUsePresentationOrder(boolean use) {}
    public Uri getBackgroundImage1() { return null; }
    public Uri getBackgroundImage2() { return null; }
    public Uri getBackgroundVideo1() { return null; }
    public Uri getBackgroundVideo2() { return null; }
    public void setBackgroundImage1(Uri uri) {}
    public void setBackgroundImage2(Uri uri) {}
    public void setBackgroundVideo1(Uri uri) {}
    public void setBackgroundVideo2(Uri uri) {}
    public float getPresoTitleTextSize() { return 24f; }
    public void setPresoTitleTextSize(float s) {}
    public float getPresoAuthorTextSize() { return 18f; }
    public void setPresoAuthorTextSize(float s) {}
    public float getPresoCopyrightTextSize() { return 14f; }
    public void setPresoCopyrightTextSize(float s) {}
    public float getPresoClockSize() { return 18f; }
    public void setPresoClockSize(float s) {}
    public boolean getPresoShowClock() { return false; }
    public void setPresoShowClock(boolean show) {}
    public boolean getPresoClock24h() { return true; }
    public void setPresoClock24h(boolean h24) {}
    public boolean getPresoClockSeconds() { return false; }
    public void setPresoClockSeconds(boolean secs) {}
    public int getPresoInfoAlign() { return 0; }
    public void setPresoInfoAlign(int align) {}
    public int getPresoXMargin() { return 0; }
    public void setPresoXMargin(int m) {}
    public int getPresoYMargin() { return 0; }
    public void setPresoYMargin(int m) {}
    public boolean getDefaultPresentationText() { return true; }
    public void setDefaultPresentationText(boolean def) {}
    public String getCcliLicence() { return ""; }
    public boolean getShowNextLinePreview() { return false; }
    public void setShowNextLinePreview(boolean show) {}
    public float getFontSizePresoMax() { return 64f; }
    public void setFontSizePresoMax(float s) {}
    public Object getSongSectionsAdapter() {
        return new Object() {
            public int getItemCount() { return 0; }
        };
    }
    public int getPresoLyricsAlign() { return 0; }
    public void setPresoLyricsAlign(int align) {}
    public int getPresoLyricsVAlign() { return 0; }
    public void setPresoLyricsVAlign(int valign) {}
    public boolean getPresoShowChords() { return true; }
    public void setPresoShowChords(boolean show) {}
    public boolean getPresoLyricsBold() { return false; }
    public void setPresoLyricsBold(boolean bold) {}
    public boolean getUsePresentationOrder() { return false; }
    public int getShowNextLinePreviewTransparency() { return 0; }
    public void setShowNextLinePreviewTransparency(int t) {}
    public float getShowNextLinePreviewSpacing() { return 1f; }
    public void setShowNextLinePreviewSpacing(float s) {}
}
