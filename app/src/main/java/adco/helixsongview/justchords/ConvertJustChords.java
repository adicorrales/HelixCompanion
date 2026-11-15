package adco.helixsongview.justchords;

import android.content.Context;
import android.net.Uri;
import adco.helixsongview.songprocessing.Song;

public class ConvertJustChords {
    public ConvertJustChords(Context c) {}
    public void resetVariables() {}
    public String getExtension() { return ".justchords"; }
    public Uri buildJustChordsSingleSong(Song thisSong) { return null; }
    public String getJustChordsLyrics(Song thisSong) { return ""; }
    public Uri buildJustChordsSet(String setname) { return null; }
    public void addOpenSongToArray(Song song) {}
    public void createSongsFromImportedSet() {}
    public JustChordsObject getJustChordsObjectFromImportUri() { return null; }
    public JustChordsObject getJustChordsObjectFromUri(Uri uri) { return null; }
    public JustChordsSongObject getJustChordsSongObject(JustChordsObject obj, int itemNumber) { return null; }
    public Song getOpenSongFromJustChordsSong(JustChordsSongObject obj) { return null; }
    public String getOpenSongLyrics(String rawData) { return ""; }
}
