package adco.helixsongview.appdata;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

import adco.helixsongview.R;
import adco.helixsongview.customviews.MyMaterialSimpleTextView;
import adco.helixsongview.interfaces.MainActivityInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Font handling for the app.
 *
 * Only LOCAL fonts are used:
 *  - fonts bundled in the app assets (Lato, Inconsolata, Roboto Mono)
 *  - font files the user has placed in the 'Fonts' folder
 *
 * There is no dependency on Google Play Services, on the Google Fonts provider or
 * on any network fetch. Resolving a remote (provider) font on every TextView
 * creation was very expensive, so everything is a plain, cached local Typeface.
 */
public class MyFonts {
    @SuppressWarnings({"unused","FieldCanBeLocal"})
    private final String TAG = "MyFonts";
    private final Context c;
    private final MainActivityInterface mainActivityInterface;

    private ArrayList<String> monoFontNames = new ArrayList<>();

    // Names of the fonts bundled in the app assets
    public static final String FONT_LATO = "Lato",
            FONT_INCONSOLATA = "Inconsolata",
            FONT_ROBOTO_MONO = "Roboto Mono";

    private static final String ASSET_INCONSOLATA = "font/Inconsolata.ttf",
            ASSET_ROBOTO_MONO = "font/RobotoMono.ttf";

    // The fonts used in the app
    private Typeface lyricFont;
    private Typeface chordFont;
    private Typeface presoFont;
    private Typeface presoInfoFont;
    private Typeface stickyFont;
    private Typeface monoFont;

    private String lyricFontName, chordFontName, monoFontName;

    // Loaded bundled typefaces, so a font file is parsed only once
    private final HashMap<String, Typeface> bundledFontCache = new HashMap<>();

    public MyFonts(Context c) {
        this.c = c;
        mainActivityInterface = (MainActivityInterface) c;
    }

    // Set the fonts
    public void setLyricFont(String lyricFontName, Typeface lyricFont) {
        this.lyricFontName = lyricFontName;
        this.lyricFont = lyricFont;
    }
    public void setChordFont(String chordFontName, Typeface chordFont) {
        this.chordFontName = chordFontName;
        this.chordFont = chordFont;
    }
    public void setPresoFont(Typeface presoFont) {
        this.presoFont = presoFont;
    }
    public void setPresoInfoFont(Typeface presoInfoFont) {
        this.presoInfoFont = presoInfoFont;
    }
    public void setStickyFont(Typeface stickyFont) {
        this.stickyFont = stickyFont;
    }
    public void setMonoFont(String monoFontName, Typeface monoFont) {
        this.monoFontName = monoFontName;
        this.monoFont = monoFont;
    }

    // Get the fonts
    public Typeface getLyricFont() {
        return lyricFont;
    }
    public Typeface getChordFont() {
        return chordFont;
    }
    public Typeface getPresoFont() {
        return presoFont;
    }
    public Typeface getPresoInfoFont() {
        return presoInfoFont;
    }
    public Typeface getStickyFont() {
        return stickyFont;
    }
    public Typeface getMonoFont() {
        return monoFont;
    }

    public String getLyricFontName() {
        return lyricFontName;
    }
    public String getChordFontName() {
        return chordFontName;
    }
    public String getMonoFontName() {
        return monoFontName;
    }

