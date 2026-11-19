# Checklist de Funcionalidad — Helix Song View

> **Propósito**: inventario completo de funcionalidad para decidir qué **desaparece** (defeaturing) antes de migrar su paquete a Kotlin.
>
> **Cómo usarlo**: marca cada ítem con una de estas decisiones:
> - ✅ **Mantener** (se migra)
> - ❌ **Eliminar** (se retira; no migrar el paquete asociado)
> - ⏳ **Evaluar** (pendiente de decidir)
> - ? **Función desconocida**  (el usuario no sabe que hace)
>
> La columna "Ubicación" indica el paquete/archivo para localizarlo. Retira primero, migra después.

---

## 1. Importación de canciones (formatos de archivo)

- [✅ ] ChordPro (`.chordpro`, `.chopro`, `.cho`, `.pro`) — `songprocessing/ConvertChoPro`
- [✅ ] OnSong (`.onsong`) — `songprocessing/ConvertOnSong`
- [⏳ ] OpenSong (`.osb`, `.ost`, `.ossb`, `.osts`) — `songprocessing/ConvertOnSong`, `export`
- [✅ ] Texto plano / Word (`.txt`, `.docx` vía Mammoth) — `songprocessing/ConvertWord`, `ConvertTextSong`
- [✅ ] Backup (`.backup`) — `backupandrestore`
- [⏳ ] MusicXML — `openchords/OpenChordsMusicXML`
- [❌ ] PDF (importación) — `pdf/MakePDF`, OCR
- [❌] Imágenes (OCR vía ML Kit) — `importsongs`, ML Kit

## 2. Importación online (scraping)

- [✅ ] Chordie — `importsongs/Chordie`, `ChordieProvider`
- [✅ ] Ultimate Guitar — `importsongs/UltimateGuitar`, `UltimateGuitarProvider`
- [✅ ] E-Chords — `importsongs/EChords`, `EChordsProvider`
- [✅ ] GuitarTabs — `importsongs/GuitarTabs`, `GuitarTabsProvider`
- [✅ ] Chordinator — `importsongs/Chordinator`
- [✅ ] Vista previa web (WebView + JS bridge) — `importsongs/PreviewPopUp`, `MyJSInterface`
- [❌ ] Importación masiva (bulk) — `importsongs/ImportBulkFragment`

## 3. Exportación

- [❌ ] PDF (generar) — `pdf/MakePDF`, `export/PrepareFormats`
- [❌ ] Impresión multipágina — `export/MultipagePrinterAdapter`
- [❌ ] Bundle OSB / OSBS — `export/OpenSongSetBundle`
- [❌ ] Formatos OpenSong — `export/ExportActions`, `ExportFragment`

## 4. Gestión de canciones

- [✅ ] Lista e índice de canciones — `songmenu/*`
- [✅ ] Búsqueda de canciones — `songmenu/SongListBuildIndex`, `appdata/SearchMenuFragment`
- [✅ ] Edición de canciones (letra/acordes/metadata) — `songprocessing/EditSongFragment*`
- [✅ ] Crear canción + BPM — `songprocessing/CreateSongBottomSheet`, `GetBPMBottomSheet`
- [✅ ] Transposición — `chords/Transpose`, `TransposeBottomSheet`
- [⏳ ] Directorio de acordes / digitación — `chords/ChordDirectory`, `ChordFingeringBottomSheet`
- [✅ ] Formato de visualización de acordes — `chords/ChordFormatFragment`, `ChordDisplayProcessing`
- [⏳ ] Acordes personalizados — `chords/CustomChordsFragment`
- [✅ ] Procesado/renderizado de canción — `songprocessing/ProcessSong`

## 5. Sets (listas de canciones)

- [✅ ] Crear/editar sets — `setprocessing/SetObject`, `SetActions`
- [✅ ] Menú de sets — `setmenu/*`
- [✅ ] Set en línea (inline) — `setmenu/InlineSetFragment`
- [✅ ] Importar sets — `setprocessing/ImportSet*`
- [✅ ] Backup/restore de sets — `setprocessing/BackupRestoreSetsFragment`

## 6. OpenChords (sincronización/servicio)

