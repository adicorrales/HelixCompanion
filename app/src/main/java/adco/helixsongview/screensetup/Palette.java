package adco.helixsongview.screensetup;

import android.content.Context;
import android.util.Log;

import androidx.core.content.res.ResourcesCompat;

import adco.helixsongview.R;

// Because Android Day/Night is horrible (and breaks with WebView), we do it manually
// Each view checks this class when it gets created
// It is a pain, but it works

public final class Palette {
    public int background;
    public int onBackground;
    public int surface;
    public int onSurface;
    public int primary;
    public int onPrimary;
    public int secondary;
    public int textColor;
    public int hintColor;
    public int errorColor;
    public int secondaryVariant;
    public int secondaryFixed;
    public int primaryVariant;

    @SuppressWarnings({"unused","FieldCanBeLocal"})
    private final String TAG = "Palette";

    private final String PREF_NAME="theme_choice",
            DISPLAY_MODE="displayMode";
    public boolean dark;
    public boolean amoled;
    public String theme;

    public Palette(Context c) {
        // App chrome follows the fixed Helix Matrix suite palette.
        theme = "dark";
        setDisplayVariables(c);
        setColors(c);
    }

    public void getDefaultTheme(Context c) {
        savePref(c, "dark");
    }

    public void savePref(Context c, String ignoredTheme) {
        this.theme = "dark";
        if (c != null) {
            try {
                c.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putString(DISPLAY_MODE, "dark").apply();
            } catch (Throwable t) {
                // SharedPreferences is not supported in edit/preview mode
            }
        }
        setDisplayVariables(c);
        setColors(c);
    }

    private void setDisplayVariables(Context c) {
        // Theme can be dark, light, custom1, custom2, amoled
        // If set to amoled, then use the amoled colours
        if (theme!=null && !theme.isEmpty()) {
            switch (theme) {
                case "amoled":
                    //Use the AMOLED colours
                    dark = false;
                    amoled = true;
                    break;

                case "dark":
                case "custom1":
                    // Use the dark colours
                    dark = true;
                    amoled = false;
                    break;

                case "light":
                case "custom2":
                    // Use the light colours
                    dark = false;
                    amoled = false;
                    break;
            }
        } else {
            getDefaultTheme(c);
        }
    }

    public String getDefaultTheme() {
        // Returns a basic theme - called on app boot, particularly when no theme has yet been set
        if (amoled) {
            return "amoled";
        } else if (dark) {
            return "dark";
        } else {
            return "light";
        }

    }

