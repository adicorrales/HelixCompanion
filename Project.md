# Objetivo: desarrollar una app companion para Showcase de helix stadium, que permita
Desplegar letras y acordes de las canciones y además controles la vista Song de la helix mediante 
comandos midi, basado en https://manuals.line6.com/en/helix-stadium/live/midi.

## Control de hardware
la app debe replicar los controles de song de la helix a manera de settings.
1. Selección de canales midi (CH)
    a. Canal Global: 1 por default
    b. Bypass/Ctl: 2 por default
    c. Matrix 1/4": 3
    d. Matrix XLR: 4
    e: Matrix Phones: 5
Los canales deben ser selecionables y mostrarse claramente al usuario.
2. Modo Cue Skip (setting)
Definición: La helix tiene dos estados para las canciones:
-Estado cue: prepara la canción debajo de la activa, parpadeando. La canción está inactiva 
y los tracks son los de la canción activa. Está en fila para ser cargada o cambiada. 

-Estado Loaded: la canción despliega sus tracks en la pantalla y está lista para ser reproducida
Se muestra en el título principal.

Modo Skip: Cuando el usuario selecciona una canción la helix la pone en loaded inmediatamente.
Modo Cue: cuando el usuario selecciona una canción la helix la pone en cue inmediatamente.

3. Modo Play Load. (setting)
Modo Play: cuando una canción está en load o cue, presionar play reproduce la canción inmediatamente
Modo Load: Cuando la canción está en la Cue, play carga la canción y se requiere un segundo play
para reproducirla.


4. Máquina de Estados Skip/Cue + Play/Load (Con el comando MIDI CH1:CC51:Cualquier valor = Play)
La combinación de estos dos settings permite los siguientes escenarios de estado en la Helix.

Skip + Play: Carga instantáneamente la canción y arranca la reproducción de inmediato (CC51:127).
Skip + Load: Carga instantáneamente la canción en la ranura principal y espera al 1er CC51 para reproducir.
Cue + Play: Selecciona en CUE. El 1er CC51 carga y reproduce la canción.
Cue + Load: Selecciona en CUE. El 1er CC51 carga la canción (sin reproducir) y el 2do CC51 inicia la reproducción.

Ciclo de CC51 subsiguientes: Conmuta de forma continua entre reproducción y pausa, adaptándose al 
estado actual de la Helix (siendo el hardware agnóstico al valor numérico de CC51).

5. Los comandos MIDI no editables de la helix son los siguientes (Channel Global):

CC10 Value:0-127  Cues a song within the current Playlist (0=first song, 1=second song, 2=third song, etc.)

CC63 Value:0-127 Selects a Playlist and cues its first song (0=the main SONG LIBRARY folder, 1=Playlist 1, 2=Playlist 2, etc.)

CC46 value:0-127 Cues a Marker flag within the current song (0=song’s Start flag, 1=Marker 1, 2=Marker 2, etc.)

CC47 Return to Zero (any value)

CC48 value 0-63: Clear Cycle (or, create a Cycle between markers) 64-127: Cycle Enable/Disable
If a Cycle exists, 0-63 will clear it. If no Cycle currently exists, one will be created between existing Markers and/or Start/End flags
Values 64-127 act like pressing Stadium’s Cycle switch; that is, it sets the in and out points.

CC49 value:0-63: Previous Song  64-127: Next Song  Cues the Previous/Next Song

CC50  0-63: Previous Marker  64-127: Next Marker  Cues the Previous/Next Marker type flag

CC51 value:0-127 Toggles Play/Pause (first CC received toggles the current state); 
starts playback of a cued song or marker 

6. cambio de playlist

Mandar CC10 o CC49 dentro de la misma playlist es directo: va a la previa, siguiente, o canción 
directamente. El destino de la canción depende de la máquina de estados Skip/cue+play/load

Cuando hay un cambio de playlist CC63 se debe hacer un procedimiento especial:
CC63:n cambia el playlist a la n y carga/cue la primera canción.
-Si el modo es skip
    -Y el modo play está activo, la reprodución inicia de inmediato
    -el modo load está activo, la canción se carga y queda en espera.
- si el modo es Cue
  - ya sea en Play o Load, la 1a canción se queda en Cue: Esto puede llevar a una inconsistencia:
  La playlist aun no se carga en realidad. Si se intenta cambiar la canción con la nueva perteneciente
  al nuevo playlist, la helix no reconoce el cambio y se obtiene --  No hay ninguna playlist activa 
  y hay que selecionar una. Para evitar esto, cuando se manda CC63 en modo cue, hay que forzar la carga 
  automatica con un CC51. En modo play, iniciará a tocar y en modo load, solo quedará cargada.

Nota: CC63 INCLUYE en la Helix el equivalente a CC10:0. No hay que mandar ese comando ya que duplicará
la canción y llevará a --.

7. Feedback.

La helix no manda su estado en midi. Para que la app sepa si el comando llegó a la helix, debe oir
los CC que la helix deja pasar mediante midi Through. Esto no da el estado real, pero al menos sirve para saber que
la helix recibió el comando. Hasta ahora, todo comando recibido ha sido ejecutado en la helix.

8. controles en la app

Los controles obligatorios en la app son:
- Play (cc51)
- Prev/Next (cc49)
- Return to 0 (cc47)
- selección de canción (cc10)
- selección de playlist (cc63)

9. Playlists (sets)

