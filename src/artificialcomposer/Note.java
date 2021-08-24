
package artificialcomposer;

/**
 *
 * @author William
 */
public class Note {
    
    public static final int WHOLE = 64;
    public static final int HALF = 32;
    public static final int QUARTER = 16;
    public static final int EIGHTH = 8;
    public static final int SIXTEENTH = 4;
    
    public static final int DO = 1;
    public static final int DOSh = 2;
    public static final int RE = 3;
    public static final int RESh = 4;
    public static final int MI = 5;
    public static final int FA = 6;
    public static final int FASh = 7;
    public static final int SOL = 8;
    public static final int SOLSh = 9;
    public static final int LA = 10;
    public static final int LASh = 11;
    public static final int SI = 12;
    public static final int SILENCE = 0;
    
    // Ouais je sais c'est un peu redondant, c'est historique...
    public static final double[] FREQS = {0, 16.351, 17.323, 18.354, 19.445, 20.601, 21.826, 23.124, 24.499, 25.956, 27.5, 29.135, 30.867, 32.703};
    public static final double FDO = 16.351;
    public static final double FDOSh = 17.323;
    public static final double FRE = 18.354;
    public static final double FRESh = 19.445;
    public static final double FMI = 20.601;
    public static final double FFA = 21.826;
    public static final double FFASh = 23.124;
    public static final double FSOL = 24.499;
    public static final double FSOLSh = 25.956;
    public static final double FLA = 27.5;
    public static final double FLASh = 29.135;
    public static final double FSI = 30.867;
    private static final double FDO1 = 32.703;
    
    public static final String [] NAMES = {"Silence", "Do", "Do#", "Re", "Re#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si"}; 
    
    private final double freq;
    private final int note;
    private final int octave;
    private int duration = 1;
    private int noteValue = 64;
    
    public Note(double freq) {
        // Crée la note en fonction de la fréquence.
        this.freq = freq;
        int [] fnote = findNote(freq);
        this.note = fnote[0];
        this.octave = fnote[1];
    }
    
    public Note(String note, int octave) {
        freq = 0;
        this.octave = octave;
        for (int x = 0; x < NAMES.length; x++) {
            if (NAMES[x].equals(note)) {
                this.note = x;
                return;
            }
        }
        this.note = Note.SILENCE;
    }
    
    public Note(int note, int octave) {
        this.note = note;
        this.octave = octave;
        // En fait ça marche pas du tout comme ça c'est *2 à chaque octave...
        switch(note) {
            case Note.DO:
                freq = Note.FDO * octave;
                break;
            case Note.DOSh:
                freq = Note.FDOSh * octave;
                break;
            case Note.RE:
                freq = Note.FRE * octave;
                break;
            case Note.RESh:
                freq = Note.FRESh * octave;
                break;
            case Note.MI:
                freq = Note.FMI * octave;
                break;
            case Note.FA:
                freq = Note.FFA * octave;
                break;
            case Note.FASh:
                freq = Note.FFASh * octave;
                break;
            case Note.SOL:
                freq = Note.FSOL * octave;
                break;
            case Note.SOLSh:
                freq = Note.FSOLSh * octave;
                break;
            case Note.LA:
                freq = Note.FLA * octave;
                break;
            case Note.LASh:
                freq = Note.FLASh * octave;
                break;
            case Note.SI:
                freq = Note.FSI * octave;
                break;
            default:
                freq = 0;
        }
    }
    
    public double calculateFreq() {
        //double frequ = Note.FREQS[this.note] * (this.octave * 2);
        double frequ = Note.FREQS[this.note] * Math.pow(2, this.octave - 1);
        return frequ;
    }
    
    public void findNoteValue(int quarterDuration, int tickDuration, int minLength) {
        // Trouve la valeur de la note à partir de son nombre de tick,
        // de la durée d'un tick, et de la durée d'une ronde (les deux
        // en milisecondes).
        if (minLength > Note.QUARTER) minLength = Note.QUARTER;
        int noteVal = tickDuration * this.duration;
        // Calcul en nombre de noires :
        double nbrNoires = noteVal / (double)quarterDuration;
        // Si on a un nombre entier de noires et genre une eighth ou -, faut la nier...
        int tickVal = (int)Math.round(nbrNoires * Note.QUARTER);
        int mod = tickVal % Note.QUARTER;
        if (mod != 0) {
            if (mod >= (Note.QUARTER - minLength)) {
                tickVal += (Note.QUARTER - mod);
            } else if (mod <= minLength) {
                tickVal -= mod;
            } else {
                // Là ça deviens bizarre ce truc...
                if (mod > Note.QUARTER / 2) {
                    tickVal -= (tickVal % minLength);
                } else {
                    tickVal += (tickVal % minLength);
                }
            }
        }
        this.noteValue = tickVal;
    }
    
