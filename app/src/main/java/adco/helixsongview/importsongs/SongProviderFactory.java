package adco.helixsongview.importsongs;

import android.content.Context;
import adco.helixsongview.interfaces.SongProvider;

public class SongProviderFactory {
    public static SongProvider getProvider(String providerName, Context context) {
        switch (providerName) {
            case "UltimateGuitar": return new UltimateGuitarProvider(context);
            case "Chordie": return new ChordieProvider(context);
            case "eChords": return new EChordsProvider(context);
            case "GuitarTabs": return new GuitarTabsProvider(context);
            default: throw new IllegalArgumentException("Unknown provider: " + providerName);
        }
    }
}
