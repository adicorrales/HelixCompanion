package adco.helixcompanion

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

/**
 * Bridges the Matrix mixer settings (stored in [helixDataStore]) with the Song View
 * profile file, which is a SharedPreferences XML.
 *
 * Every exported key is prefixed with [PROFILE_KEY_PREFIX] so ProfileActions can tell
 * Matrix entries apart from the regular Song View preferences it already handles.
 */
object MatrixSettingsProfile {

    private const val TAG = "MatrixSettingsProfile"

    /** Prefix used inside the profile XML to mark a DataStore entry. */
    const val PROFILE_KEY_PREFIX = "helixMatrix_"

    /**
     * Snapshot of every Matrix setting, keyed by its profile key
     * ([PROFILE_KEY_PREFIX] + DataStore key name).
     */
    @JvmStatic
    fun exportEntries(context: Context): Map<String, Any> = runBlocking {
        withContext(Dispatchers.IO) {
            runCatching {
                context.applicationContext.helixDataStore.data.first().asMap()
                    .entries
                    .associate { (key, value) ->
                        PROFILE_KEY_PREFIX + key.name to value
                    }
            }.onFailure {
                Log.e(TAG, "Unable to read the Matrix settings", it)
            }.getOrDefault(emptyMap())
        }
    }

    /**
     * Write the Matrix settings restored from a profile back into [helixDataStore].
     * Keys missing from the profile are left untouched, matching how the Song View
     * preferences are merged when a profile is loaded.
     */
    @JvmStatic
    fun importEntries(context: Context, entries: Map<String, Any>): Boolean {
        if (entries.isEmpty()) return true
        return runBlocking {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.applicationContext.helixDataStore.edit { prefs ->
                        entries.forEach { (profileKey, value) ->
                            if (!profileKey.startsWith(PROFILE_KEY_PREFIX)) return@forEach
                            val key = profileKey.removePrefix(PROFILE_KEY_PREFIX)
                            if (key.isNotEmpty()) {
                                put(prefs, key, value)
                            }
                        }
                    }
                }.onFailure {
                    Log.e(TAG, "Unable to restore the Matrix settings", it)
                }.isSuccess
            }
        }
    }

    private fun put(prefs: MutablePreferences, key: String, value: Any) {
        when (value) {
            is Boolean -> prefs[booleanPreferencesKey(key)] = value
            is Int -> prefs[intPreferencesKey(key)] = value
            is Long -> prefs[longPreferencesKey(key)] = value
            is Float -> prefs[floatPreferencesKey(key)] = value
            is Double -> prefs[doublePreferencesKey(key)] = value
            is String -> prefs[stringPreferencesKey(key)] = value
            else -> Log.w(
                TAG,
                "Skipping unsupported Matrix setting '$key' of type ${value::class.java.name}"
            )
        }
    }
}
