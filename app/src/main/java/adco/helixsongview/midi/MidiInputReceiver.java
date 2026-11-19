package adco.helixsongview.midi;

import android.content.Context;
import android.media.midi.MidiReceiver;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import adco.helixsongview.interfaces.MainActivityInterface;
import adco.helixsongview.songprocessing.Song;

import java.util.ArrayList;
import java.util.Arrays;

@RequiresApi(api = Build.VERSION_CODES.M)
public class MidiInputReceiver extends MidiReceiver {

    private final MainActivityInterface mainActivityInterface;
    private final Context context;
    private final String TAG = "MidiInputReceiver";
    private ArrayList<Byte> receivedMessage;
    private final int nx_start=128, nx_end=143, no_start=144, no_end=159,
            cc_start=176, cc_end=191, pc_start=192, pc_end=207;
    private int msbChosen, pcChosen;
    private final Handler songMessageHander = new Handler(Looper.getMainLooper());
    private final Runnable songMessageRunnable;
    private final Handler longPressHandler = new Handler(Looper.getMainLooper());
    private final Runnable longPressRunnable;
    private int longPressNote = -1;
    private boolean listeningForLongPress = false;
    private boolean isLongPress = false;

    MidiInputReceiver(Context c) {
        context = c;
        mainActivityInterface = (MainActivityInterface) c;
        songMessageRunnable = () -> {
            // We took too long (runnable called 1sec after msbChosen is set)
            // Reset the msbChosen, so any PC after this is ignored
            try {
                msbChosen = -1;
                pcChosen = -1;
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        longPressRunnable = () -> {
            // If we haven't received the NX yet, but did receive the NO 1 sec ago, it's a long press
            if (listeningForLongPress) {
                // This stops us recording the next actionUp message
                isLongPress = true;
                mainActivityInterface.registerMidiPedalAction(false, false, true,
                        mainActivityInterface.getMidi().getNoteFromInt(longPressNote));
            }
        };
    }

    @Override
    // Confusing, but this is triggered when we receive a MIDI input message
    public void onSend(byte[] msg, int offset, int count, long timestamp) {
        try {
            // Keep a reference of the midi message (so we can record incoming messages)
            addReceivedMessage(msg);
            int length = Math.max(0, Math.min(count, msg.length - offset));
            int[] bytes = new int[length];
            for (int i = 0; i < length; i++) {
                bytes[i] = msg[offset + i] & 0xFF;
            }

            Log.d(TAG,"message:"+ Arrays.toString(bytes));
            boolean midiStart = isMidiStart(bytes);
            boolean midiStop = isMidiStop(bytes);
            boolean songSelect = isSongSelect(bytes);
            String messageType = getMessageType(bytes);
            int midiChannel = getMidiChannelFromBytes(bytes);
            // The data is numerical from 0-127
            int data1 = getData1(bytes);
            int data2 = getData2(bytes);

            Log.d(TAG,"messageType:"+messageType+"  midiChannel:"+midiChannel+"  data1:"+data1+"  data2:"+data2);

            if (messageType.equals("CC")) {
                // Control Change feedback echo from Helix (always process globally)
                mainActivityInterface.getMainHandler().post(() -> {
                    mainActivityInterface.handleMidiFeedback(midiChannel, data1, data2);
                });
            }

            String midiInfo = getMidiCommandText(
                    messageType, midiChannel, data1, data2, midiStart, midiStop, songSelect);
            mainActivityInterface.getMainHandler().post(() -> {
                if (mainActivityInterface != null) {
                    mainActivityInterface.getShowToast().doItMidi(midiInfo);
                }
            });

            // Only do something if we are listening on these channels and the message is appropriate
            if (midiChannel == mainActivityInterface.getMidi().getMidiInputChannelPedal() &&
                    messageType.equals("NO") || messageType.equals("NX")) {
                // This is a likely foot pedal command - note on==actionDown==NO, note off=actionUp==NX
                // We also check for a long press using a handler
                // Long press means the actionDown is recieved and no actionUp is received within 1 second
                boolean actionDown = messageType.equals("NO");
                boolean actionUp = messageType.equals("NX");

                if (actionDown) {
                    // Just in case this is a long press, keep a note of the data 1 (note pressed)
                    longPressNote = data1;

                    // Set the values to start waiting for a long press
                    // Clear any current runnables from the handler and listen for 1 second
                    isLongPress = false;
                    listeningForLongPress = true;
                    longPressHandler.removeCallbacks(longPressRunnable);
                    longPressHandler.postDelayed(longPressRunnable, 1000);
                } else {
                    // This is an actionUp, so we don't need the long press check
                    longPressHandler.removeCallbacks(longPressRunnable);
                }

                // Check if this is an actionUp called immediately after a long press
                // Because the long press handler dealt with it, ignore this one.
                // If we didn't we'd trigger both a long press and single press action
                boolean falseActionUp = actionUp && isLongPress;

                // Now we can reset the longPress flag ready for another go later
                if (actionUp) {
                    isLongPress = false;
                    listeningForLongPress = false;
                }

                // This only sends simple actionDown/actionUp.
                // Long presses are sent from the handler directly, so use the check
                if (!falseActionUp) {
                    mainActivityInterface.registerMidiPedalAction(actionDown, actionUp, false,
                            mainActivityInterface.getMidi().getNoteFromInt(data1));
                }


            } else if ((midiChannel == mainActivityInterface.getMidi().getMidiInputChannelSong()) || songSelect) {
                // This is a likely a change song command or a start/stop for autoscroll
                // This is in multiple parts so only proceed if within time and first part is received
                if (midiStart) {
                    // Try to start the autoscroll/metronome/pad
                    Log.d(TAG, "Start the autoscroll/metronome/pad");
                    if (mainActivityInterface.getMidi().getMidiInputAutoscroll()) {
                        Log.d(TAG,"Start autoscroll");
                        mainActivityInterface.getAutoscroll().startAutoscroll();
                    }

                    if (mainActivityInterface.getMidi().getMidiInputPad()) {
                        Log.d(TAG,"Start pad");
                        int whichPad = mainActivityInterface.getPad().whichPadPlaying();
                        if (whichPad==0) {
                            whichPad = 1;
                        }
                        mainActivityInterface.getPad().playStopOrPause(whichPad);
                    }

                } else if (midiStop) {
                    // Try to pause the autoscroll/metronome/pad
                    if (mainActivityInterface.getMidi().getMidiInputAutoscroll()) {
                        Log.d(TAG,"Pause autoscroll");
                        mainActivityInterface.getAutoscroll().pauseAutoscroll();
                    }

                    if (mainActivityInterface.getMidi().getMidiInputPad()) {
                        Log.d(TAG,"Pause pad");
                        mainActivityInterface.getPad().playStopOrPause(mainActivityInterface.getPad().whichPadPlaying());
                    }

                } else if (messageType.equals("CC") && (data1 == 0 || data1 == 32)) {
                    // This is the bank select on the MSB
                    // Set the handler to clear the MSB value after 1s.  Time for PC to arrive
                    songMessageHander.removeCallbacks(songMessageRunnable);
                    songMessageHander.postDelayed(songMessageRunnable, 1000);
                    Log.d(TAG,"MSB chosen:" + data1 + "," + data2);
                    msbChosen = data2;

                } else if (messageType.equals("CC")) {
                    // Control Change feedback echo from Helix
                    mainActivityInterface.getMainHandler().post(() -> {
                        mainActivityInterface.handleMidiFeedback(midiChannel, data1, data2);
                    });

                } else if (messageType.equals("PC") || songSelect) {
                    // We have received the PC song number and may also have the MSB chosen - song chosen
                    songMessageHander.removeCallbacks(songMessageRunnable);
                    pcChosen = data1;
                    int songNumber = getSongNumber();
                    // Clear the chosen values
                    msbChosen = -1;
                    pcChosen = -1;
                    Log.d(TAG, "songNumber:" + songNumber);
                    Song songToLoad = mainActivityInterface.getSQLiteHelper().getSongFromMidiIndex(songNumber);
                    if (songToLoad!=null && songToLoad.getFilename()!=null && !songToLoad.getFilename().isEmpty() &&
                        songToLoad.getFolder()!=null && !songToLoad.getFolder().isEmpty()) {
                        // A matching song has been found, so try to load it!
                        Log.d(TAG,"loading song "+songToLoad.getFolder()+"/"+songToLoad.getFilename());
                        mainActivityInterface.doSongLoad(songToLoad.getFolder(),songToLoad.getFilename(),false);
                    }
                }
            }
        } catch (Exception | OutOfMemoryError e) {
            e.printStackTrace();
        }
    }

    // MIDI message logging
    public void resetReceivedMessage() {
        receivedMessage = new ArrayList<>();
    }
    @SuppressWarnings("unused")
    public ArrayList<Byte> getReceivedMessage() {
        return receivedMessage;
    }
    private void addReceivedMessage(byte[] bytes) {
        resetReceivedMessage();
        for (byte thisByte:bytes) {
            try {
                receivedMessage.add(thisByte);
            } catch (Exception | OutOfMemoryError e) {
                receivedMessage = null;
                resetReceivedMessage();
                e.printStackTrace();
            }
        }
    }


    // Get the MIDI message information from the received bytes
    // Android delivers raw MIDI starting with the status byte, but some Bluetooth
    // transports (and the old code path) prepend a header/timestamp byte. Detect
    // the real status position instead of assuming a fixed index, otherwise
    // [status, CC, value] is read as [CC, value] and every message looks generic.
    private int getStatusOffset(int[] bytes) {
        if (bytes.length >= 5 && bytes[1] >= 0x80 && bytes[2] >= 0x80) {
            // BLE-MIDI header + timestamp + status + data...
            return 2;
        }
        if (bytes.length >= 2 && bytes[1] >= 0x80) {
            // Header/timestamp + status + data...
            return 1;
        }
        return 0;
    }

    // If there is an issue, return a sensible non-null value
    private String getMessageType(int[] bytes) {
        int o = getStatusOffset(bytes);
        if (bytes.length > o) {
            int status = bytes[o];
            if (status >= nx_start && status <= nx_end) {
                // This is a note off message
                return "NX";
            } else if (status >= no_start && status <= no_end) {
                // This is a note on message
                return "NO";
            } else if (status >= cc_start && status <= cc_end) {
                // This is a control change message
                return "CC";
            } else if (status >= pc_start && status <= pc_end) {
                // This is a program change message
                return "PC";
            }
        }
        return "";
    }
    private int getMidiChannelFromBytes(int[] bytes) {
        int o = getStatusOffset(bytes);
        if (bytes.length > o) {
            // The status byte holds the action and MIDI channel in hex format
            int status = bytes[o];
            if (status >= nx_start && status <= nx_end) {
                return (status - nx_start) + 1;
            } else if (status >= no_start && status <= no_end) {
                return (status - no_start) + 1;
            } else if (status >= cc_start && status <= cc_end) {
                return (status - cc_start) + 1;
            } else if (status >= pc_start && status <= pc_end) {
                return (status - pc_start) + 1;
            }
        }
        // Use default MIDI input channel if we can't get it from the message
        return -1;
    }
    private int getData1(int[] bytes) {
        int o = getStatusOffset(bytes);
        if (bytes.length > o + 1) {
            return bytes[o + 1];
        }
        return 0;
    }
    private int getData2(int[] bytes) {
        int o = getStatusOffset(bytes);
        if (bytes.length > o + 2) {
            return bytes[o + 2];
        }
        return 0;
    }

    private String getMidiCommandText(String messageType, int channel, int data1, int data2,
                                      boolean midiStart, boolean midiStop, boolean songSelect) {
        String command;
        if (midiStart) {
            command = context.getString(adco.helixsongview.R.string.midi_command_start);
        } else if (midiStop) {
            command = context.getString(adco.helixsongview.R.string.midi_command_stop);
        } else if (songSelect) {
            command = context.getString(
                    adco.helixsongview.R.string.midi_command_select_song, data1 + 1);
        } else if ("CC".equals(messageType)) {
            switch (data1) {
                case 10:
                    command = context.getString(
                            adco.helixsongview.R.string.midi_command_select_song, data2 + 1);
                    break;
                case 46:
                    command = context.getString(
                            adco.helixsongview.R.string.midi_command_select_marker, data2 + 1);
                    break;
                case 47:
                    command = context.getString(adco.helixsongview.R.string.midi_command_return_to_zero);
                    break;
                case 48:
                    command = context.getString(data2 < 64
                            ? adco.helixsongview.R.string.midi_command_clear_cycle
                            : adco.helixsongview.R.string.midi_command_toggle_cycle);
                    break;
                case 49:
                    command = context.getString(data2 < 64
                            ? adco.helixsongview.R.string.midi_command_previous_song
                            : adco.helixsongview.R.string.midi_command_next_song);
                    break;
                case 50:
                    command = context.getString(data2 < 64
                            ? adco.helixsongview.R.string.midi_command_previous_marker
                            : adco.helixsongview.R.string.midi_command_next_marker);
                    break;
                case 51:
                    command = context.getString(adco.helixsongview.R.string.midi_command_play_pause);
                    break;
                case 63:
                    command = context.getString(
                            adco.helixsongview.R.string.midi_command_select_playlist, data2);
                    break;
                default:
                    command = context.getString(
                            adco.helixsongview.R.string.midi_command_control_change, data1, data2);
                    break;
            }
        } else if ("NO".equals(messageType)) {
            command = context.getString(adco.helixsongview.R.string.midi_command_note_on, data1);
        } else if ("NX".equals(messageType)) {
            command = context.getString(adco.helixsongview.R.string.midi_command_note_off, data1);
        } else if ("PC".equals(messageType)) {
            command = context.getString(adco.helixsongview.R.string.midi_command_program, data1);
        } else {
            command = context.getString(adco.helixsongview.R.string.midi_command_other);
        }

        return channel > 0
                ? context.getString(adco.helixsongview.R.string.midi_command_channel, channel, command)
                : command;
    }

    private boolean isMidiStart(int[] bytes) {
        // Could be a MIDI start (0xFA)
        int o = getStatusOffset(bytes);
        if (bytes.length > o) {
            return (bytes[o] & 0xFF) == 0xFA;
        }
        return false;
    }
    private boolean isMidiStop(int[] bytes) {
        // Could be a MIDI stop (0xFC)
        int o = getStatusOffset(bytes);
        if (bytes.length > o) {
            return (bytes[o] & 0xFF) == 0xFC;
        }
        return false;
    }
    private boolean isSongSelect(int[] bytes) {
        // Could be a Song Select message (0xF3)
        int o = getStatusOffset(bytes);
        if (bytes.length > o) {
            return (bytes[o] & 0xFF) == 0xF3;
        }
        return false;
    }

    private int getSongNumber() {
        // Use the MSB bank number and PC to get the song number
        // If not MSB received, we assume bank 0 and only use the first 127 songs
        // Song 1 will have msbChosen = 0, pcChosen = 0, so (msbChosen * 128) + pcChosen + 1 = 1
        // Song 128 will have msbChosen = 0, pcChosen = 127, so (msbChosen * 128) + pcChosen + 1 = 128
        // Song 129 will have msbChosen = 1, pcChosen = 0, so (msbChosen * 128) + pcChosen + 1 = 129
        // The final +1 is to change 0 into 1, etc.
        int songNumber = -1;
        if (pcChosen>=0) {
            if (msbChosen>=0) {
                songNumber = (msbChosen * 128) + pcChosen + 1;
            } else {
                songNumber = pcChosen + 1;
            }
        }
        Log.d(TAG,"songNumber:"+songNumber);
        return songNumber;
    }
}