- [❌ ] API OpenChords — `openchords/OpenChordsAPI`
- [❌ ] Login/autenticación — `openchords/AuthInterceptor`, `OpenChordsLogin*`
- [❌ ] Carpetas y permisos — `openchords/OpenChordsFolder*`
- [❌ ] Resolución de conflictos — `openchords/OpenChordsConflict*`
- [❌ ] Sincronización de canciones y sets — `openchords/OpenChordsFragment`

## 7. Presentación / Performance

- [✅ ] Modo performance — `performance/PerformanceFragment`
- [⏳ ] Autoscroll — `autoscroll/*`
- [✅ ] Backing tracks / pads — `backingtracks/*`
- [✅ ] Gestos (swipes) — `controls/Swipes`, `SwipeFragment`
- [⏳ ] Gestos personalizados — `controls/Gestures`, `GestureListener`, `GesturesFragment`
- [⏳ ] Zonas activas (hot zones) — `controls/HotZones`, `HotZonesSettingsFragment`
- [✅ ] Botones de página — `controls/PageButtons`, `PageButtonFragment`
- [✅ ] Display anterior/siguiente — `performance/DisplayPrevNext`
- [✅ ] Cast (Chromecast) — `mediarouter`, `play-services-cast`
- [❌ ] Nearby Connections (dispositivos cercanos) — `play-services-nearby`

## 8. MIDI

- [✅ ] MIDI USB — `midi/Midi`, `MidiInputReceiver`
- [✅ ] MIDI Bluetooth LE (GATT) — `midi/DirectMidiGattCallback`
- [❌ ] Reloj MIDI (tempo) — `midi/MidiClock`
- [✅ ] Mensajes/adaptador MIDI — `midi/MidiMessagesAdapter`, `MidiItemTouchHelper`
- [✅ ] Fragmento/opciones MIDI — `midi/MidiFragment`, `MidiOptionsFragment`
- [❌ ] Taquigrafía MIDI — `midi/ShortHandMidi`

## 9. Pedales / Accesibilidad

- [✅ ] Acciones de pedal — `controls/PedalActions`
- [✅ ] Fragmento de pedales — `controls/PedalsFragment`
- [✅ ] Overlay de depuración de pedal — `controls/PedalDebugOverlay`
- [✅ ] Servicio de accesibilidad de pedal — `appdata/PedalAccessibilityService`

## 10. Servidor web (Ktor)

- [❌ ] Servidor local — `webserver/KtorServer.kt`, `WebServer`
- [❌ ] HTML del servidor — `webserver/CreateHTML`
- [❌ ] WebSocket de actualización en vivo — `webserver/KtorServer.kt`
- [❌ ] API remota (SongWebEditor / PWA) — `webserver/KtorServer.kt` (api/song, api/list, api/save, api/remote)
- [❌ ] Guardado remoto — `webserver/RemoteSaveBottomSheet`
- [❌ ] Identificador de canción (JSON) — `webserver/SongIdentifier`
- [❌ ] Host WiFi local — `webserver/LocalWiFiHost`

## 11. Personalización / Ajustes de pantalla

- [⏳ ] Temas y colores — `screensetup/ThemeKeeper`, `ThemeColors`, `ThemeSetupFragment`, `ChooseColorBottomSheet`
- [⏳ ] Fuentes (incl. Google Fonts) — `screensetup/FontSetupFragment`, `FontSetupPreviewFragment`, `appdata/MyFonts`
- [✅ ] Escalado de pantalla — `screensetup/DisplayScalingFragment`
- [✅ ] Márgenes — `screensetup/MarginsFragment`
- [✅ ] Menú de pantalla — `screensetup/DisplayMenuFragment`, `DisplayExtraFragment`
- [✅ ] Barra de acción (action bar) — `screensetup/ActionBarSettingsFragment`
- [✅ ] Botones de scroll — `screensetup/ScrollButtons`
- [?] Bandera de ventana — `screensetup/WindowFlags`
- [✅ ] Estado de batería — `screensetup/BatteryStatus`
- [❌ ] Idioma (~20 idiomas) — `appdata/LanguageFragment`, `res/values-*`
- [✅ ] Preferencias — `preferences/Preferences`, `HelixSettingsFragment`
- [❌ ] Tutorial ShowCase (destacar features) — `animation/ShowCase`, MaterialShowcaseView