    /**
     * Loads the fonts requested in the preferences.
     * The handler is kept for API compatibility with the callers, loading is synchronous
     * now that no remote font has to be requested.
     */
    public void setUpAppFonts(Handler lyricFontHandler,
                              Handler chordFontHandler, Handler stickyFontHandler,
                              Handler presoFontHandler, Handler presoInfoHandler, Handler monoFontHandler) {

        // Load up the user preferences
        lyricFontName = mainActivityInterface.getPreferences().getMyPreferenceString("fontLyric", FONT_LATO);
        chordFontName = mainActivityInterface.getPreferences().getMyPreferenceString("fontChord", FONT_LATO);
        monoFontName = mainActivityInterface.getPreferences().getMyPreferenceString("fontMono", FONT_INCONSOLATA);
        String fontSticky = mainActivityInterface.getPreferences().getMyPreferenceString("fontSticky", FONT_LATO);
        String fontPreso = mainActivityInterface.getPreferences().getMyPreferenceString("fontPreso", FONT_LATO);
        String fontPresoInfo = mainActivityInterface.getPreferences().getMyPreferenceString("fontPresoInfo", FONT_LATO);

        changeFont("fontLyric", lyricFontName, lyricFontHandler);
        changeFont("fontChord", chordFontName, chordFontHandler);
        changeFont("fontPreso", fontPreso, presoFontHandler);
        changeFont("fontPresoInfo", fontPresoInfo, presoInfoHandler);
        changeFont("fontSticky", fontSticky, stickyFontHandler);
        changeFont("fontMono", monoFontName, monoFontHandler);
    }

    /**
     * Sets the font for the given setting. Only local fonts are supported:
     * a file name from the user's 'Fonts' folder or one of the bundled fonts.
     */
    public void changeFont(String which, String fontName, Handler handler) {
        // Save the preferences
        mainActivityInterface.getPreferences().setMyPreferenceString(which, fontName);

        if (fontName != null && fontName.startsWith("Fonts/")) {
            if (loadUserFont(which, fontName)) {
                return;
            }
            // The file is no longer available - fall back to the bundled default
            Log.w(TAG, "Local font not available: " + fontName + ". Using the bundled default.");
        }

        String localName = normaliseFontName(fontName);
        doSetDesiredFont(which, getBundledTypeface(localName), localName, null);
    }

    /**
     * Copies a font from the user's 'Fonts' folder into app storage and loads it.
     * @return true if the font was loaded
     */
    private boolean loadUserFont(String which, String fontName) {
        try {
            String actualName = fontName.replace("Fonts/", "");
            // We need the font to be in a file readable location - the app storage
            File fontFile = mainActivityInterface.getStorageAccess().getAppSpecificFile("Files","",actualName);
            Uri uri = mainActivityInterface.getStorageAccess().getUriForItem("Fonts", "", actualName);
            InputStream inputStream = mainActivityInterface.getStorageAccess().getInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(fontFile);
            mainActivityInterface.getStorageAccess().copyFile(inputStream, outputStream);
            Typeface typeface = Typeface.createFromFile(fontFile.getPath());
            doSetDesiredFont(which, typeface, fontName, null);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Could not load local font " + fontName, e);
            return false;
        }
    }

    private void doSetDesiredFont(String which, Typeface typeface, String fontName, MyMaterialSimpleTextView textView) {
        // Set the desired font
        switch (which) {
            case "fontLyric":
                setLyricFont(fontName,typeface);
                break;
            case "fontChord":
                setChordFont(fontName,typeface);
                break;
            case "fontSticky":
                setStickyFont(typeface);
                break;
            case "fontPreso":
                setPresoFont(typeface);
                break;
            case "fontPresoInfo":
                setPresoInfoFont(typeface);
                break;
            case "fontMono":
                setMonoFont(fontName,typeface);
                break;
        }
        mainActivityInterface.getPreferences().setMyPreferenceString(which,fontName);

        // If we are previewing the font, update the text (this will be null otherwise)
        if (textView != null) {
            textView.setTypeface(typeface);
        }
    }

