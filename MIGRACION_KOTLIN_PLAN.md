# Plan de Migración a Kotlin — Helix Companion (v2, revisado)

> **Estado**: reemplaza al artefacto de plan original. Incorpora las observaciones del inventario real y reorganiza el trabajo en paquetes de 1–2 horas diarias, de menor a mayor riesgo.

---

## 0. Cruce con el checklist de defeaturing (decisiones del usuario)

> Esta sección **prevalece** sobre las tablas de paquetes de las secciones 3. Ajusta el alcance según lo marcado en `CHECKLIST_FUNCIONALIDAD.md`.

> **Estado (Nov 2026):** eliminación aplicada y compilando ✅. Se retiraron `pdf`, `webserver` (incluido `KtorServer.kt`), `customslides`, `filemanagement/ExportSongListBottomSheet`, y los archivos puros de export (`ExportFragment`, `PrepareFormats`, `MultipagePrinterAdapter`). Se **conservaron** `export/ExportActions.java` (backup + share) y `export/OpenSongSetBundle.java` (import de set bundle), y el paquete completo `variations`, según tus decisiones ✅. `variations` se mantiene intacto.

### 0.1 Paquetes completos a ELIMINAR (no migrar)

| Paquete | Archivos | Líneas | WP afectado |
|---|---|---|---|
| `pdf` | 4 | 70 | **WP-05 eliminado** |
| `export` | 5 | 2.661 | **WP-39 y WP-40 eliminados** |
| `webserver` | 7 | 1.736 | **WP-35 y WP-36 eliminados** (incluye `KtorServer.kt`, ya Kotlin) |
| `customslides` | 3 | 250 | WP-04 (parte) |
| `variations` | 1 | 316 | WP-03 (parte) |

### 0.2 Archivos sueltos a ELIMINAR (ajustan el WP, no lo cancelan)

| Archivo | WP afectado |
|---|---|
| `animation/ShowCase` (tutorial) | WP-03 |
| `importsongs/ImportBulkFragment` (bulk) | WP-43 |
| `midi/MidiClock`, `midi/ShortHandMidi` | WP-37 |
| `appdata/TimeTools`, `CustomAlertDialog`, `ShareLogsFragment`, `VersionNumber`, `LanguageFragment` | WP-12/13 |
| `screensetup/MyGlideModule` | WP-09 |

### 0.3 Integraciones/dependencias a ELIMINAR (limpieza en `build.gradle`)

- `play-services-nearby` (Nearby), `mlkit` text-recognition + document-scanner, `android-image-cropper`, `exifinterface`, `zxing` (QR), `age-signals`, `play review`, `androidsvg` (ABC), `ktor-*` + `websockets`/`cors`/`netty` (webserver).
- **Verificar antes de quitar**: `glide` (puede usarse más allá de slides), `mammoth` (Word, ⏳ pendiente), `kotlinx-coroutines` (¿se usa fuera de Ktor?), `play-services-cast` (Cast sigue ✅).

### 0.4 Decisiones PENDIENTES (⏳) — bloquean estos WPs

| Tema | WP(s) bloqueados |
|---|---|
| OpenSong `.osb/.ost/.ossb/.osts` | WP-23, WP-47 |
| Texto plano / Word (`.txt`/`.docx`) | WP-22 |
| Backup `.backup` | WP-47 |
| MusicXML | WP-45/46 |
| Directorio de acordes / digitación (`ChordDirectory`) | WP-32 |
| Acordes personalizados | WP-31 |
| OpenChords (completo) | WP-44, 45, 46 |
| Autoscroll | WP-33 |
| Gestos personalizados + Hot zones | WP-54 |
| Temas/colores + Fuentes (Google Fonts) | WP-09/10 |
| Estado de batería (`BatteryStatus`) | WP-09 |

### 0.5 Ítems marcados `?` — qué son realmente (para decidir)

