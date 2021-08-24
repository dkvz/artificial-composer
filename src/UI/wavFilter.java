/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UI;

import java.io.File;
import java.io.FilenameFilter;
import javax.sound.sampled.*;

/**
 *
 * @author William
 */
public class wavFilter implements FilenameFilter {
    
    AudioFormat match;
    
    public wavFilter(AudioFormat match) {
        if (match == null) {
            this.match = new AudioFormat(22050, 16, 1, true, false);
        } else {
            this.match = match;
        }
    }

    public boolean accept(File dir, String name) {
        if (name.toLowerCase().contains(".wav")) {
            File theFile = new File(dir.getPath() + File.separator + name);
            try {
                AudioFileFormat format = AudioSystem.getAudioFileFormat(theFile);
                if (format.getType() == AudioFileFormat.Type.WAVE) {
                    // Vérifier le format des données.
                    AudioFormat frm = format.getFormat();
                    if (frm.matches(match)) {
                        return true;
                    }
                }
            } catch (Exception ex) {
                // Le fichier est pas un wav, ou erreur entrée/sortie.
                System.out.println(ex);
            }
        }
        return false;
    }
}
