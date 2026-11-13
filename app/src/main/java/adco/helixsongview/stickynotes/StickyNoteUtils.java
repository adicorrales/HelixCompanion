package adco.helixsongview.stickynotes;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import adco.helixsongview.songprocessing.Song;
import java.util.ArrayList;

public class StickyNoteUtils {
    private final ArrayList<StickyNote> stickyNotes = new ArrayList<>();
    public StickyNoteUtils(Context c) {}
    public void destroyStickyPopUps() {}
    public void prepareStickyPopUps(View v) {}
    public void toggleStickyPopUps(View v) {}
    public void updateStickyAlphas(float alpha) {}
    public void parseAndMigrate(Song s) {}
    public void populateStickyNoteEditorViews(Context c, LinearLayout layout) {}
    public boolean hasStickyOnOverride() { return false; }
    public boolean hasStickyOffOverride() { return false; }
    public ArrayList<StickyNote> getStickyNotes() { return stickyNotes; }
    public void syncStickyNotesToTempSong() {}
    public void updateOverrides(String o) {}
}
