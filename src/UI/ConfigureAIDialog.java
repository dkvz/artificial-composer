/*
 * ConfigureAIDialog.java
 *
 * Created on 8 décembre 2008, 12:56
 */
package UI;

import javax.sound.sampled.*;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import AI.*;
import artificialcomposer.*;
import java.io.*;

/**
 *
 * @author  William
 */
public class ConfigureAIDialog extends javax.swing.JDialog {

    private mainFrame frm;
//    private FreqPerceptron perceptron = null;
    FreqPerceptron[] perceptronList = new FreqPerceptron[4];
    private Note noteFound = null;
    private int harmCountFound = -1;

    public ConfigureAIDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        frm = (mainFrame) parent;
        initComponents();
        // On nie le silence dans cette liste...
        for (int x = 1; x < Note.NAMES.length; x++) {
            this.jComboBoxNotes.addItem(Note.NAMES[x]);
        }
        if (frm.getProps().getPerceptronList()[0] == null) {
            // Créer un perceptron basique.
            this.createNewPerceptron(0);
            // ******* Ajouté pour perceptron évolué :
//            double[] weights = new double[7];
//            // *******
//            weights[0] = 0.92;
//            weights[1] = 0.1;
//            weights[2] = 0.1;
//            weights[3] = 0.1;
//            weights[4] = -0.2;
//            weights[5] = -0.2;
//            weights[6] = -0.2;
//            for (int x = 2; x < weights.length; x++) {
//                weights[x] = 0.1;
//            }
        } else {
            perceptronList = frm.getProps().getPerceptronList();
        }
        this.jSpinnerFreqTresh.getModel().setValue(perceptronList[0].getResultTresh());
    }

    private void createNewPerceptron(int indice) {
        double[] weights = new double[4];
        weights[0] = 0.90;
        weights[1] = 0.08;
        for (int x = 2; x < weights.length; x++) {
            weights[x] = 0.01;
        }
        FreqPerceptron perceptron = new FreqPerceptron(weights.length, weights);
        if (perceptronList[0] != null) {
            perceptron.setResultTresh(perceptronList[0].getResultTresh());
        } else {
            perceptron.setResultTresh(0.2);
        }
        perceptronList[indice] = perceptron;
        System.out.println("Nouveau perceptron créé en " + indice);
    }

    private void analyse(byte[] audioBytes) {
        this.jLabelStatus.setText("Analyse...");
        this.paint(this.getGraphics());
        int maxAmpl = (Integer) this.jSpinnerMaxAmpl.getModel().getValue();
        int tresh = 5;  // Sert à comparer les spectres.

        int[] prevMaxims = {0, 0, 0, 0};
        int count = 0;
        int middle = audioBytes.length / 2;
        for (int x = middle; x <= (audioBytes.length - 2 * 2048); x += 2048) {
            byte[] feed = new byte[2048];
            for (int y = 0; y < 2048; y++) {
                feed[y] = audioBytes[x + y];
            }
            Spectrum spectre = soundAnalyser.findReducedSpectrum(feed, 44100, maxAmpl, 4, true);
            int[] maxims = spectre.getIndices();
            this.harmCountFound = -1;
            for (int i = 0; i < maxims.length; i++) {
                if (maxims[i] != 0) {
                    this.harmCountFound++;
                }
            }
            if (maxims[0] != 0) {
                if (soundAnalyser.compareMaxims(maxims, prevMaxims, tresh)) {
                    count++;
                    if (count == 4) {
                        // Alaise on garde.
                        // Le perceptron est fréquentiel pour être indép. des conditions
                        // d'analyse, faut donc calculer les fréquences...
                        double[] entries = new double[maxims.length];
                        for (int i = 0; i < entries.length; i++) {
                            // On a une longueur de 8*1024 avec la prolongation...
                            // Donc 4 * le nbre de bytes.
                            entries[i] = maxims[i] * 44100.0 / (4 * feed.length);
                        }
                        for (int i = 0; i < 4; i++) {
                            entries[i] = maxims[i] * 44100.0 / (4 * feed.length);
                        }
                        double freq;
                        if (perceptronList[this.harmCountFound] == null) {
                            // Existe pas encore, on le crée...
                            this.createNewPerceptron(this.harmCountFound);
                        }
                        perceptronList[this.harmCountFound].setEntries(entries);
                        freq = perceptronList[this.harmCountFound].processEntries();
                        Note note = new Note(freq);
                        this.jLabelNote.setText(note.toString());
                        this.jLabelFreq.setText("Fréq. : " + Math.round(freq) + "hz");
                        this.noteFound = note;
                        break;
                    }
                } else {
                    prevMaxims = maxims;
                }
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel9 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jSpinnerFreqTresh = new javax.swing.JSpinner();
        jPanel1 = new javax.swing.JPanel();
        jButtonNew = new javax.swing.JButton();
        jButtonLoad = new javax.swing.JButton();
        jButtonHelp = new javax.swing.JButton();
        jButtonSave = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jButtonRecord = new javax.swing.JButton();
        jLabelStatus = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jSpinnerMaxAmpl = new javax.swing.JSpinner();
        jPanel5 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabelNote = new javax.swing.JLabel();
        jLabelFreq = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jComboBoxNotes = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        jSpinnerOctave = new javax.swing.JSpinner();
        jButtonLearn = new javax.swing.JButton();
        jButtonLearnSlow = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jButtonClose = new javax.swing.JButton();

        jPanel9.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jLabel6.setText("Treshold fréquentiel du perceptron (truc de fou pour connaisseur) :");
        jPanel9.add(jLabel6);

        jSpinnerFreqTresh.setModel(new javax.swing.SpinnerNumberModel(Double.valueOf(0.1d), Double.valueOf(0.0d), null, Double.valueOf(1.0d)));
        jSpinnerFreqTresh.setPreferredSize(new java.awt.Dimension(50, 20));
        jSpinnerFreqTresh.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSpinnerFreqTreshStateChanged(evt);
            }
        });
        jPanel9.add(jSpinnerFreqTresh);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Apprendre le contenu harmonique");

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jButtonNew.setText("Nouveau");
        jButtonNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonNewActionPerformed(evt);
            }
        });
        jPanel1.add(jButtonNew);

        jButtonLoad.setText("Charger un mécanisme de reconnaissance");
        jButtonLoad.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLoadActionPerformed(evt);
            }
        });
        jPanel1.add(jButtonLoad);

        jButtonHelp.setText("Je comprend rien du tout !");
        jButtonHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonHelpActionPerformed(evt);
            }
        });
        jPanel1.add(jButtonHelp);

        jButtonSave.setText("Sauver le mécanisme de reconnaissance");
        jButtonSave.setEnabled(false);
        jButtonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveActionPerformed(evt);
            }
        });
        jPanel1.add(jButtonSave);

        getContentPane().add(jPanel1, java.awt.BorderLayout.NORTH);

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel4.setLayout(new java.awt.BorderLayout());

        jButtonRecord.setText("Enregistrer");
        jButtonRecord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRecordActionPerformed(evt);
            }
        });
        jPanel10.add(jButtonRecord);

        jLabelStatus.setText("Etat");
        jLabelStatus.setPreferredSize(new java.awt.Dimension(140, 14));
        jPanel10.add(jLabelStatus);

        jPanel4.add(jPanel10, java.awt.BorderLayout.NORTH);

        jLabel1.setText("Niveau d'amplitude minimum :");
        jPanel11.add(jLabel1);

        jSpinnerMaxAmpl.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(1000), Integer.valueOf(0), null, Integer.valueOf(50)));
        jSpinnerMaxAmpl.setPreferredSize(new java.awt.Dimension(70, 20));
        jPanel11.add(jSpinnerMaxAmpl);

        jPanel4.add(jPanel11, java.awt.BorderLayout.CENTER);

        jPanel2.add(jPanel4, java.awt.BorderLayout.NORTH);

        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Note trouvée"));
        jPanel6.setLayout(new javax.swing.BoxLayout(jPanel6, javax.swing.BoxLayout.Y_AXIS));

        jLabelNote.setPreferredSize(new java.awt.Dimension(170, 50));
        jPanel6.add(jLabelNote);
        jPanel6.add(jLabelFreq);

        jPanel5.add(jPanel6, java.awt.BorderLayout.WEST);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Si note non correcte :"));
        jPanel7.setLayout(new java.awt.BorderLayout());

        jPanel8.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jLabel4.setText("Note correcte :");
        jPanel8.add(jLabel4);

        jComboBoxNotes.setEnabled(false);
        jPanel8.add(jComboBoxNotes);

        jLabel5.setText("Octave :");
        jPanel8.add(jLabel5);

        jSpinnerOctave.setModel(new javax.swing.SpinnerNumberModel(5, 1, 9, 1));
        jSpinnerOctave.setEnabled(false);
        jSpinnerOctave.setPreferredSize(new java.awt.Dimension(40, 20));
        jPanel8.add(jSpinnerOctave);

        jButtonLearn.setText("Enregistrer");
        jButtonLearn.setEnabled(false);
        jButtonLearn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLearnActionPerformed(evt);
            }
        });
        jPanel8.add(jButtonLearn);

        jButtonLearnSlow.setText("Apprendre (mode lent)");
        jButtonLearnSlow.setEnabled(false);
        jButtonLearnSlow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLearnSlowActionPerformed(evt);
            }
        });
        jPanel8.add(jButtonLearnSlow);

        jPanel7.add(jPanel8, java.awt.BorderLayout.NORTH);

        jPanel5.add(jPanel7, java.awt.BorderLayout.CENTER);

        jPanel2.add(jPanel5, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel2, java.awt.BorderLayout.CENTER);

        jButton1.setText("Donne moi les poids...");
        jButton1.setEnabled(false);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel3.add(jButton1);

        jButtonClose.setText("Fermer");
        jButtonClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCloseActionPerformed(evt);
            }
        });
        jPanel3.add(jButtonClose);

        getContentPane().add(jPanel3, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void jButtonCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCloseActionPerformed
    this.frm.getProps().setPerceptronList(perceptronList);
    this.dispose();
}//GEN-LAST:event_jButtonCloseActionPerformed

