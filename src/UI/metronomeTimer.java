
package UI;

import java.util.TimerTask;

/**
 *
 * @author William
 */
public class metronomeTimer extends TimerTask {
    
    private mainFrame frm;
    
    public metronomeTimer(mainFrame frm) {
        this.frm = frm;
    }
    
    @Override
    public void run() {
        if (frm != null) {
            frm.metronomeSignalOn();
            try {
                Thread.sleep(150);
            } catch (InterruptedException ex) {
                // Rien.
            }
            frm.metronomeSignalOff();
        }
    }
    
}
