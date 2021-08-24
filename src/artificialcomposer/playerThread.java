package artificialcomposer;

import UI.mainFrame;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.*;
import javax.swing.JOptionPane;

public class playerThread extends Thread {

    private File file;
    private mainFrame interf;

    public playerThread(File file, mainFrame interf) {
        this.file = file;
        this.interf = interf;
    }

    @Override
    public void run() {

        AudioInputStream strm = null;
        try {
            Mixer pb = interf.getPlayer();
            Line.Info[] lin = pb.getSourceLineInfo();
            Clip clp = (Clip) pb.getLine(lin[1]);
            strm = AudioSystem.getAudioInputStream(file);
            clp.open(strm);
            clp.start();
        } catch (UnsupportedAudioFileException ex) {
            JOptionPane.showMessageDialog(null, "Format audio non supporté.", "Erreur", JOptionPane.ERROR_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Fichier illisible, peut être en cours d'utilisation.", "Erreur", JOptionPane.ERROR_MESSAGE);
        } catch (LineUnavailableException ex) {
            JOptionPane.showMessageDialog(null, "Erreur carte son.", "Erreur", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                strm.close();
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }

    }
}
