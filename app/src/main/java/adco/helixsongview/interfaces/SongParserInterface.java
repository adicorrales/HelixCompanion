package adco.helixsongview.interfaces;

import adco.helixsongview.songprocessing.Song;

public interface SongParserInterface {
    Song processContent(MainActivityInterface mainActivityInterface, Song targetSong, String html);
}