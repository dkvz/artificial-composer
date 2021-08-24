package UI;

import javax.sound.sampled.*;
import javax.swing.JOptionPane;
import java.io.ByteArrayOutputStream;
import artificialcomposer.soundAnalyser;

/**
 *
 * @author William
 */
public class tunerThread extends Thread {

    private tuner tun;
    private boolean running = true;
    private TargetDataLine rcl = null;

    public tunerThread(tuner tun) {
        if (tun != null) {
            try {
                this.tun = tun;
                Mixer rec = tun.getFrm().getRecorder();
                Line.Info[] lin = rec.getTargetLineInfo();
                rcl = (TargetDataLine) rec.getLine(lin[0]);
            } catch (LineUnavailableException ex) {
                JOptionPane.showMessageDialog(tun, "Périphérique de capture indisponible.", "Erreur", JOptionPane.ERROR_MESSAGE);
                running = false;
                tun.dispose();
            }
        } else {
            running = false;
        }
    }

    @Override
    public void run() {
        try {
            AudioFormat format = new AudioFormat(22050, 16, 1, true, false);
            rcl.open(format);
            rcl.start();
            //AudioInputStream flStream = new AudioInputStream(rcl);
            ByteArrayOutputStream btStream = new ByteArrayOutputStream();
            tun.getJLabelState().setText("Accordage en cours, référence : La medium.");
            tun.pack();
            byte[] data = new byte[rcl.getBufferSize() / 5];
            int numBytesRead;
            while (isRunning()) {
                numBytesRead = rcl.read(data, 0, data.length);
                btStream.write(data, 0, numBytesRead);
                if (btStream.size() >= 32000) {
                    double fonda = soundAnalyser.findFundamental(btStream.toByteArray(), btStream.size(), 22050, soundAnalyser.NO_CORREC, false);
                    tun.showTune(fonda);
                    Thread.sleep(300);
                    btStream.reset();
                }
            }
        } catch (LineUnavailableException ex) {
            JOptionPane.showMessageDialog(tun, "Périphérique de capture indisponible.", "Erreur", JOptionPane.ERROR_MESSAGE);
        } catch (InterruptedException ex) {
        // Miam.
        } finally {
            rcl.stop();
            rcl.close();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }
}
