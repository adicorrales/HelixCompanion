package adco.helixsongview.songprocessing;

public class DrumCalculations {
    public static String getFixedTempoString(String tempo, boolean addBpm) {
        if (tempo == null) return "";
        return tempo.trim();
    }

    public static String getFixedTimeSignatureString(String timesig, boolean addText) {
        if (timesig == null) return "";
        return timesig.trim();
    }

    public static boolean isTempoTimeSigValid(Song song, boolean checkTempo) {
        return song != null && song.getTempo() != null && !song.getTempo().isEmpty();
    }
}