| Ítem | Qué hace | Recomendación |
|---|---|---|
| `screensetup/WindowFlags` | **Crítico**: edge-to-edge, insets, notch/cutout, modo inmersivo, esquinas redondeadas, márgenes y altura de teclado (701 líneas). No es una "bandera" trivial. | ✅ Mantener |
| `analytics/*` (SortMode, AnalyticsBottomSheet, SQLite) | Registra reproducciones y **alimenta el orden de canciones por "popularidad/recientes/reproducciones/sets"**. Eliminarlo quita ese criterio de orden. | ⏳ Decidir (afecta orden de canciones) |
| `links/*` | Enlaces por canción (YouTube/web/audio/archivo) con previsualización de audio y "usar audio como pad". Feature de usuario. | ⏳ Decidir |
| Esquema `opensongapp://` + apertura de archivos | Cómo otras apps y el navegador abren canciones/archivos en Helix. | ⏳ Decidir |
| `appdata/CheckInternet` | Verifica conexión (usada por importación online y pref "solo WiFi"). | ✅ Mantener si quedan scrapers |
| `appdata/FixLocale` | Idioma de la app **y RTL** (muestra letras hebreas de derecha a izquierda). Aunque elimines traducciones, el RTL de letras sigue siendo útil. | ⏳ Decidir (mantener lógica RTL) |
| `appdata/AlertChecks` | Avisos: Play Services, "app actualizada", recordatorio de backup, age-verification Texas. | ⏳ Decidir (reducir, no todo) |
| `app-update`, `feature-delivery` | Actualización in-app y entrega de recursos de Play. | ⏳ Decidir |
| Número de versión/build | `versionCode`/`versionName`: es metadata, no feature. | ✅ Mantener (no migra, es build.gradle) |

### 0.6 Impacto transversal al retirar (encoge los WPs grandes)

- `MainActivity` (4.702) y `MainActivityInterface` (313): pierden `webServer`, `gson` (verificar si se usa en otro lado), export, compartir, NFC, QR, Nearby, Wear. → WP-59/60/61 resultan **más pequeños**.
- `navigation.xml`, `menu/mainactivitymenu.xml`, `strings.xml` y `res/values-*` (20 idiomas): destinos y recursos a retirar.
- `AndroidManifest.xml`: quitar permisos/features/intent-filters huérfanos (NFC, Nearby, cámara, micrófono si no se usan, deeplinks si se descartan).

### 0.7 WPs resultantes

- **Eliminados**: WP-05, WP-35, WP-36, WP-39, WP-40.
- **Reducidos**: WP-03 (sin ShowCase ni Variations), WP-04 (solo analytics), WP-09 (sin MyGlideModule), WP-12/13 (sin TimeTools/CustomAlertDialog/ShareLogs/VersionNumber/LanguageFragment), WP-31 (sin CustomChordsFragment), WP-37 (sin MidiClock/ShortHandMidi), WP-43 (sin ImportBulkFragment).
- **Bloqueados por ⏳**: WP-22, WP-23, WP-31(parcial), WP-32, WP-33, WP-44, WP-45, WP-46, WP-47(parcial), WP-54(parcial).
- **Sin cambio**: el resto (WP-00, 01, 02, 06, 07, 08, 10, 11, 14, 15, 16, 17, 18, 19, 20, 21, 24–30, 34, 38, 41, 42, 48–58, 59–61, 62, 63).

---

## 1. Observaciones que corrigen el plan original

1. **El alcance real es mayor de lo declarado.** El plan original decía "~215 archivos / ~50.000 líneas". El inventario real es **328 archivos Java / ~91.700 líneas**. Distribución:

| Paquete | Archivos | Líneas | Mayor archivo |
|---|---|---|---|
| interfaces | 12 | 426 | MainActivityInterface (313) |
| variations | 1 | 316 | Variations (316) |
| animation | 2 | 151 | ShowCase (107) |
| analytics | 5 | 301 | AnalyticsBottomSheet (190) |
| customslides | 3 | 250 | ImageSlideAdapter (218) |
| pdf | 4 | 70 | MakePDF (35) |
| links | 2 | 422 | LinksBottomSheet (345) |
| preferences | 9 | 2.110 | Preferences (745) |
| screensetup | 18 | 4.609 | ThemeColors (1.039) |
| appdata | 20 | 3.450 | SearchSettingsAdapter (1.167) |
| sqlite | 7 | 2.613 | CommonSQL (1.362) |
| filemanagement | 11 | 5.632 | StorageAccess (2.789) |
| songprocessing | 26 | 11.313 | ProcessSong (4.807) |
| setprocessing | 17 | 4.590 | SetActions (1.964) |
| chords | 8 | 7.477 | ChordDirectory (3.827) |
| autoscroll | 4 | 879 | Autoscroll (545) |
| backingtracks | 5 | 1.311 | backingTrack (629) |
| midi | 17 | 5.646 | Midi (1.662) |
| webserver | 7 | 1.736 | CreateHTML (579) |
| export | 5 | 2.661 | ExportFragment (1.444) |
| importsongs | 21 | 4.985 | ImportOnlineFragment (1.256) |
| openchords | 29 | 4.656 | OpenChordsAPI (2.538) |
| backupandrestore | 3 | 1.405 | ImportOSBFragment (622) |
| customviews | 50 | 9.004 | MyToolbar (777) |
| controls | 14 | 3.332 | PedalActions (722) |
| songmenu | 11 | 2.343 | SongMenuFragment (887) |
| setmenu | 12 | 2.367 | SetAdapter (503) |
| performance | 3 | 2.971 | PerformanceFragment (1.601) |
| (raíz) MainActivity | 1 | 4.702 | MainActivity (4.702) |