    @SuppressWarnings("deprecation")
    public void setColors(Context context) {
        if (context == null) {
            background = 0xFF121418;
            onBackground = 0xFFE2E8F0;
            surface = 0xFF181B20;
            onSurface = 0xFFE2E8F0;
            primary = 0xFF181B20;
            primaryVariant = 0xFF21252E;
            onPrimary = 0xFFE2E8F0;
            textColor = 0xFFE2E8F0;
            hintColor = 0xFFA0AEC0;
            secondary = 0xFF20A050;
            secondaryVariant = 0xFF286E42;
            secondaryFixed = 0xFF16281E;
            errorColor = 0xFFE53E3E;
            return;
        }
        try {
            background = ResourcesCompat.getColor(context.getResources(), amoled ? R.color.amoled_primary : dark ? R.color.dark_primary : R.color.light_primary, null);
            onBackground = ResourcesCompat.getColor(context.getResources(), amoled ? R.color.amoled_color : dark ? R.color.dark_color : R.color.light_color, null);
            surface = ResourcesCompat.getColor(context.getResources(), amoled ? R.color.amoled_surface : dark ? R.color.dark_surface : R.color.light_surface, null);
            onSurface = ResourcesCompat.getColor(context.getResources(), amoled ? R.color.amoled_color : dark ? R.color.dark_color : R.color.light_color, null);
            primary = ResourcesCompat.getColor(context.getResources(), amoled ? R.color.amoled_primary : dark ? R.color.dark_primary : R.color.light_primary, null);
            primaryVariant = ResourcesCompat.getColor(context.getResources(), amoled ? R.color.amoled_primary_variant : dark ? R.color.dark_primary_variant : R.color.light_primary_variant, null);
            onPrimary = ResourcesCompat.getColor(context.getResources(), amoled ? R.color.amoled_color : dark ? R.color.dark_color : R.color.light_color, null);
            textColor = ResourcesCompat.getColor(context.getResources(), amoled ? R.color.amoled_color : dark ? R.color.dark_color : R.color.light_color, null);
            hintColor = ResourcesCompat.getColor(context.getResources(), amoled ? R.color.amoled_hint : dark ? R.color.dark_hint : R.color.light_hint, null);
            secondary = ResourcesCompat.getColor(context.getResources(), amoled ? R.color.amoled_secondary : dark ? R.color.dark_secondary : R.color.light_secondary, null);
            secondaryVariant = ResourcesCompat.getColor(context.getResources(), amoled ? R.color.amoled_secondary_variant : dark ? R.color.dark_secondary_variant : R.color.light_secondary_variant, null);
            secondaryFixed = ResourcesCompat.getColor(context.getResources(), amoled ? R.color.amoled_secondary_fixed : dark ? R.color.dark_secondary_fixed : R.color.light_secondary_fixed, null);
            errorColor = ResourcesCompat.getColor(context.getResources(), amoled ? R.color.amoled_error : dark ? R.color.dark_error : R.color.light_error, null);
        } catch (Throwable t) {
            try {
                // Catches both missing method Errors on API 22 and any runtime Exceptions cleanly
                background = context.getResources().getColor(amoled ? R.color.amoled_primary : dark ? R.color.dark_primary : R.color.light_primary);
                onBackground = context.getResources().getColor(amoled ? R.color.amoled_color : dark ? R.color.dark_color : R.color.light_color);
                surface = context.getResources().getColor(amoled ? R.color.amoled_surface : dark ? R.color.dark_surface : R.color.light_surface);
                onSurface = context.getResources().getColor(amoled ? R.color.amoled_color : dark ? R.color.dark_color : R.color.light_color);
                primary = context.getResources().getColor(amoled ? R.color.amoled_primary : dark ? R.color.dark_primary : R.color.light_primary);
                primaryVariant = context.getResources().getColor(amoled ? R.color.amoled_primary_variant : dark ? R.color.dark_primary_variant : R.color.light_primary_variant);
                onPrimary = context.getResources().getColor(amoled ? R.color.amoled_color : dark ? R.color.dark_color : R.color.light_color);
                textColor = context.getResources().getColor(amoled ? R.color.amoled_color : dark ? R.color.dark_color : R.color.light_color);
                hintColor = context.getResources().getColor(amoled ? R.color.amoled_hint : dark ? R.color.dark_hint : R.color.light_hint);
                secondary = context.getResources().getColor(amoled ? R.color.amoled_secondary : dark ? R.color.dark_secondary : R.color.light_secondary);
                secondaryVariant = context.getResources().getColor(amoled ? R.color.amoled_secondary_variant : dark ? R.color.dark_secondary_variant : R.color.light_secondary_variant);
                secondaryFixed = context.getResources().getColor(amoled ? R.color.amoled_secondary_fixed : dark ? R.color.dark_secondary_fixed : R.color.light_secondary_fixed);
                errorColor = context.getResources().getColor(amoled ? R.color.amoled_error : dark ? R.color.dark_error : R.color.light_error);
            } catch (Throwable t2) {
                background = 0xFF121418;
                onBackground = 0xFFE2E8F0;
                surface = 0xFF181B20;
                onSurface = 0xFFE2E8F0;
                primary = 0xFF181B20;
                primaryVariant = 0xFF21252E;
                onPrimary = 0xFFE2E8F0;
                textColor = 0xFFE2E8F0;
                hintColor = 0xFFA0AEC0;
                secondary = 0xFF20A050;
                secondaryVariant = 0xFF286E42;
                secondaryFixed = 0xFF16281E;
                errorColor = 0xFFE53E3E;
            }
        }
    }

}
