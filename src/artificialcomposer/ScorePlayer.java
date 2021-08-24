
package artificialcomposer;

import javax.sound.midi.*;
import javax.sound.midi.MidiDevice.Info;

/**
 *
 * @author William
 */
public class ScorePlayer extends Thread {

    private final MusicScore score;
    private int instrument = 0;
    private int velocity = 80;
    private Sequence sequence;
    private Sequencer sequencer;
    
    public ScorePlayer(MusicScore score, int instrument) throws InvalidMidiDataException, MidiUnavailableException {
        this.score = score;
        this.instrument = instrument;
        sequence = new Sequence(Sequence.PPQ, 16);
        Track track = sequence.createTrack();
        // Change l'instrument pour le track 0 :
        ShortMessage sm = new ShortMessage( );
        sm.setMessage(ShortMessage.PROGRAM_CHANGE, 0, instrument, 0);
        track.add(new MidiEvent(sm, 0));
        // Calculer les valeurs de notes et les ajouter au track :
        int time = 0;   // Temps MIDI en ticks.
        for (int x = 0; x < score.getNotes().size(); x++) {
            // 60 : Do octave 5.
            if (score.getNotes().get(x).getNote() != Note.SILENCE) {
                int baseKey = 60 + (score.getNotes().get(x).getNote() - 1) + 12 * (score.getNotes().get(x).getOctave() - 5);
                ShortMessage on = new ShortMessage();
                on.setMessage(ShortMessage.NOTE_ON,  0, baseKey, velocity);
                ShortMessage off = new ShortMessage( );
                off.setMessage(ShortMessage.NOTE_OFF, 0, baseKey, velocity);
                track.add(new MidiEvent(on, time));
                track.add(new MidiEvent(off, time + score.getNotes().get(x).getNoteValue()));
            }
            time += score.getNotes().get(x).getNoteValue();
        }
        Info[] infos = MidiSystem.getMidiDeviceInfo();
        int rtPos = -1;
        for (int x = 0; x < infos.length; x++) {
            if(infos[x].getName().contains("Real Time Sequencer")) {
                rtPos = x; 
                break;
            }
        }

        if (rtPos == -1) throw new MidiUnavailableException();
        sequencer = (Sequencer) MidiSystem.getMidiDevice(infos[rtPos]);
    }
    
    @Override
    public void run() {
        try {
            //= MidiSystem.getSequencer();
            sequencer.open();  
            Synthesizer synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            sequencer.getTransmitter().setReceiver(synthesizer.getReceiver( ));
            sequencer.setSequence(sequence);
            sequencer.setTempoInBPM(score.getTempo());
            sequencer.start();
        } catch (MidiUnavailableException ex) {
            // C'est la merde...
        } catch (InvalidMidiDataException ex) {
            // C'est la merde...
        }
    }

}