2. **No hay tests.** `app/src/test/` está vacío y `app/src/androidTest/` solo tiene dos placeholders. El código Java es la única especificación de comportamiento. → Cada paquete de trabajo debe preservar comportamiento, no "mejorarlo".

3. **Kotlin ya está parcialmente presente.** Existe `webserver/KtorServer.kt` (~780 líneas) que ya consume clases Java (`MainActivity.gson`, `MainActivityInterface`, `Song`, `ShareableObject`, `CreateHTML`, `SongIdentifier`, `RemoteSaveBottomSheet`). Esto confirma que la interop Java↔Kotlin ya funciona en el proyecto y que **el webserver ya tiene un núcleo Kotlin**.

4. **Configuración de toolchain a verificar.** El `kotlin-gradle-plugin` 2.4.0 está en el classpath raíz, pero `org.jetbrains.kotlin.android` no aparece en el bloque `plugins` de `app/build.gradle`. Como `KtorServer.kt` compila, hay que **verificar cómo se aplica hoy el plugin Kotlin** y normalizarlo (posiblemente AGP 9 con Kotlin integrado). Además hay una tensión de versiones: `kotlin-stdlib` fijado en **2.2.10** (comentario: *"KEEP AT 2.2.10 — crashes FireHD if upgraded"*) vs plugin 2.4.0. No tocar sin verificar en FireHD.

5. **Riesgos de dominio concentrados en pocos archivos.** ~20 archivos (>800 líneas) suman ~40.000 líneas y concentran el riesgo: parsers de formatos (`ProcessSong`, `Convert*`), transposición/acordes (`ChordDirectory`, `Transpose`), almacenamiento (`StorageAccess`), y `MainActivity`. Migrarlos **al final**, cuando sus dependencias ya sean Kotlin, minimiza el retrabajo.

---

## 2. Principio de ordenamiento

**Leaf-first (hojas primero) + quick wins**: migrar primero lo que (a) no depende de nada del proyecto, (b) es pequeño y mecánico, (c) no es funcionalidad principal. Así, cuando lleguemos a los archivos grandes, la mayoría de sus referencias ya serán Kotlin y apenas habrá que tocarlas.

- **Regla de "hecho" por paquete**: (1) convertir a `.kt`, (2) borrar el `.java`, (3) `./gradlew app:assembleDebug` en verde, (4) sin errores nuevos de lint en los archivos tocados.
- **Regla de interop**: al convertir una clase que otros aún usan desde Java, mantener la API compatible (`@JvmOverloads`, `@JvmStatic`, evitar `val` expuesto donde Java espera getters, revisar nulabilidad en interfaces).
- **Regla de safety net**: el `.java` borrado puede quedar como referencia en git; nunca se "mejora" lógica durante la migración. Los cambios de comportamiento van en el track de *defeaturing*, no aquí.

---

## 3. Paquetes de trabajo (1–2 h cada uno)

> Cada paquete = lista concreta de archivos + verificación por compilación. Estimación total: **~45–55 paquetes ≈ 70–90 horas** (a 1–2 h/día ≈ 2–4 meses).

### Etapa 0 — Habilitación (prerequisito, 1 paquete)

