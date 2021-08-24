
package artificialcomposer;

import UI.*;
import AI.*;

public class Main {

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                mainFrame f = new mainFrame();
                f.setLocationRelativeTo(null);
                f.setVisible(true);
            }
        });
//        double [] weights = new double[4];
//        weights[0] = 0.8;
//        weights[1] = 0.2;
//        weights[2] = 0.1;
//        weights[3] = 0.1;
//        double [] entries = {160, 80, 240, 320};
//        FreqPerceptron p = new FreqPerceptron(4, weights);
//        p.setEntries(entries);
//        p.setResultTresh(0.1);
//        System.out.println("Résultat initial : " + p.processEntries());
//        p.learnUntilConvergence(80);
//        System.out.println("Après apprentissage : " + p.processEntries());
    }

}
