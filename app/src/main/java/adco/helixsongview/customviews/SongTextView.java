package adco.helixsongview.customviews;

import android.content.Context;
import android.os.Build;
import android.text.Layout;
import android.widget.TextView;

/**
 * Lightweight TextView used to build the song body.
 *
 * The song views set every property they need explicitly (typeface, size, colour,
 * gravity, padding and font padding), so none of the AppCompat text machinery
 * (theme style chain, tinting, remote font resolution) is needed here.
 *
 * Creating hundreds of these on every song change is noticeably cheaper than
 * creating AppCompatTextViews, and they are only ever created programmatically
 * (never inflated from a layout).
 */
public class SongTextView extends TextView {
    public SongTextView(Context context) {
        // No defStyleAttr: the song views define their own appearance, so resolving
        // the theme's textViewStyle for every single line is wasted work.
        super(context, null, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // High quality line breaking runs a full paragraph optimisation and the
            // hyphenation dictionary on every measure pass. Song fragments are short
            // and must not be hyphenated, so the simple strategy is both faster and
            // closer to the expected output.
            setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
            setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        }
    }
}