**WP-00 · Toolchain Kotlin.** Verificar/normalizar aplicación del plugin Kotlin en `app/build.gradle`; cambiar `androidx.navigation.safeargs` → `androidx.navigation.safeargs.kotlin`; confirmar que `assembleDebug` compila con `KtorServer.kt` incluido. **No** tocar `kotlin-stdlib` 2.2.10. *(~1 h)*

### Etapa A — Hojas triviales: contratos y modelos sin dependencias

| WP | Contenido | Líneas | Esfuerzo |
|---|---|---|---|
| WP-01 | `interfaces/*` (12 interfaces: SongProvider, ActionInterface, DisplayInterface, RecyclerInterface, RetrofitInterface, SongParserInterface, DialogReturnInterface, SetItemTouchInterface, SwipeDrawingInterface, MidiItemTouchInterface, EditSongFragmentInterface) — **excepto** MainActivityInterface | ~113 | 1 h |
| WP-02 | `interfaces/MainActivityInterface` (313 líneas; revisar nulabilidad, es la interfaz más referenciada) | 313 | 1 h |
| WP-03 | `variations/Variations` + `animation/CustomAnimation`, `animation/ShowCase` | 457 | 1 h |
| WP-04 | `customslides/*` (3) + `analytics/*` (5) | 551 | 1–2 h |

### Etapa B — Utilidades y configuración de UI (riesgo bajo-medio)

| WP | Contenido | Líneas | Esfuerzo |
|---|---|---|---|
| WP-05 | `pdf/*` (4, probable stubs — confirmar y marcar para *defeaturing*) | 70 | 0.5 h |
| WP-06 | `links/*` (2) | 422 | 1 h |
| WP-07 | `preferences`: AppPermissions, ProfileActions, SettingsCategories, Preferences | ~1.200 | 1–2 h |
| WP-08 | `preferences`: HelixSettingsFragment, ProfileFragment, ProfileBottomSheet, TextInputBottomSheet, AreYouSureBottomSheet | ~900 | 1–2 h |
| WP-09 | `screensetup` helpers: ScrollButtons, MyGlideModule, ThemeKeeper, ThemeColors, ShowToast, WindowFlags, BatteryStatus, Palette | ~2.300 | 2 h |
| WP-10 | `screensetup` fragments (parte 1): DisplayMenuFragment, FontSetupFragment, ThemeSetupFragment, MarginsFragment | ~1.300 | 1–2 h |
| WP-11 | `screensetup` fragments (parte 2): DisplayExtraFragment, MenuSettingsFragment, DisplayScalingFragment, FontSetupPreviewFragment, ActionBarSettingsFragment, ChooseColorBottomSheet | ~1.000 | 1–2 h |
| WP-12 | `appdata` modelos/utils: SettingItem, VersionNumber, CheckInternet, TimeTools, FixLocale, AlertChecks, MyFonts, CustomAlertDialog, PedalAccessibilityService, ShareLogsFragment | ~1.800 | 2 h |
| WP-13 | `appdata` UI (parte 1): SearchSettingsAdapter, SearchSettingsViewHolder, BootUpFragment, LanguageFragment, ModeFragment | ~1.200 | 1–2 h |
| WP-14 | `appdata` UI (parte 2): AlertInfoBottomSheet, BootUpIndexBottomSheet, InformationBottomSheet, AboutAppFragment, SearchMenuFragment | ~450 | 1 h |

### Etapa C — Persistencia (riesgo medio, alto valor)

| WP | Contenido | Líneas | Esfuerzo |
|---|---|---|---|
| WP-15 | `sqlite`: ShareableObject, SQLiteHelper, AnalyticsSQLiteHelper, NonOpenSongSQLiteHelper | ~700 | 1–2 h |
| WP-16 | `sqlite`: SQLite, SongsDatabase | ~550 | 1 h |
| WP-17 | `sqlite`: CommonSQL (1.362; `.use {}` para Cursores) | 1.362 | 1–2 h |
| WP-18 | `filemanagement`: SaveSong, LoadSong, SetStorageBottomSheet, StorageOptionsFragment, KitKatFolderChooseFragment | ~1.500 | 2 h |
| WP-19 | `filemanagement`: StorageManagementFragment, FolderManagementBottomSheet, ExportSongListBottomSheet, SetStorageLocationFragment, MoveContentFragment | ~1.300 | 1–2 h |
| WP-20 | `filemanagement`: StorageAccess (2.789; partir internamente en trozos) | 2.789 | 2–3 h (2 sesiones) |

