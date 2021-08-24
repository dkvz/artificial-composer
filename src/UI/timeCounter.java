/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package UI;

import java.util.TimerTask;
import artificialcomposer.*;

public class timeCounter extends TimerTask {

    private mainFrame frm;
    private int counter = 0;
    
    public timeCounter(mainFrame frm) {
        this.frm = frm;
    }
    
    @Override
    public void run() {
        if (frm != null) {
            counter++;
            frm.getJLabelCounter().setText("Temps écoulé : " + counter + "s");
        }
    }

}
