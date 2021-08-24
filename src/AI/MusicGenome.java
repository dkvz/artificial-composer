
package AI;

import artificialcomposer.*;
import java.util.*;

/**
 *
 * @author William
 */
public class MusicGenome extends MusicScore {

    // C'est tout à fait normal de rien capter à ce truc, c'est complètement
    // tordu...
    private double[] reprodTypeP = {0.0, 0.0, 0.0};
    private double fitness = 0;
    public static final int MEASURE = 0;
    public static final int MOREM = 1;
    public static final int DAWA = 2;
    
    public MusicGenome(ArrayList<Note> notes, int tempo) {
        this.setNotes(notes);
        this.setTempo(tempo);
    }
    
    public MusicGenome(MusicScore score) {
        this.setNotes(new ArrayList<Note>(score.getNotes()));
        this.setFil(score.getFil());
        this.setTempo(score.getTempo());
    }
            
    public void randomizeP() {
        // La somme de ces trucs doit faire 1.
        // Euh... Paske je l'ai décidé.
        Random rand = new Random();
        double remaiming = 1.0;
        boolean[] done = {false, false, false};
        while (done[0] == false || done[1] == false || done[2] == false) {
            int start = rand.nextInt(3);
            if (done[start] == true) continue;
            double prob = rand.nextDouble();
            while (prob >= remaiming) {
                prob /= 2;
            }
            remaiming -= prob;
            reprodTypeP[start] = prob;
            done[start] = true;
        }
    }

    public double[] getReprodTypeP() {
        return reprodTypeP;
    }
    
    public void setReprodTypeP(double[] reprodTypeP) {
        this.reprodTypeP = reprodTypeP;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

}