private void jButtonRecordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecordActionPerformed
    this.jButtonRecord.setEnabled(false);
    this.noteFound = null;
    this.jLabelStatus.setText("Enregistrement...");
    this.paint(this.getGraphics());
    Mixer rec = frm.getRecorder();
    Line.Info[] lin = rec.getTargetLineInfo();
    try {
        TargetDataLine tget = (TargetDataLine) rec.getLine(lin[0]);
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        ByteArrayOutputStream strm = new ByteArrayOutputStream();
        int numBytesRead = 0;
        byte[] data = new byte[tget.getBufferSize() / 5];
        tget.open(format);
        tget.start();
        int maxLen = 44100 * 2 * 4;    // 4 secondes.
        while (numBytesRead < maxLen) {
            numBytesRead += tget.read(data, 0, data.length);
            strm.write(data, 0, data.length);
        }
        // Données disponibles, créer le tableau de données :
        byte[] audioBytes = strm.toByteArray();
        // Maintenant faut balancer l'analyse en utilisant le perceptron chargé.
        this.analyse(audioBytes);
        if (this.noteFound != null) {
            // Libérer les boutons d'apprentissage.
            this.jComboBoxNotes.setEnabled(true);
            this.jSpinnerOctave.setEnabled(true);
            this.jButtonLearn.setEnabled(true);
            this.jButtonLearnSlow.setEnabled(true);
        } else {
            JOptionPane.showMessageDialog(this, "Aucune note plausible détectée.\nVous pouvez tenter de réduire l'amplitude min, vous rapprocher du micro et/ou jouer plus fort.\nEssayez d'avoir une grande constance dans votre son.", "Pas de bol", JOptionPane.INFORMATION_MESSAGE);
        }
        this.jLabelStatus.setText("Prêt");
    } catch (LineUnavailableException ex) {
        JOptionPane.showMessageDialog(this, "Erreur audio, ligne indisponible.\nContactez l'auteur du programme à ce propos.", "Erreur", JOptionPane.ERROR_MESSAGE);
    }
    this.jButtonRecord.setEnabled(true);
}//GEN-LAST:event_jButtonRecordActionPerformed

