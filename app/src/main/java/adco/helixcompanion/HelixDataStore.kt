package adco.helixcompanion

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Process wide settings store.
 *
 * It lives in the companion package so the Song View, the Matrix mixer and the
 * shared MIDI layer can read and write the same values without depending on
 * each other. There must only ever be one instance of this delegate: DataStore
 * refuses to open the same file twice in one process.
 */
val Context.helixDataStore: DataStore<Preferences> by preferencesDataStore(name = "helix_midi_mixer_settings")