## 12. Analytics

- [✅] Registro de uso (SQLite) — `analytics/*`, `sqlite/AnalyticsSQLiteHelper`
- [✅] Bottom sheet de analytics — `analytics/AnalyticsBottomSheet`
- [✅] Orden de analytics — `analytics/SortMode`

## 13. PDF / OCR / Escaneo

- [❌ ] Generar PDF — `pdf/MakePDF`
- [❌ ] Escaneo de documentos — ML Kit document scanner
- [❌ ] OCR de texto (PDF/cámara) — ML Kit text recognition
- [❌ ] Crop/rotación de imagen — `com.vanniktech:android-image-cropper`

## 14. Imágenes / Slides

- [❌ ] Slides de imágenes — `customslides/*`
- [❌ ] Carga de imágenes (Glide) — `screensetup/MyGlideModule`
- [❌ ] Datos EXIF (orientación cámara) — `exifinterface`

## 15. Variaciones (ABC / SVG)

- [❌ ] Variaciones / notación ABC → SVG — `variations/Variations`, `androidsvg`

## 16. Enlaces

- [✅] Fragmento de enlaces — `links/LinksFragment`
- [?] Bottom sheet de enlaces — `links/LinksBottomSheet`

## 17. Compartir / Conectividad

- [❌ ] Compartir canciones — `filemanagement`, `export`
- [❌ ] NFC — permiso `NFC`, `uses-feature nfc`
- [❌ ] Códigos QR — `com.google.zxing:core`

## 18. Almacenamiento / SAF

- [✅ ] Storage Access Framework — `filemanagement/StorageAccess`
- [✅ ] Gestión de carpetas — `filemanagement/*Management*`
- [✅ ] Ubicación de almacenamiento (KitKat) — `filemanagement/KitKatFolderChooseFragment`
- [ ✅] Mover contenido — `filemanagement/MoveContentFragment`

## 19. Backup / Restore

- [✅ ] Backup OSB — `backupandrestore/BackupOSBFragment`
- [✅ ] Importar desde iOS — `backupandrestore/ImportIOSFragment`
- [✅ ] Importar OSB — `backupandrestore/ImportOSBFragment`

## 20. Deeplinks / Apertura de archivos

- [✅] Esquema `opensongapp://` — `AndroidManifest.xml` intent-filter
- [✅] Apertura de `.osb/.chordpro/.onsong/...` desde otras apps — `AndroidManifest.xml` intent-filters

## 21. Wear OS companion

- [❌] App de acompañamiento Wear — referencia `com.google.android.apps.wear.companion`, `values/wear.xml`

## 22. Integraciones Play / Dispositivo

- [❌ ] Age verification (Texas) — `play-services age-signals`
- [?] Actualizaciones in-app — `play app-update`
- [❌ ] Reseñas in-app — `play review`
- [?] Feature delivery (recursos) — `play feature-delivery`
- [✅ ] Cast framework — `play-services-cast-framework`

## 23. Utilidades transversales

- [✅] Comprobación de internet — `appdata/CheckInternet`
- [❌ ] Herramientas de tiempo — `appdata/TimeTools`
- [❌ ] Locale/fecha fija — `appdata/FixLocale`
- [?] Comprobaciones de alertas — `appdata/AlertChecks`
- [❌ ] Diálogos personalizados — `appdata/CustomAlertDialog`
- [❌ ] Compartir logs — `appdata/ShareLogsFragment`
- [❌ ] Comparador de versiones — `appdata/VersionNumber`
- [✅] Número de versión / build — `app/build.gradle` (versionCode 508, versionName 7.0.3)

---

## Notas de decisión (rellenar según avances)

| # | Feature | Decisión (✅/❌/⏳) | Justificación |
|---|---|---|---|
| 1 |  |  |  |
| 2 |  |  |  |
| 3 |  |  |  |

> **Sugerencia**: los candidatos naturales a ❌ (por tamaño/stub/obsolescencia) son `pdf/*` (70 líneas, posible stub), los scrapers de `importsongs` (frágiles y dependientes de sitios externos), y `PedalDebugOverlay` (tooling de depuración). Revisa primero esos paquetes.