private void jButtonLearnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLearnActionPerformed
    Note note = new Note((String) this.jComboBoxNotes.getSelectedItem(), (Integer) this.jSpinnerOctave.getModel().getValue());
    double expectedFreq = note.calculateFreq();
    if (this.noteFound != null) {
        try {
            perceptronList[this.harmCountFound].learnUntilConvergence(expectedFreq);
        } catch (ArithmeticException ex) {
            JOptionPane.showMessageDialog(this, "Le perceptron n'a pas convergé.\nC'est la merde...", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
        this.jButtonLearn.setEnabled(false);
        this.jButtonLearnSlow.setEnabled(false);
        this.jButtonSave.setEnabled(true);
    }
}//GEN-LAST:event_jButtonLearnActionPerformed

private void jSpinnerFreqTreshStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSpinnerFreqTreshStateChanged
    Double resTresh = (Double) this.jSpinnerFreqTresh.getModel().getValue();
    for (int i = 0; i < perceptronList.length; i++) {
        if (perceptronList[i] != null) {
            perceptronList[i].setResultTresh(resTresh);
        }
    }
}//GEN-LAST:event_jSpinnerFreqTreshStateChanged

private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveActionPerformed
    // Sauvegarde par sérialisation...
    JFileChooser fc = new JFileChooser();
    fc.setCurrentDirectory(frm.getWorkingDir());
    fc.setFileFilter(new ExtensionFilter("rec", "Fichiers "));
    fc.setAcceptAllFileFilterUsed(false);
    int resp = fc.showSaveDialog(this);
    if (resp == JFileChooser.APPROVE_OPTION) {
        // Sauvegarder l'affaire...
        File dest = fc.getSelectedFile();
        try {
            FileOutputStream fout = new FileOutputStream(dest);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(perceptronList);
            oos.close();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Une erreur s'est produite à l'écriture de l'objet.\nVeuillez réessayer euh... En changeant quelque chose.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}//GEN-LAST:event_jButtonSaveActionPerformed

private void jButtonLoadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLoadActionPerformed
    JFileChooser fc = new JFileChooser();
    fc.setCurrentDirectory(frm.getWorkingDir());
    fc.setFileFilter(new ExtensionFilter("rec", "Fichiers "));
    fc.setAcceptAllFileFilterUsed(false);
    int resp = fc.showOpenDialog(this);
    if (resp == JFileChooser.APPROVE_OPTION) {
        File fil = fc.getSelectedFile();
        try {
            FileInputStream fin = new FileInputStream(fil);
            ObjectInputStream ois = new ObjectInputStream(fin);
            this.perceptronList = (FreqPerceptron[]) ois.readObject();
            ois.close();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Une erreur s'est produite à la lecture de l'objet.\nVeuillez réessayer euh... En changeant quelque chose.", "Erreur", JOptionPane.ERROR_MESSAGE);
        } catch (ClassNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "Ce fichier semble ne pas contenir des données adéquates.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }
}//GEN-LAST:event_jButtonLoadActionPerformed

private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
    for (int i = 0; i < perceptronList.length; i++) {
        if (perceptronList[i] != null) {
            for (int x = 0; x < perceptronList[i].getWeights().length; x++) {
                System.out.println(perceptronList[i].getWeights()[x]);
            }
            System.out.println();
        }
    }
}//GEN-LAST:event_jButton1ActionPerformed

private void jButtonLearnSlowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLearnSlowActionPerformed
    // Apprend mais pas jusqu'à convergence...
    Note note = new Note((String) this.jComboBoxNotes.getSelectedItem(), (Integer) this.jSpinnerOctave.getModel().getValue());
    double expectedFreq = note.calculateFreq();
    if (this.noteFound != null) {
        int count = 4;
        for (int x = 0; x < count; x++) {
            perceptronList[this.harmCountFound].learnOneStep(expectedFreq);
        }
        this.jButtonLearn.setEnabled(false);
        this.jButtonLearnSlow.setEnabled(false);
        this.jButtonSave.setEnabled(true);
    }
}//GEN-LAST:event_jButtonLearnSlowActionPerformed

private void jButtonHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonHelpActionPerformed
    HelpDialog help = new HelpDialog(frm, true, HelpDialog.AI_HELP);
    help.setLocationRelativeTo(null);
    help.setVisible(true);
}//GEN-LAST:event_jButtonHelpActionPerformed

private void jButtonNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonNewActionPerformed
    this.perceptronList = new FreqPerceptron[4];
    this.createNewPerceptron(0);
}//GEN-LAST:event_jButtonNewActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButtonClose;
    private javax.swing.JButton jButtonHelp;
    private javax.swing.JButton jButtonLearn;
    private javax.swing.JButton jButtonLearnSlow;
    private javax.swing.JButton jButtonLoad;
    private javax.swing.JButton jButtonNew;
    private javax.swing.JButton jButtonRecord;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JComboBox jComboBoxNotes;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabelFreq;
    private javax.swing.JLabel jLabelNote;
    private javax.swing.JLabel jLabelStatus;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JSpinner jSpinnerFreqTresh;
    private javax.swing.JSpinner jSpinnerMaxAmpl;
    private javax.swing.JSpinner jSpinnerOctave;
    // End of variables declaration//GEN-END:variables
}