### Etapa D — Dominio (riesgo medio-alto, máximo valor)

| WP | Contenido | Líneas | Esfuerzo |
|---|---|---|---|
| WP-21 | `songprocessing`: `Song` → `data class` (contrato Gson: `@SerializedName` + defaults) | ~500 | 1 h |
| WP-22 | `songprocessing` conversores: ConvertWord, ConvertTextSong | ~600 | 1 h |
| WP-23 | `songprocessing` conversores: ConvertOnSong, ConvertChoPro | ~800 | 1–2 h |
| WP-24 | `songprocessing`: ProcessSong (parte 1) | ~1.600 | 2 h |
| WP-25 | `songprocessing`: ProcessSong (parte 2) | ~1.600 | 2 h |
| WP-26 | `songprocessing`: ProcessSong (parte 3) + verificación | ~1.600 | 2 h |
| WP-27 | `songprocessing` edición: GetBPMBottomSheet, CreateSongBottomSheet, EditSongFragment* | ~1.500 | 2 h |
| WP-28 | `setprocessing` modelos: FoundSet, SetObject, SetSlideObject, SetSlideGroupObject, CurrentSet | ~1.100 | 1–2 h |
| WP-29 | `setprocessing`: SetActions (1.964) | 1.964 | 2 h |
| WP-30 | `setprocessing` adapters/fragments: SetManageAdapter, SetManageFragment, ImportSetItemAdapter, ImportSetBundleFragment, BackupRestoreSetsFragment | ~1.500 | 2 h |
| WP-31 | `chords`: Transpose, ChordDisplayProcessing, ChordFingeringBottomSheet, ChordSettingsFragment, ChordFormatFragment, TransposeBottomSheet, CustomChordsFragment | ~3.650 | 3 h (2 sesiones) |
| WP-32 | `chords`: ChordDirectory (3.827) | 3.827 | 2–3 h (2 sesiones) |

### Etapa E — Periféricos

| WP | Contenido | Líneas | Esfuerzo |
|---|---|---|---|
| WP-33 | `autoscroll/*` (4) | 879 | 1–2 h |
| WP-34 | `backingtracks/*` (5) | 1.311 | 1–2 h |
| WP-35 | `webserver`: CreateHTML, SongIdentifier, LocalWiFiHost, WebServer (dejar `KtorServer.kt` como está) | ~1.157 | 2 h |
| WP-36 | `webserver`: WebServerFragment, RemoteSaveBottomSheet, WebServerMessagesBottomSheet | ~579 | 1 h |
| WP-37 | `midi` modelos/helpers: MidiInfo, MidiClock, ShortHandMidi, MidiInputReceiver, DirectMidiGattCallback, MidiAdapterViewHolder, MidiMessagesAdapter, MidiItemTouchHelper | ~2.300 | 2 h |
| WP-38 | `midi`: Midi (1.662) + MidiFragment, MidiOptionsFragment, BottomSheets | ~3.350 | 3 h (2 sesiones) |
| WP-39 | `export`: PrepareFormats, OpenSongSetBundle, ExportActions, MultipagePrinterAdapter | ~1.200 | 1–2 h |
| WP-40 | `export`: ExportFragment (1.444) | 1.444 | 2 h |
| WP-41 | `importsongs` providers/scrapers (parte 1): BaseSongProvider, SongProviderFactory, Chordie, ChordieProvider, EChords, EChordsProvider, GuitarTabs, GuitarTabsProvider | ~1.600 | 2 h |
| WP-42 | `importsongs` providers/scrapers (parte 2): UltimateGuitar, UltimateGuitarProvider, Chordinator, SongSelect, WebDownload, SongMatch | ~1.600 | 2 h |
| WP-43 | `importsongs` UI: SongMatchAdapter, PreviewPopUp, MyJSInterface, ImportOnlineFragment, ImportFileFragment, ImportOptionsFragment, ImportBulkFragment | ~1.800 | 2 h |
| WP-44 | `openchords` DTOs (29 archivos, la mayoría POJOs): OpenChordsSong, SetList, FolderObject, ConflictObject, LoginRequest/Response, MusicXML, Abc, etc. | ~1.200 | 2 h |
| WP-45 | `openchords`: OpenChordsAPI (2.538) + AuthInterceptor | ~2.700 | 2–3 h (2 sesiones) |
| WP-46 | `openchords` UI: OpenChordsFragment, BottomSheets, Adapters, ConflictCheck | ~750 | 1–2 h |
| WP-47 | `backupandrestore/*` (3) | 1.405 | 1–2 h |