    public static int [] findNote(double frequency) {
        int [] res = new int[2];
        int count = 1;  // Octave 0 : à partir de 16 hz.
        while (frequency > Note.FSI + (Note.FDO1 - Note.FSI) / 2) {
            frequency /= 2.0;
            count++;
        }
        // On est plus petit ou égal au si...
        res[1] = count;
        if (frequency < (Note.FDOSh - (Note.FDOSh - Note.FDO) / 2) && frequency >= (Note.FDO - (Note.FDOSh - Note.FDO) / 2)) {
            res[0] = Note.DO;
        } else if (frequency < (Note.FRE - (Note.FRE - Note.FDOSh) / 2) && frequency >= (Note.FDOSh - (Note.FDOSh - Note.FDO) / 2)) {
            res[0] = Note.DOSh;
        } else if (frequency < (Note.FRESh - (Note.FRESh - Note.FRE) / 2) && frequency >= (Note.FRE - (Note.FRE - Note.FDOSh) / 2)) {
            res[0] = Note.RE;
        } else if (frequency < (Note.FMI - (Note.FMI - Note.FRESh) / 2) && frequency >= (Note.FRESh - (Note.FRESh - Note.FRE) / 2)) {
            res[0] = Note.RESh;
        } else if (frequency < (Note.FFA - (Note.FFA - Note.FMI) / 2) && frequency >= (Note.FMI - (Note.FMI - Note.RESh) / 2)) {
            res[0] = Note.MI;
        } else if (frequency < (Note.FFASh - (Note.FFASh - Note.FFA) / 2) && frequency >= (Note.FFA - (Note.FFA - Note.FMI) / 2)) {
            res[0] = Note.FA;
        } else if (frequency < (Note.FSOL - (Note.FSOL - Note.FFASh) / 2) && frequency >= (Note.FFASh - (Note.FFASh - Note.FFA) / 2)) {
            res[0] = Note.FASh;
        } else if (frequency < (Note.FSOLSh - (Note.FSOLSh - Note.FSOL) / 2) && frequency >= (Note.FSOL - (Note.FSOL - Note.FFASh) / 2)) {
            res[0] = Note.SOL;
        } else if (frequency < (Note.FLA - (Note.FLA - Note.FSOLSh) / 2) && frequency >= (Note.FSOLSh - (Note.FSOLSh - Note.FSOL) / 2)) {
            res[0] = Note.SOLSh;
        } else if (frequency < (Note.FLASh - (Note.FLASh - Note.FLA) / 2) && frequency >= (Note.FLA - (Note.FLA - Note.FSOLSh) / 2)) {
            res[0] = Note.LA;
        } else if (frequency < (Note.FSI - (Note.FSI - Note.FLASh) / 2) && frequency >= (Note.FLASh - (Note.FLASh - Note.FLA) / 2)) {
            res[0] = Note.LASh;
        } else if (frequency < (Note.FDO1 - (Note.FDO1 - Note.FSI) / 2) && frequency >= (Note.FSI - (Note.FSI - Note.FLASh) / 2)) {
            res[0] = Note.SI;
        } else {
            res[0] = Note.SILENCE;
        }
        return res;
    }

    public void incrementDuration() {
        duration++;
    }
    
    public double getFreq() {
        return freq;
    }

    public int getNote() {
        return note;
    }
    
    public int getOctave() {
        return octave;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public String getNoteString() {
        return (NAMES[this.note]);
    }
    
    public String toScoreString() {
        return (NAMES[this.note] + " - oct. " + octave + " - durée " + noteValue);
    }
    
    @Override
    public String toString() {
        return (NAMES[this.note] + " octave " + octave + " durée " + duration);
    }

    public int getNoteValue() {
        return noteValue;
    }

    public void setNoteValue(int noteValue) {
        this.noteValue = noteValue;
    }

}