    /**
     * Fonts available to the user: the bundled ones plus the font files found in the
     * 'Fonts' folder. No network access is needed.
     */
    public ArrayList<String> getAvailableFonts() {
        ArrayList<String> fontNames = new ArrayList<>();
        fontNames.add(FONT_LATO);

        monoFontNames = new ArrayList<>();
        monoFontNames.add(FONT_INCONSOLATA);
        monoFontNames.add(FONT_ROBOTO_MONO);

        ArrayList<String> userFonts = mainActivityInterface.getStorageAccess().listFilesInFolder("Fonts","");
        if (userFonts != null && !userFonts.isEmpty()) {
            Collections.sort(userFonts, Collections.reverseOrder());
            for (String userFont : userFonts) {
                fontNames.add(0, "Fonts/" + userFont);
                monoFontNames.add(0, "Fonts/" + userFont);
            }
        }

        return fontNames;
    }

    public ArrayList<String> getAvailableMonoFonts() {
        return monoFontNames;
    }

    public Typeface getAppDefault() {
        return getBundledTypeface(FONT_LATO);
    }

    // Returns the name of a bundled font, falling back to Lato for unknown names
    private String normaliseFontName(String fontName) {
        if (fontName != null) {
            if (fontName.equalsIgnoreCase(FONT_INCONSOLATA)) {
                return FONT_INCONSOLATA;
            }
            if (fontName.equalsIgnoreCase(FONT_ROBOTO_MONO) || fontName.equalsIgnoreCase("RobotoMono")) {
                return FONT_ROBOTO_MONO;
            }
        }
        return FONT_LATO;
    }

    private Typeface getBundledTypeface(String fontName) {
        Typeface cached = bundledFontCache.get(fontName);
        if (cached != null) {
            return cached;
        }
        Typeface typeface;
        if (FONT_INCONSOLATA.equals(fontName)) {
            typeface = loadFontSafely(ASSET_INCONSOLATA);
        } else if (FONT_ROBOTO_MONO.equals(fontName)) {
            typeface = loadFontSafely(ASSET_ROBOTO_MONO);
        } else {
            // Lato is also the app theme font, so it is a local resource (res/font/lato.ttf)
            typeface = ResourcesCompat.getFont(c, R.font.lato);
            if (typeface == null) {
                typeface = Typeface.SANS_SERIF;
            }
        }
        bundledFontCache.put(fontName, typeface);
        return typeface;
    }

    /**
     * Defensive font loader that handles case-sensitivity issues across different filesystems.
     * Uses case-insensitive matching to find the font file in one pass.
     *
     * @param fontPath The desired font path (e.g., "font/Lato.ttf" or "font/lato.ttf")
     * @return Typeface object, never null (falls back to SANS_SERIF on failure)
     */
    private Typeface loadFontSafely(String fontPath) {
        // Try original path first (fast path for correct case)
        try {
            return Typeface.createFromAsset(c.getAssets(), fontPath);
        } catch (Exception e) {
            Log.w(TAG, "Font not found at: " + fontPath + ", searching case-insensitively...");
        }

        // Extract directory and filename
        int lastSlash = fontPath.lastIndexOf("/");
        String directory = lastSlash >= 0 ? fontPath.substring(0, lastSlash) : "";
        String targetFilename = lastSlash >= 0 ? fontPath.substring(lastSlash + 1) : fontPath;

        // List all files in the directory and find case-insensitive match
        try {
            String[] files = c.getAssets().list(directory);
            if (files != null) {
                for (String file : files) {
                    if (file.equalsIgnoreCase(targetFilename)) {
                        // Found a case-insensitive match
                        String actualPath = directory.isEmpty() ? file : directory + "/" + file;
                        Typeface typeface = Typeface.createFromAsset(c.getAssets(), actualPath);
                        Log.i(TAG, "Found font '" + targetFilename + "' as '" + file + "' (case mismatch resolved)");
                        return typeface;
                    }
                }
            }
            Log.e(TAG, "Font '" + targetFilename + "' not found in directory '" + directory + "'");
        } catch (Exception e) {
            Log.e(TAG, "Error searching for font: " + fontPath, e);
        }

        // All attempts failed - fall back to system default
        Log.e(TAG, "Could not load font " + fontPath + ". Using system default SANS_SERIF.");
        return Typeface.SANS_SERIF;
    }
}
