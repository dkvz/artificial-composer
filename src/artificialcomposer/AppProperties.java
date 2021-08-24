
package artificialcomposer;

import java.io.*;
import AI.*;

/**
 *
 * @author William
 */
public class AppProperties implements Serializable {

    // Enfait je set manuellement la ligne pour le player,
    // Et la ligne pour l'enregistreur... Peut-être une
    // mauvaise idée.
    private int playerMixerNbr;
    private int recMixerNbr;
    private int midiSeqNbr;
    private int minAmpl;
    private int instrument;
    private double tessitureLow;
    private double tessitureHigh;
    private int activeCorrectionCode;
    private boolean echProlongation;
    private boolean useAI;
    // Sauver le perceptron ?
//    private FreqPerceptron perceptron;
    private FreqPerceptron[] perceptronList;
    private File workingDir;
    // Le fichier de props DOIT être dans le même rep. que l'app...
    
    public AppProperties() {
        minAmpl = 1700;
        tessitureLow = 100.0;
        tessitureHigh = 2000.0;
        echProlongation = true;
        useAI = false;
        workingDir = new File(".");
        instrument = 0;
        midiSeqNbr = 2; // Euh ouais...
        recMixerNbr = 5;
        playerMixerNbr = 1;
        perceptronList = new FreqPerceptron[4];
        for (int i = 0; i < perceptronList.length; i++) {
            perceptronList[i] = null;
        }
        activeCorrectionCode = soundAnalyser.NO_CORREC;
    }
    
    public AppProperties(File fil) throws IOException, ClassNotFoundException {
        readFromDisk(fil);
    }
    
    public void writeToDisk(File out) throws IOException {
        FileOutputStream fout = new FileOutputStream(out);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(this);
        oos.close();
    }
    
    public void readFromDisk(File in) throws IOException, ClassNotFoundException {
        FileInputStream fin = new FileInputStream(in);
        ObjectInputStream ois = new ObjectInputStream(fin);
        AppProperties toLoad = (AppProperties) ois.readObject();
        this.setActiveCorrectionCode(toLoad.getActiveCorrectionCode());
        this.setEchProlongation(toLoad.isEchProlongation());
        this.setInstrument(toLoad.getInstrument());
        this.setMidiSeqNbr(toLoad.getMidiSeqNbr());
        this.setMinAmpl(toLoad.getMinAmpl());
        this.setPerceptronList(toLoad.getPerceptronList());
        this.setPlayerMixerNbr(toLoad.getPlayerMixerNbr());
        this.setRecMixerNbr(toLoad.getRecMixerNbr());
        this.setTessitureHigh(toLoad.getTessitureHigh());
        this.setTessitureLow(toLoad.getTessitureLow());
        this.setUseAI(toLoad.isUseAI());
        this.setWorkingDir(toLoad.getWorkingDir());
        ois.close();
    }

    public int getPlayerMixerNbr() {
        return playerMixerNbr;
    }

    public void setPlayerMixerNbr(int playerMixerNbr) {
        this.playerMixerNbr = playerMixerNbr;
    }

    public int getRecMixerNbr() {
        return recMixerNbr;
    }

    public void setRecMixerNbr(int recMixerNbr) {
        this.recMixerNbr = recMixerNbr;
    }

    public int getMidiSeqNbr() {
        return midiSeqNbr;
    }

    public void setMidiSeqNbr(int midiSeqNbr) {
        this.midiSeqNbr = midiSeqNbr;
    }

    public int getMinAmpl() {
        return minAmpl;
    }

    public void setMinAmpl(int minAmpl) {
        this.minAmpl = minAmpl;
    }

    public int getInstrument() {
        return instrument;
    }

    public void setInstrument(int instrument) {
        this.instrument = instrument;
    }

    public double getTessitureLow() {
        return tessitureLow;
    }

    public void setTessitureLow(double tessitureLow) {
        this.tessitureLow = tessitureLow;
    }

    public double getTessitureHigh() {
        return tessitureHigh;
    }

    public void setTessitureHigh(double tessitureHigh) {
        this.tessitureHigh = tessitureHigh;
    }

    public int getActiveCorrectionCode() {
        return activeCorrectionCode;
    }

    public void setActiveCorrectionCode(int activeCorrectionCode) {
        this.activeCorrectionCode = activeCorrectionCode;
    }

    public boolean isEchProlongation() {
        return echProlongation;
    }

    public void setEchProlongation(boolean echProlongation) {
        this.echProlongation = echProlongation;
    }

    public File getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(File workingDir) {
        this.workingDir = workingDir;
    }

    public boolean isUseAI() {
        return useAI;
    }

    public void setUseAI(boolean useAI) {
        this.useAI = useAI;
    }

    /**
     * @return the perceptronList
     */
    public FreqPerceptron[] getPerceptronList() {
        return perceptronList;
    }

    /**
     * @param perceptronList the perceptronList to set
     */
    public void setPerceptronList(FreqPerceptron[] perceptronList) {
        this.perceptronList = perceptronList;
    }
    
}
