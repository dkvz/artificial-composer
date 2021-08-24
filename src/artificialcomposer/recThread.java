package artificialcomposer;

import java.io.IOException;
import java.io.File;
import javax.sound.sampled.*;
import javax.swing.JOptionPane;
import UI.*;

public class recThread extends Thread {

    private TargetDataLine rcl = null;
    private mainFrame interf;

    public recThread(mainFrame interf) {
        try {
            this.interf = interf;
            Mixer rec = interf.getRecorder();
            Line.Info[] lin = rec.getTargetLineInfo();
            setRcl((TargetDataLine) rec.getLine(lin[0]));
        } catch (LineUnavailableException ex) {
            JOptionPane.showMessageDialog(null, "Carte son non prête, ou ligne utilisée.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void run() {
        if (rcl != null && interf != null) {
            try {
                // Maintenant on va lire les données micro.
                // C'est le mixer à l'indice 5.
                // Un bon usage serait de passer les objets mixer ou plutot line
                // depuis l'interface, avec options pour choisir.
                AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
                rcl.open(format);
                rcl.start();
                AudioInputStream flStream = new AudioInputStream(rcl);
                File tget = findSuitableFile();
                interf.inform("En train d'enregistrer...");
                int numBytesRead = AudioSystem.write(flStream, AudioFileFormat.Type.WAVE, tget);
                flStream.close();
                rcl.close();
                interf.inform("Enregistrement terminé : " + numBytesRead + " octets dans le fichier " + tget.getName());
                interf.fillFileList();
            } catch (LineUnavailableException ex) {
                JOptionPane.showMessageDialog(null, "Carte son non prête, ou ligne utilisée.", "Erreur", JOptionPane.ERROR_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Erreur d'écriture (pas de bol).", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public File findSuitableFile() {
        String prefix = interf.getWorkingDir().getPath() + File.separator;
        File curDir = new File(prefix + "extrait.wav");
        int iter = 1;
        while (true) {
            curDir = new File(prefix + "extrait_" + iter + ".wav");
            if (!curDir.exists()) {
                return curDir;
            }
            iter++;
        }
    }

    public TargetDataLine getRcl() {
        return rcl;
    }

    public void setRcl(TargetDataLine rcl) {
        this.rcl = rcl;
    }
}