La app va a tener dos categorias de sets (playlist)
- Helix: todo cambio de set cc63 y de canción cc10 cc49, debe mandar un comando midi para control de la helix
- Local: los cambios no deben mandar comandos midi
Motivo: la categoría helix está sincronizada manualemente con canciones y playlist em la helix. 
la categoria local son canciones que pueden estar en el dispositivo o en la helix.

Ambas categorías deben ser administradas por separado, sin mezclarse.

La playlist o set 0 en helix debe ser siempre, y sin posibilidad de edición en su índice, el set llamado Song Library.
De los sets 1 a 127 deberán poder ser reordenados mediante drag and drop.
Las canciones en los sets también deberán poder ser reordenadas mediante drag and drop.

Sin importar la categoría, solo un set puede ser el activo en la app. Jamás puede haber dos desplegados.


## Funcionalidad de la app

La app tiene como base una aplicación llamada openSong que tiene exceso de features y hay que eficientar 
y minimiza. La prioridad de la app debe ser:

1. desplegado de canciones: cambiar una canción debe ser prácticamente inmediato. No es aceptable
que el usuario espere varios segundos hasta que la canción se muestre.
2. Selección rápida de sets y despliegue de las canciones que contiene.
3. envío de comandos a la helix, en función de estado
4. Desplegar y usar canciones tipo openChord. No pdf, imagen, words, etc. esto para eficientar
el despliegue y procesamiento de la canción al usuario
5. No se requiere comunicación con otros dispositivos neraby y por otros protocolos que no sea midi
6. no se requiere personalización de vistas o slides.
7. el motor de performance (despliegue de canciones) debe mantener sus features como autoscale,
edición de tamaños de letra, colores de secciones, etc.
8. no se requiere otro modo de display, además de performance
9. Las funciones que ya existen además de las que no se requieren pueden mantenerse si no afectan 
el desempeño al cargar las canaciones.
10. no se requiere midi clock, metronomo o drummer.
11. las canciones solo deberán contener: letra acordes y tablatura en ascii. NO se requiere nada 
relacionado con ABCnotation
12. Los tags en cada archvo xml de canción ( o sin extensión como está ahora) deben ser minimos:
link del pad, tempo, time signature, key, artist, title, duration, posicion de capotrasto. eliminar todo
tag referente a biblia, beatbuddy, drummer, copyright, presentation, hymn, ccli, aka, user, abcnotation, abctranspose
est para eficientar la lectura y escritura de la canción.
13. la reproducción de archivos de audio (pads) debe mantenerse como ahora, pero debe ser la unica opcion de
pad. Si no hay un archivo de media asociado, no debe recurrirse a ningun pad. las opciones de PAD
auto y off deben desaparecer. Si no hay link a archivo de audo asociado, el pad es OFF por default.
14. Eliminar todo lo referente a multitrack, spotify, song tags y la exportación o share de la canción.
15. en los pages boton, incluir un boton que permita llamar a otras app instalada, para hacer cambios
rápídos sin salir de esta.

Como regla general, mantener la funcionalidad de la app, de manera que se logre el objetivo prioritario
de eficiencia sin sacrificar la funcionalidad que no se ha declarado de forma explicita en los puntos
anteriores.

## Desempeño actual:
1. un desempeño terrible en el despliegue de menus o fragmentos para la seleccion de sets o canciones
2. desempeño terrible en el cambio de sets.
3. desempeño terrible en el cambio de canciones, sobe todo en las largas y complejas.
4. desempeño aceptable en el envío de comando a la helix, con deficiencia en mantener la secuenca de cc51
en los diferentes modos o en cc63.
5. bugs extraños en los que las canciones cambian solas despues de desplegar la correcta.
6. bugs al momento de iniciar donde se selecciona un set diferente al que etaba selecionado al salir.
7. inconsistencia en las tabs de helix y local en donde las canciones se muestran sin importar a que grupo 
pertenecen
8. al reiniciar la app, el set seleccionado presenta canciones duplicadas o triplicadas, 
al volverse a cargar sobre el set existente.

Lo más crítico es el enorme tiempo que hay que esperar para que la app haga casicualquie cosa, y el log
lleno de skipped frames

Muchas de estas características ya están implementadas, pero el desempeño sigue siendo
inaceptable para una experiencia funcional.

## Bugs y funcionalidad erronea persistente.
1. hay tres tabs en el page view para seleccionar las canciones. LA tab helix set muetra los sets de l¿helix y local st muestar los sets de locla. el fragmento Song in sets muestra las canciones del set selecionado. Sin embargo, la lista de canciones de ve en las dos tabs. La funcionalidad es que si no hay set sleecionado en helix o en local, no debería haber un alista de cacniones desplegada. No tiene caso tener la lsita duplicada. Solo debe mostrarse en la tab correspondente
2. el indicador de conexión midi no funciona correctamente. El último dispositivo está apagado totalemnte y me indica que tengo conexión. Incluso test de settings en MIDI me dice ok. pero no sé ¿A qué está conectada la tablet? no hay ninguna indicación.


## implementacion futura (Planning)
- Lo que queda es el coste real: en la canción de 613 vistas, 2,7 s hasta que el frame nuevo está listo (de ahí 1,7 s sólo de crear TextViews), y la pantalla mantiene la anterior todo ese tiempo. Eso ya es el límite del modelo de "una vista por trozo de línea"; el siguiente escalón es reducir vistas, no el flujo.