### Etapa F — Vistas personalizadas y controles (riesgo alto: XML inflation, `@JvmOverloads`)

| WP | Contenido | Líneas | Esfuerzo |
|---|---|---|---|
| WP-48 | `customviews` (parte 1: ~12 vistas simples) | ~2.000 | 2 h |
| WP-49 | `customviews` (parte 2: ~12 vistas) | ~2.200 | 2 h |
| WP-50 | `customviews` (parte 3: ~12 vistas) | ~2.400 | 2 h |
| WP-51 | `customviews` (parte 4: resto, incl. MyToolbar 777, MyRecyclerView, MyZoomLayout) | ~2.400 | 2 h |
| WP-52 | `controls`: Swipes, Gestures, GestureListener, HotZones, CommonControls, PedalDebugOverlay | ~1.700 | 2 h |
| WP-53 | `controls`: PedalActions (722), PageButtons, PageButtonFragment, PedalsFragment | ~1.600 | 2 h |
| WP-54 | `controls`: GesturesFragment, HotZonesSettingsFragment, SwipeFragment, ControlMenuFragment | ~1.600 | 2 h |

### Etapa G — UI principal y MainActivity (riesgo alto)

| WP | Contenido | Líneas | Esfuerzo |
|---|---|---|---|
| WP-55 | `songmenu`: IndexAdapter, SongListAdapter, SongListBuildIndex + BottomSheets | ~1.450 | 2 h |
| WP-56 | `songmenu`: SongMenuFragment (887) | 887 | 1–2 h |
| WP-57 | `setmenu`: SetAdapter, SelectSetAdapter, SetMenuFragment, InlineSetFragment + BottomSheets | ~2.367 | 2–3 h |
| WP-58 | `performance`: DisplayPrevNext, PerformanceGestures (1.103), PerformanceFragment (1.601) | ~2.971 | 3 h (2 sesiones) |
| WP-59 | `MainActivity` (parte 1: campos, ciclo de vida, ViewBinding) | ~1.500 | 2 h |
| WP-60 | `MainActivity` (parte 2: callbacks/interfaces) | ~1.600 | 2 h |
| WP-61 | `MainActivity` (parte 3: resto) | ~1.600 | 2 h |

### Etapa H — Verificación final y limpieza

| WP | Contenido | Esfuerzo |
|---|---|---|
| WP-62 | Eliminar `__TODO__.java` y stubs; revisar `grep -r "\.java"` residual; `assembleDebug` + `testDebugUnitTest` | 1–2 h |
| WP-63 | Smoke test manual en emulador: boot, carga de canción, transposición, autoscroll, webserver, import/export OSB | 2–3 h |

---

## 4. Reglas de trabajo diario

1. **Un WP por sesión**, de principio a fin (convertir → borrar `.java` → compilar). No dejar WP a medias: un WP a medias rompe la regla de "build siempre verde".
2. **Nunca refactorizar lógica** durante la migración. Si algo huele a código muerto o feature obsoleta, anotarlo en el checklist de *defeaturing*, no tocarlo aquí.
3. **Compilar al final de cada WP** (`./gradlew app:assembleDebug`). Es la única red de seguridad que existe (no hay tests).
4. **Archivos grandes en trozos**: `ProcessSong`, `StorageAccess`, `ChordDirectory`, `OpenChordsAPI`, `MainActivity` se migran por secciones, compilando entre secciones cuando sea posible.
5. **Interop**: al migrar algo que aún consumen desde Java, conservar firmas (`@JvmOverloads` en constructores de vistas, `@JvmStatic` en `companion`, atención a `T?` vs `T` en interfaces).

---

## 5. Criterio de éxito

- 0 archivos `.java` en `app/src/main/java` (salvo los que el usuario decida conservar deliberadamente).
- `assembleDebug` verde en cada WP y al final.
- Comportamiento observable idéntico (validado por el smoke test manual WP-63).
- Los features marcados para *defeaturing* en el checklist se retiran **antes** de migrar su paquete, para no migrar código que va a desaparecer.
