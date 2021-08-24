/*
 * mainFrame.java
 *
 * Created on 21 septembre 2008, 11:12
 */
package UI;

import java.io.FileNotFoundException;
import javax.swing.JOptionPane;
import javax.swing.JFileChooser;
import javax.sound.sampled.*;
import java.io.File;
import artificialcomposer.*;
import java.awt.Cursor;
import java.io.IOException;
import java.util.*;
import javax.sound.midi.*;
import AI.*;

/**
 *
 * @author  William
 */
public class mainFrame extends javax.swing.JFrame {

    // Jme sert de cette frame comme controller.
    // Toutes les infos sont centralisées ici.
    private double tessitureLow;
    private double tessitureHigh;
    private boolean tunerActive = false;
    private Mixer player;
    private Mixer recorder;
    private Timer counter;
    private Timer metronome;
    private recThread rec = null;
    private int minAmpl;
    private int instrument;
    private File workingDir;
//    private FreqPerceptron perceptron = null;
    public static final int sampNumBytes = 2048;
    private AppProperties props = null;
    private File propFil = new File("conf.cnf");

    /** Creates new form mainFrame */
    public mainFrame() {
        initComponents();
        // Hey ! Eske ça marche aussi avec ça ?:
        //workingDir = new File(""); Ben non
        // Vérifier si on a un fichier de propriétés :
        if (propFil.exists()) {
            try {
                props = new AppProperties(propFil);
            } catch (Exception ex) {
                props = new AppProperties();
            }
        } else {
            props = new AppProperties();
        }
        this.transitionProperties();
        fillFileList();
        fillAnalList();
        this.jLabelState.setText("Prêt");
    }
    
    private void transitionProperties() {
        // Cette méthode sert à faciliter la transition entre :
        // "toutes les props dans mainFrame" vers :
        // "toutes les props dans mainFrame mais... Dans une classe.".
        // Les props devraient être dans un contrôleur, je suis très sale.
        if (this.props != null) {
            this.minAmpl = props.getMinAmpl();
            this.instrument = props.getInstrument();
            Mixer.Info[] inf = AudioSystem.getMixerInfo();
            player = AudioSystem.getMixer(inf[props.getPlayerMixerNbr()]);
            recorder = AudioSystem.getMixer(inf[props.getRecMixerNbr()]);
            this.workingDir = props.getWorkingDir();
            this.tessitureHigh = props.getTessitureHigh();
            this.tessitureLow = props.getTessitureLow();
            this.workingDir = props.getWorkingDir();
            switch (props.getActiveCorrectionCode()) {
                case soundAnalyser.NO_CORREC:
                    this.jRadioButtonMenuItemNoCorrec.setSelected(true);
                    break;
                case soundAnalyser.CONC_FLUTE_CORREC:
                    this.jRadioButtonMenuItemConcFlute.setSelected(true);
                    break;
                case soundAnalyser.CRUDE_CORREC:
                    this.jRadioButtonMenuItemCrudeCorrec.setSelected(true);
                    break;
                case soundAnalyser.TRUMP_CORREC:
                    this.jRadioButtonMenuItemTrumpCorrec.setSelected(true);
                    break;
            }
            this.jCheckBoxMenuItemProlong.setSelected(props.isEchProlongation());
            this.jCheckBoxMenuItemIA.setSelected(props.isUseAI());
        }
    }

    public void metronomeSignalOn() {
        jLabelMetronome.setBackground(java.awt.Color.green);
    }

    public void metronomeSignalOff() {
        jLabelMetronome.setBackground(null);
    }

    public void inform(String text) {
        if (text != null) {
            this.jTextAreaNotif.append(text + '\n');
            this.jTextAreaNotif.setCaretPosition(this.jTextAreaNotif.getText().length());
        }
    }

    public void fillAnalList() {
        String[] anList = workingDir.list(new xmlFilter());
        if (anList != null) {
            jListAnalysis.setListData(anList);
        }
    }

    public void fillFileList() {
        // Remplis la liste de fichier wav.
        // Faut vérifier s'ils sont du bon format, fréq échantillonnage, etc.        
        AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
        wavFilter filter = new wavFilter(format);
        String[] fList = workingDir.list(filter);
        if (fList != null) {
            jListFiles.setListData(fList);
        }
    }

    private ArrayList<Note> experimentWithNotes(boolean usePerceptron) {
        // Cette méthode devrait pas être ici... Raisons historiques.
        // A cause de cette connerie, cette vue/controlleur contient maintenant
        // aussi un bout du modèle... Yeah !
        String fname = this.workingDir.getPath() + File.separator + (String) this.jListFiles.getSelectedValue();
        ArrayList<Note> notes = new ArrayList();
        try {
            if (!fname.isEmpty()) {
                // Vérifier si le fichier existe.
                File fil = new File(fname);
                if (fil.exists()) {
                    // Ouvrir le fichier et en sortir des tableaux de bits.
                    // Tampon arbitraire de 2048 bytes.
                    // Ca fait 1024 échantillons...
                    int numBytes = mainFrame.sampNumBytes;
                    try {
                        AudioInputStream inStream = AudioSystem.getAudioInputStream(fil);
                        int position = 0;
                        this.getjProgressBarShit().setValue(0);
                        this.getjProgressBarShit().setMaximum((int) fil.length());
                        byte[] audioBytes = new byte[numBytes];
                        int numBytesRead = 0;
                        int progressPos = 0;
                        while ((numBytesRead = inStream.read(audioBytes)) != -1) {
                            // Mise à jour de la barre de progression :
                            position += numBytesRead;
                            if (position >= progressPos + 100000) {
                                progressPos = position;
                                this.getjProgressBarShit().setValue(position);
                                this.paint(this.getGraphics());
                            }
                            //this.getjProgressBarShit().paint(this.getjProgressBarShit().getGraphics());
                            double fondaC = 0.0;
                            if (!usePerceptron) {
                                // Faudra vérifier ici si l'amplitude fait que c'est un silence...
                                int maxi = soundAnalyser.findMaxAmplitude(audioBytes);
                                // Si le max est plus petit que 1550, ça a l'air d'être du bruit...
                                if (maxi <= this.minAmpl) {
                                    if (!notes.isEmpty() && (notes.get(notes.size() - 1).getNote() == Note.SILENCE)) {
                                        notes.get(notes.size() - 1).incrementDuration();
                                    } else {
                                        notes.add(new Note(Note.SILENCE, 1));
                                    }
                                    continue;
                                }
                                if (this.jRadioButtonMenuItemTrumpCorrec.isSelected()) {
                                    fondaC = soundAnalyser.findFundamental(audioBytes, numBytesRead, 44100, soundAnalyser.TRUMP_CORREC, this.jCheckBoxMenuItemProlong.isSelected());
                                } else if (this.jRadioButtonMenuItemConcFlute.isSelected()) {
                                    fondaC = soundAnalyser.findFundamental(audioBytes, numBytesRead, 44100, soundAnalyser.CONC_FLUTE_CORREC, this.jCheckBoxMenuItemProlong.isSelected());
                                } else if (this.jRadioButtonMenuItemCrudeCorrec.isSelected()) {
                                    fondaC = soundAnalyser.findFundamental(audioBytes, numBytesRead, 44100, soundAnalyser.CRUDE_CORREC, this.jCheckBoxMenuItemProlong.isSelected());
                                } else {
                                    fondaC = soundAnalyser.findFundamental(audioBytes, numBytesRead, 44100, soundAnalyser.NO_CORREC, this.jCheckBoxMenuItemProlong.isSelected());
                                }
                            } else {
                                // Trouve la fondamentale en utilisant le perceptron.
//                                Spectrum spectre = soundAnalyser.findReducedSpectrum(audioBytes, 44100, this.minAmpl - 300, this.perceptron.getWeights().length, this.jCheckBoxMenuItemProlong.isSelected());
                                Spectrum spectre = soundAnalyser.findReducedSpectrum(audioBytes, 44100, this.minAmpl - 300, 4, this.jCheckBoxMenuItemProlong.isSelected());
                                int nbrBytes;
                                if (this.jCheckBoxMenuItemProlong.isSelected()) {
                                    nbrBytes = numBytes * 8;
                                } else {
                                    nbrBytes = mainFrame.sampNumBytes;
                                }
                                int[] maxims = spectre.getIndices();
                                if (maxims[0] != 0) {
                                    double[] entries = new double[maxims.length];
                                    for (int i = 0; i < maxims.length; i++) {
                                        entries[i] = maxims[i] * 44100.0 * 2 / nbrBytes;
                                    }
                                    // Montre le spectre :
//                                    System.out.println("Nvo spectre :");
//                                    for (int i = 0; i < entries.length; i++) {
//                                        System.out.println(entries[i]);
//                                    }
                                    int harmCount = 0;
                                    for (int i = 1; i < maxims.length; i++) {
                                        if (maxims[i] != 0) {
                                            harmCount++;
                                        }
                                    }
                                    FreqPerceptron perceptronL[] = this.props.getPerceptronList();
                                    if (perceptronL[harmCount] != null) {
                                        perceptronL[harmCount].setEntries(entries);
                                        fondaC = perceptronL[harmCount].processEntries();
                                    } else {
                                        // Utiliser un des autres...
                                        for (int it = 0; it < perceptronL.length; it++) {
                                            if (perceptronL[it] != null && it != harmCount) {
                                                perceptronL[it].setEntries(entries);
                                                fondaC = perceptronL[harmCount].processEntries();
                                            }
                                        }
                                    }
                                } else {
                                    if (!notes.isEmpty() && (notes.get(notes.size() - 1).getNote() == Note.SILENCE)) {
                                        notes.get(notes.size() - 1).incrementDuration();
                                    } else {
                                        notes.add(new Note(Note.SILENCE, 1));
                                    }
                                    continue;
                                }
                            }
                            if (fondaC > tessitureHigh || fondaC < tessitureLow) {
                                if (!notes.isEmpty() && (notes.get(notes.size() - 1).getNote() == Note.SILENCE)) {
                                    notes.get(notes.size() - 1).incrementDuration();
                                } else {
                                    notes.add(new Note(Note.SILENCE, 1));
                                }
                                continue;
                            }
                            Note note = new Note(fondaC);
                            if (!notes.isEmpty() && (notes.get(notes.size() - 1).getNote() == note.getNote() && notes.get(notes.size() - 1).getOctave() == note.getOctave())) {
                                notes.get(notes.size() - 1).incrementDuration();
                            } else {
                                notes.add(note);
                            }
                        }
                        inStream.close();
                        // Nettoyage des notes de durée 1.
                        // Afficher toutes les notes.
                        for (int x = 0; x < notes.size(); x++) {
                            if (notes.get(x).getDuration() <= 3 && x >= 1) {
                                notes.get(x - 1).incrementDuration();
                                notes.remove(x);
                                x--;
                            }
                            if (x >= 1 && (notes.get(x).getNote() == notes.get(x - 1).getNote() && notes.get(x).getOctave() == notes.get(x - 1).getOctave())) {
                                notes.get(x - 1).setDuration(notes.get(x - 1).getDuration() + notes.get(x).getDuration());
                                notes.remove(x);
                                x--;
                            }
                        }
                    } catch (IOException ex) {
                        inform("Erreur à l'ouverture du fichier, description :" + ex.getMessage());
                    } catch (UnsupportedAudioFileException ex) {
                        inform("Fichier audio non supporté :" + ex.getMessage());
                    }
                }
            }
        } catch (NullPointerException ex) {
            inform("Vous devez sélectionner un extrait à analyser.");
        }
        this.getjProgressBarShit().setValue(0);
        return notes;
    }

    private void experimentalShit() {
        // Experimental playground...
        inform("Cette option est expérimentale, elle analyse pas vraiment...");
        String fname = (String) this.jListFiles.getSelectedValue();
        try {
            if (!fname.isEmpty()) {
                // Vérifier si le fichier existe.
                File fil = new File(fname);
                if (fil.exists()) {
                    // Ouvrir le fichier et en sortir des tableaux de bits.
                    try {
                        AudioInputStream inStream = AudioSystem.getAudioInputStream(fil);
                        // Tampon arbitraire de 2048 bytes.
                        int numBytes = 2048;

                        byte[] audioBytes = new byte[numBytes];
                        int numBytesRead = 0;
                        double fonda = 0.0;
                        double prevFound = 0.0;
                        int count = 0;
                        int valCount = 3;
                        //double[] history = new double[valCount];
                        while ((numBytesRead = inStream.read(audioBytes)) != -1) {
                            double fondaC = soundAnalyser.findFundamental(audioBytes, numBytesRead, 44100, soundAnalyser.CONC_FLUTE_CORREC, true);
                            // System.out.println(fonda);
                            if (!(fondaC > fonda - 5 && fondaC < fonda + 5)) {
                                // 2000 c'est presk un contre-contre-contre ut.
                                // On a trouvé une note différente de la précédente.
                                // Avec la flûte traversière, on a tendance à tomber sur le double
                                // de la bonne fréquence...
                                fonda = fondaC;
                                count = 0;
                            } else if (fondaC > tessitureLow && fondaC < tessitureHigh) {
                                count++;
                                if (count == valCount) {
                                    fonda = fondaC;
                                    if (!(fonda > prevFound - 5 && fonda < prevFound + 5)) {
                                        inform("Trouvé fondamentale à " + fonda + "hz.");
                                        Note note = new Note(fonda);
                                        inform("Nom de la note : " + note.toString());
                                        prevFound = fonda;
                                    }
                                }
                            }
                        }
                        inStream.close();
                    } catch (Exception ex) {
                        inform("Erreur à l'ouverture du fichier, description :" + ex.getMessage());
                    }
                }
            }
        } catch (NullPointerException ex) {
            // Ouai.
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroupCorrec = new javax.swing.ButtonGroup();
        jPanel1 = new javax.swing.JPanel();
        jPanelNotif = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaNotif = new javax.swing.JTextArea();
        jPanel4 = new javax.swing.JPanel();
        jPanelRecs = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jListFiles = new javax.swing.JList();
        jPanel5 = new javax.swing.JPanel();
        jButtonAnalyse = new javax.swing.JButton();
        jButtonAnalyseAndSave = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        jButtonPlay = new javax.swing.JButton();
        jPanelAnal = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jListAnalysis = new javax.swing.JList();
        jPanel6 = new javax.swing.JPanel();
        jButtonPlayNotes = new javax.swing.JButton();
        jButtonEdit = new javax.swing.JButton();
        jButtonDeleteNotes = new javax.swing.JButton();
        jButtonCompose = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanelRecord = new javax.swing.JPanel();
        jLabelCounter = new javax.swing.JLabel();
        jButtonRecord = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldTempo = new javax.swing.JTextField();
        jToggleButtonMetronome = new javax.swing.JToggleButton();
        jLabelMetronome = new javax.swing.JLabel();
        jPanelStatus = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabelState = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jProgressBarShit = new javax.swing.JProgressBar();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemNewAnalyse = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JSeparator();
        jMenuItemChangeWorkingDir = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        jMenuItemQuit = new javax.swing.JMenuItem();
        jMenuAnalyse = new javax.swing.JMenu();
        jMenuItemSeuil = new javax.swing.JMenuItem();
        jCheckBoxMenuItemProlong = new javax.swing.JCheckBoxMenuItem();
        jMenuPartCorrections = new javax.swing.JMenu();
        jRadioButtonMenuItemNoCorrec = new javax.swing.JRadioButtonMenuItem();
        jRadioButtonMenuItemConcFlute = new javax.swing.JRadioButtonMenuItem();
        jRadioButtonMenuItemCrudeCorrec = new javax.swing.JRadioButtonMenuItem();
        jRadioButtonMenuItemTrumpCorrec = new javax.swing.JRadioButtonMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        jCheckBoxMenuItemIA = new javax.swing.JCheckBoxMenuItem();
        jMenuItemConfigIA = new javax.swing.JMenuItem();
        jMenuView = new javax.swing.JMenu();
        jMenuItemRefresh = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        jMenuItemClearText = new javax.swing.JMenuItem();
        jMenuTools = new javax.swing.JMenu();
        jMenuItemConfig = new javax.swing.JMenuItem();
        jMenuItemTessiture = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        jMenuItemTuner = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        jMenuItemInstrChange = new javax.swing.JMenuItem();
        jMenuHelp = new javax.swing.JMenu();
        jMenuItemHelp = new javax.swing.JMenuItem();
        jMenuItemTroubleshoot = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("CIFI le compositeur infernal");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanelNotif.setBorder(javax.swing.BorderFactory.createTitledBorder("Notifications"));
        jPanelNotif.setLayout(new java.awt.BorderLayout());

        jTextAreaNotif.setColumns(20);
        jTextAreaNotif.setLineWrap(true);
        jTextAreaNotif.setRows(5);
        jTextAreaNotif.setWrapStyleWord(true);
        jScrollPane1.setViewportView(jTextAreaNotif);

        jPanelNotif.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanelNotif, java.awt.BorderLayout.CENTER);

        jPanel4.setLayout(new java.awt.GridLayout(1, 2));

        jPanelRecs.setBorder(javax.swing.BorderFactory.createTitledBorder("Enregistrements"));
        jPanelRecs.setLayout(new java.awt.BorderLayout());

        jScrollPane2.setPreferredSize(new java.awt.Dimension(250, 150));

        jListFiles.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jListFiles.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jListFilesMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(jListFiles);

        jPanelRecs.add(jScrollPane2, java.awt.BorderLayout.NORTH);

        jPanel5.setLayout(new java.awt.GridLayout(2, 2));

        jButtonAnalyse.setText("Analyser");
        jButtonAnalyse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAnalyseActionPerformed(evt);
            }
        });
        jPanel5.add(jButtonAnalyse);

        jButtonAnalyseAndSave.setText("Analyser et sauver");
        jButtonAnalyseAndSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAnalyseAndSaveActionPerformed(evt);
            }
        });
        jPanel5.add(jButtonAnalyseAndSave);

        jButtonDelete.setText("Supprimer");
        jButtonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteActionPerformed(evt);
            }
        });
        jPanel5.add(jButtonDelete);

        jButtonPlay.setText("Jouer sélection");
        jButtonPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPlayActionPerformed(evt);
            }
        });
        jPanel5.add(jButtonPlay);

        jPanelRecs.add(jPanel5, java.awt.BorderLayout.CENTER);

        jPanel4.add(jPanelRecs);

        jPanelAnal.setBorder(javax.swing.BorderFactory.createTitledBorder("Extraits analysés"));
        jPanelAnal.setLayout(new java.awt.BorderLayout());

        jScrollPane3.setPreferredSize(new java.awt.Dimension(250, 150));

        jListAnalysis.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jListAnalysisMouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(jListAnalysis);

        jPanelAnal.add(jScrollPane3, java.awt.BorderLayout.NORTH);

        jPanel6.setLayout(new java.awt.GridLayout(2, 0));

        jButtonPlayNotes.setText("Jouer");
        jButtonPlayNotes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPlayNotesActionPerformed(evt);
            }
        });
        jPanel6.add(jButtonPlayNotes);

        jButtonEdit.setText("Editer");
        jButtonEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonEditActionPerformed(evt);
            }
        });
        jPanel6.add(jButtonEdit);

        jButtonDeleteNotes.setText("Supprimer");
        jButtonDeleteNotes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteNotesActionPerformed(evt);
            }
        });
        jPanel6.add(jButtonDeleteNotes);

        jButtonCompose.setText("Composer");
        jButtonCompose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonComposeActionPerformed(evt);
            }
        });
        jPanel6.add(jButtonCompose);

        jPanelAnal.add(jPanel6, java.awt.BorderLayout.CENTER);

        jPanel4.add(jPanelAnal);

        jPanel1.add(jPanel4, java.awt.BorderLayout.NORTH);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanelRecord.setLayout(new java.awt.GridBagLayout());

        jLabelCounter.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelCounter.setText("Temps écoulé : 0");
        jLabelCounter.setPreferredSize(new java.awt.Dimension(120, 14));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        jPanelRecord.add(jLabelCounter, gridBagConstraints);

        jButtonRecord.setText("Enregistrer");
        jButtonRecord.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRecordActionPerformed(evt);
            }
        });
        jPanelRecord.add(jButtonRecord, new java.awt.GridBagConstraints());

        jLabel1.setText("Tempo :");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(30, 0, 0, 0);
        jPanelRecord.add(jLabel1, gridBagConstraints);

        jTextFieldTempo.setText("120");
        jTextFieldTempo.setPreferredSize(new java.awt.Dimension(60, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        jPanelRecord.add(jTextFieldTempo, gridBagConstraints);

        jToggleButtonMetronome.setText("Metronome");
        jToggleButtonMetronome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jToggleButtonMetronomeActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(30, 0, 0, 0);
        jPanelRecord.add(jToggleButtonMetronome, gridBagConstraints);

        jLabelMetronome.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelMetronome.setOpaque(true);
        jLabelMetronome.setPreferredSize(new java.awt.Dimension(50, 50));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        jPanelRecord.add(jLabelMetronome, gridBagConstraints);

        jPanel2.add(jPanelRecord, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel2, java.awt.BorderLayout.EAST);

        jPanelStatus.setLayout(new java.awt.BorderLayout(50, 0));

        jLabel2.setText("Etat :");
        jPanel3.add(jLabel2);

        jLabelState.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelState.setPreferredSize(new java.awt.Dimension(280, 14));
        jPanel3.add(jLabelState);

        jPanelStatus.add(jPanel3, java.awt.BorderLayout.WEST);

        jPanel7.setLayout(new java.awt.BorderLayout());

        jProgressBarShit.setPreferredSize(new java.awt.Dimension(146, 16));
        jPanel7.add(jProgressBarShit, java.awt.BorderLayout.CENTER);

        jPanelStatus.add(jPanel7, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanelStatus, java.awt.BorderLayout.SOUTH);

        jMenuFile.setMnemonic('f');
        jMenuFile.setText("Fichier");

        jMenuItemNewAnalyse.setText("Nouveau fichier d'analyse vierge...");
        jMenuItemNewAnalyse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemNewAnalyseActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemNewAnalyse);
        jMenuFile.add(jSeparator6);

        jMenuItemChangeWorkingDir.setText("Changer de répertoire de travail...");
        jMenuItemChangeWorkingDir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemChangeWorkingDirActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemChangeWorkingDir);
        jMenuFile.add(jSeparator4);

        jMenuItemQuit.setMnemonic('q');
        jMenuItemQuit.setText("Quitter");
        jMenuItemQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemQuitActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemQuit);

        jMenuBar1.add(jMenuFile);

        jMenuAnalyse.setMnemonic('A');
        jMenuAnalyse.setText("Analyse");

        jMenuItemSeuil.setText("Seuil d'amplitude min...");
        jMenuItemSeuil.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSeuilActionPerformed(evt);
            }
        });
        jMenuAnalyse.add(jMenuItemSeuil);

        jCheckBoxMenuItemProlong.setSelected(true);
        jCheckBoxMenuItemProlong.setText("Prolongation des éch. (lent)");
        jCheckBoxMenuItemProlong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemProlongActionPerformed(evt);
            }
        });
        jMenuAnalyse.add(jCheckBoxMenuItemProlong);

        jMenuPartCorrections.setMnemonic('c');
        jMenuPartCorrections.setText("Corrections particulières");

        buttonGroupCorrec.add(jRadioButtonMenuItemNoCorrec);
        jRadioButtonMenuItemNoCorrec.setText("Pas de correction");
        jRadioButtonMenuItemNoCorrec.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonMenuItemNoCorrecActionPerformed(evt);
            }
        });
        jMenuPartCorrections.add(jRadioButtonMenuItemNoCorrec);

        buttonGroupCorrec.add(jRadioButtonMenuItemConcFlute);
        jRadioButtonMenuItemConcFlute.setText("Proportionnel balancé");
        jRadioButtonMenuItemConcFlute.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonMenuItemConcFluteActionPerformed(evt);
            }
        });
        jMenuPartCorrections.add(jRadioButtonMenuItemConcFlute);

        buttonGroupCorrec.add(jRadioButtonMenuItemCrudeCorrec);
        jRadioButtonMenuItemCrudeCorrec.setText("Simple");
        jRadioButtonMenuItemCrudeCorrec.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonMenuItemCrudeCorrecActionPerformed(evt);
            }
        });
        jMenuPartCorrections.add(jRadioButtonMenuItemCrudeCorrec);

        buttonGroupCorrec.add(jRadioButtonMenuItemTrumpCorrec);
        jRadioButtonMenuItemTrumpCorrec.setText("Trompette");
        jRadioButtonMenuItemTrumpCorrec.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonMenuItemTrumpCorrecActionPerformed(evt);
            }
        });
        jMenuPartCorrections.add(jRadioButtonMenuItemTrumpCorrec);

        jMenuAnalyse.add(jMenuPartCorrections);
        jMenuAnalyse.add(jSeparator5);

        jCheckBoxMenuItemIA.setText("Utiliser IA harmonique");
        jCheckBoxMenuItemIA.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxMenuItemIAActionPerformed(evt);
            }
        });
        jMenuAnalyse.add(jCheckBoxMenuItemIA);

        jMenuItemConfigIA.setText("Configurer IA harmonique...");
        jMenuItemConfigIA.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemConfigIAActionPerformed(evt);
            }
        });
        jMenuAnalyse.add(jMenuItemConfigIA);

        jMenuBar1.add(jMenuAnalyse);

        jMenuView.setMnemonic('c');
        jMenuView.setText("Affichage");

        jMenuItemRefresh.setMnemonic('r');
        jMenuItemRefresh.setText("Rafraîchir les listes");
        jMenuItemRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemRefreshActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuItemRefresh);
        jMenuView.add(jSeparator3);

        jMenuItemClearText.setMnemonic('v');
        jMenuItemClearText.setText("Vider texte");
        jMenuItemClearText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemClearTextActionPerformed(evt);
            }
        });
        jMenuView.add(jMenuItemClearText);

        jMenuBar1.add(jMenuView);

        jMenuTools.setMnemonic('o');
        jMenuTools.setText("Outils");

        jMenuItemConfig.setMnemonic('c');
        jMenuItemConfig.setText("Configuration son...");
        jMenuItemConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemConfigActionPerformed(evt);
            }
        });
        jMenuTools.add(jMenuItemConfig);

        jMenuItemTessiture.setMnemonic('t');
        jMenuItemTessiture.setText("Tessiture de l'instrument...");
        jMenuItemTessiture.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemTessitureActionPerformed(evt);
            }
        });
        jMenuTools.add(jMenuItemTessiture);
        jMenuTools.add(jSeparator2);

        jMenuItemTuner.setMnemonic('a');
        jMenuItemTuner.setText("Accordeur...");
        jMenuItemTuner.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemTunerActionPerformed(evt);
            }
        });
        jMenuTools.add(jMenuItemTuner);
        jMenuTools.add(jSeparator1);

        jMenuItemInstrChange.setText("Changer d'instrument MIDI...");
        jMenuItemInstrChange.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemInstrChangeActionPerformed(evt);
            }
        });
        jMenuTools.add(jMenuItemInstrChange);

        jMenuBar1.add(jMenuTools);

        jMenuHelp.setMnemonic('i');
        jMenuHelp.setText("Aide");

        jMenuItemHelp.setMnemonic('c');
        jMenuItemHelp.setText("Contenu...");
        jMenuItemHelp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemHelpActionPerformed(evt);
            }
        });
        jMenuHelp.add(jMenuItemHelp);

        jMenuItemTroubleshoot.setMnemonic('m');
        jMenuItemTroubleshoot.setText("Ça marche pas...");
        jMenuItemTroubleshoot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemTroubleshootActionPerformed(evt);
            }
        });
        jMenuHelp.add(jMenuItemTroubleshoot);

        jMenuBar1.add(jMenuHelp);

        setJMenuBar(jMenuBar1);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-712)/2, (screenSize.height-614)/2, 712, 614);
    }// </editor-fold>//GEN-END:initComponents
    private void jMenuItemQuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemQuitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_jMenuItemQuitActionPerformed

    private void jMenuItemHelpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemHelpActionPerformed
        //JOptionPane.showMessageDialog(this, "FUCK l'aide.", "Aide", JOptionPane.INFORMATION_MESSAGE);
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        HelpDialog diag = new HelpDialog(this, false, HelpDialog.GENERAL_HELP);
        this.setCursor(Cursor.getDefaultCursor());
        diag.setLocationRelativeTo(null);
        diag.setVisible(true);
    }//GEN-LAST:event_jMenuItemHelpActionPerformed

    private void jListFilesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jListFilesMouseClicked
        if (evt.getClickCount() == 2) {
            this.jButtonPlayActionPerformed(null);
        }
    }//GEN-LAST:event_jListFilesMouseClicked

    private void jButtonRecordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRecordActionPerformed
        if (rec == null) {
            this.jLabelState.setText("Enregistrement en cours...");
            this.jButtonRecord.setText("Stop");
            this.getJLabelCounter().setText("Temps écoulé : 0");
            rec = new recThread(this);
            counter = new Timer();
            counter.scheduleAtFixedRate(new timeCounter(this), 0, 1000);
            rec.start();
        } else {
            this.jLabelState.setText("Prêt");
            this.jButtonRecord.setText("Enregistrer");
            rec.getRcl().stop();
            rec = null;
            counter.cancel();
        }
    }//GEN-LAST:event_jButtonRecordActionPerformed

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteActionPerformed
        String fname = this.workingDir.getPath() + File.separator + (String) this.jListFiles.getSelectedValue();
        try {
            if (!fname.isEmpty()) {
                // Vérifier si le fichier existe.
                File fil = new File(fname);
                if (fil.exists()) {
                    // Demande confirmation.
                    int resp = JOptionPane.showConfirmDialog(this, "Confirmez vous la suppresson de " + fil.getName() + " ?", "Supprimer", JOptionPane.YES_NO_OPTION);
                    if (resp == JOptionPane.YES_OPTION) {
                        fil.delete();
                        if (fil.exists()) {
                            inform("Y'a eu comme un problème...");
                        } else {
                            inform(fil.getName() + " a été supprimé.");
                        }
                    }
                }
                this.fillFileList();
            }
        } catch (NullPointerException ex) {
            // Ouai.
        }
    }//GEN-LAST:event_jButtonDeleteActionPerformed

    private void jButtonAnalyseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAnalyseActionPerformed
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        this.jLabelState.setText("Analyse en cours...");
        inform("Analyse en cours...");
        inform("Utilisez les règlages du menu Outils pour améliorer la reconnaissance.");
        inform("Cette fonction ne sert qu'à vérifier l'analyse et ne crée aucun fichier sur le disque.");
        ArrayList<Note> notes = this.experimentWithNotes(this.jCheckBoxMenuItemIA.isSelected());
        int tempo = 0;
//        int tickDuration = (int)Math.round((2048 / (double)44100) * 1000);
        int tickDuration = 23;
        int quarterDuration = 23;
        try {
            tempo = Integer.parseInt(this.jTextFieldTempo.getText());
            quarterDuration = (int) Math.round(1000 / (tempo / (double) 60));
        } catch (NumberFormatException ex) {
            // Rien.
        }
        for (int x = 0; x < notes.size(); x++) {
            inform("Note trouvée : " + notes.get(x).toString() + " fréquence " + notes.get(x).getFreq());
            if (tempo != 0) {
                notes.get(x).findNoteValue(quarterDuration, tickDuration, Note.SIXTEENTH);
                inform("Nombre de ticks pour tempo " + tempo + " : " + notes.get(x).getNoteValue());
            }
        }
        inform("");
        this.jLabelState.setText("Prêt");
        this.setCursor(Cursor.getDefaultCursor());
    }//GEN-LAST:event_jButtonAnalyseActionPerformed

    private void jMenuItemConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemConfigActionPerformed
        deviceSelector sel = new deviceSelector(this);
        sel.setLocationRelativeTo(null);
        sel.setVisible(true);
    }//GEN-LAST:event_jMenuItemConfigActionPerformed

    private void jMenuItemTessitureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemTessitureActionPerformed
        tessitureSel sel = new tessitureSel(this);
        sel.setLocationRelativeTo(null);
        sel.setVisible(true);
    }//GEN-LAST:event_jMenuItemTessitureActionPerformed

    private void jButtonPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlayActionPerformed
        if (!this.jListFiles.isSelectionEmpty()) {
            String fname = this.workingDir.getPath() + File.separator + (String) this.jListFiles.getSelectedValue();
            File theFile = new File(fname);
            playerThread pl = new playerThread(theFile, this);
            pl.start();
        }
    }//GEN-LAST:event_jButtonPlayActionPerformed

    private void jMenuItemTunerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemTunerActionPerformed
        if (!tunerActive) {
            if (rec != null) {
                if (rec.isAlive()) {
                    JOptionPane.showMessageDialog(this, "Un enregistrement est en cours.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            tunerActive = true;
            tuner tun = new tuner(this);
            tun.setLocationRelativeTo(null);
            tun.setVisible(true);
        }
    }//GEN-LAST:event_jMenuItemTunerActionPerformed

private void jMenuItemSeuilActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSeuilActionPerformed
    String resp = JOptionPane.showInputDialog(this, "Veuillez saisir l'amplitude sonore minimum à considérer dans la reconaissance de notes.\nLe mieux est d'essayer de monter cette valeur le plus haut possible en enregistrant à proximité du microphone.", Integer.toString(this.minAmpl));
    if (resp != null && !resp.isEmpty()) {
        try {
            int val = Integer.parseInt(resp);
            if (val >= 0) {
                this.minAmpl = val;
                props.setMinAmpl(minAmpl);
            }
        } catch (NumberFormatException ex) {
            // Rien.
        }
    }
}//GEN-LAST:event_jMenuItemSeuilActionPerformed

private void jMenuItemRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemRefreshActionPerformed
    this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    this.fillFileList();
    this.fillAnalList();
    this.setCursor(Cursor.getDefaultCursor());
}//GEN-LAST:event_jMenuItemRefreshActionPerformed

private void jMenuItemClearTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemClearTextActionPerformed
    this.jTextAreaNotif.setText("");
}//GEN-LAST:event_jMenuItemClearTextActionPerformed

private void jToggleButtonMetronomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButtonMetronomeActionPerformed
    if (this.jToggleButtonMetronome.isSelected()) {
        // Calculer le rate.
        int tempo;
        try {
            tempo = Integer.parseInt(this.jTextFieldTempo.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Erreur dans la lecture du tempo. Veuillez l'écrire correctement (entier, noires par minutes).", "Erreur", JOptionPane.ERROR_MESSAGE);
            this.jToggleButtonMetronome.setSelected(false);
            return;
        }
        this.metronome = new Timer();
        int dureeNoire = 1000 / (tempo / 60); // Division entière.

        TimerTask zgeg = new metronomeTimer(this);
        metronome.scheduleAtFixedRate(zgeg, 0, dureeNoire);
    } else {
        this.metronome.cancel();
        this.metronome = null;
    }
}//GEN-LAST:event_jToggleButtonMetronomeActionPerformed

private void jButtonAnalyseAndSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAnalyseAndSaveActionPerformed
    String fname = this.workingDir.getPath() + File.separator + (String) this.jListFiles.getSelectedValue();
    if (fname == null) {
        inform("Vous devez sélectionner un extrait à analyser.");
        return;
    }
    String resp = JOptionPane.showInputDialog(this, "Veuillez saisir le tempo de référence de l'extrait (noires / min).\nLes durées des notes sont déterminées par ce paramètre.", this.jTextFieldTempo.getText());
    int tempo;
    try {
        if (resp != null && !resp.isEmpty()) {
            tempo = Integer.parseInt(resp);
            if (tempo <= 0) {
                throw new NumberFormatException();
            }
        } else {
            return;
        }
    } catch (NumberFormatException ex) {
        JOptionPane.showMessageDialog(this, "Il convient d'entrer un tempo entier positif.", "Erreur", JOptionPane.ERROR_MESSAGE);
        return;
    }
    // Vérifier si le fichier xml existe pas déjà...
    String xmlFileName = fname.replaceAll(".wav", ".xml");
    File fil = new File(xmlFileName);
    if (fil.exists()) {
        int respo = JOptionPane.showConfirmDialog(this, "Le fichier XML cible de l'analyse existe déjà.\nVoulez-vous l'écraser ?\nCliquez sur non pour annuler l'analyse.", "Fichier d'analyse existant", JOptionPane.YES_NO_OPTION);
        if (respo == JOptionPane.NO_OPTION) {
            return;
        }
    }
    this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    this.jLabelState.setText("Analyse en cours...");
    inform("Analyse en cours en vue de sauvegarde sur le disque...");
    ArrayList<Note> notes = this.experimentWithNotes(this.jCheckBoxMenuItemIA.isSelected());
    int tickDuration = 23;
    int quarterDuration = (int) Math.round(1000 / (tempo / (double) 60));
    // Retirer le silence du début et de la fin :
    if (notes.size() > 0) {
        inform("Suppression des silences au début et à la fin...");
        if (notes.get(0).getNote() == Note.SILENCE) {
            notes.remove(0);
        }
        if (notes.get(notes.size() - 1).getNote() == Note.SILENCE) {
            notes.remove(notes.size() - 1);
        }
    }
    for (int x = 0; x < notes.size(); x++) {
        inform("Note trouvée : " + notes.get(x).toString() + " fréquence " + notes.get(x).getFreq());
        if (tempo != 0) {
            notes.get(x).findNoteValue(quarterDuration, tickDuration, Note.SIXTEENTH);
            // Retirer les notes à durée nulle.
            if (notes.get(x).getNoteValue() <= 0) {
                notes.remove(x);
                inform("\tNote retirée : jugée à durée négligeable.");
                x--;
            } else {
                inform("Nombre de ticks pour tempo " + tempo + " : " + notes.get(x).getNoteValue());
            }
        }
    }
    if (notes.size() == 0) {
        inform("Aucune note trouvée, aucun fichier n'a été créé.");
        return;
    }
    // On est prêt pour la sauvegarde en XML.
    this.jLabelState.setText("Sauvegarde de l'analyse...");
    MusicScore score;
    try {
        score = new MusicScore(fil);
        score.setNotes(notes);
        score.notesToXML(tempo, fname);
        inform("Fichier " + fil.getName() + " écrit avec succès.");
        fillAnalList();
    } catch (FileNotFoundException ex) {
        inform("Erreur critique à l'écriture du fichier XML :\nEcritude impossible.");
    } catch (IOException ex) {
        inform("Erreur d'entrée/sortie à l'écritude du fichier XML.");
    } finally {
        this.jLabelState.setText("Prêt");
        score = null;
        System.gc();
        this.setCursor(Cursor.getDefaultCursor());
    }
}//GEN-LAST:event_jButtonAnalyseAndSaveActionPerformed

private void jButtonDeleteNotesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteNotesActionPerformed
    Object[] selFiles = this.jListAnalysis.getSelectedValues();
    if (selFiles == null) {
        return;
    }
    for (int x = 0; x < selFiles.length; x++) {
        File fil = new File(this.workingDir.getPath() + File.separator + (String) selFiles[x]);
        if (fil.exists()) {
            int resp = JOptionPane.showConfirmDialog(this, "Confirmez vous la suppresson de " + fil.getName() + " ?", "Supprimer", JOptionPane.YES_NO_OPTION);
            if (resp == JOptionPane.YES_OPTION) {
                fil.delete();
                selFiles = this.jListAnalysis.getSelectedValues();
                if (fil.exists()) {
                    inform("Y'a eu comme un problème... Java est pas très sympa avec la fermeture des fichiers.\nFaudra attendre que la Garbage Collection ait eu lieu.");
                    System.gc();
                } else {
                    inform(fil.getName() + " (fichier d'analyse) a été supprimé.");
                }
                this.fillAnalList();
            }
        }
    }
}//GEN-LAST:event_jButtonDeleteNotesActionPerformed

private void jButtonEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonEditActionPerformed
    String selFile = this.workingDir.getPath() + File.separator + (String) this.jListAnalysis.getSelectedValue();
    if (selFile != null && !selFile.isEmpty()) {
        File xml = new File(selFile);
        if (xml.exists()) {
            MusicScore score = new MusicScore(xml);
            score.XMLToNotes();
            if (score.getNotes().size() == 0) {
                JOptionPane.showMessageDialog(this, "Cette partition est vide.", "Attention", JOptionPane.INFORMATION_MESSAGE);
            }
            EditScoreDialog diag = new EditScoreDialog(this, true, score);
            diag.setLocationRelativeTo(null);
            diag.setVisible(true);
            score = null;
        }
    }
}//GEN-LAST:event_jButtonEditActionPerformed

private void jButtonPlayNotesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPlayNotesActionPerformed
    String selFile = this.workingDir.getPath() + File.separator + (String) this.jListAnalysis.getSelectedValue();
    if (selFile != null && !selFile.isEmpty()) {
        File xml = new File(selFile);
        if (xml.exists()) {
            MusicScore score = new MusicScore(xml);
            score.XMLToNotes();
            if (score.getNotes().size() == 0) {
                JOptionPane.showMessageDialog(this, "Cette partition est vide.", "Attention", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            try {
                ScorePlayer MidiPlay = new ScorePlayer(score, this.instrument);
                MidiPlay.start();
            } catch (InvalidMidiDataException ex) {
                inform("Problème à la lecture des données MIDI.\nIl se peut qu'une note soit incorrecte (octave ? Valeur ?)");
            } catch (MidiUnavailableException ex) {
                inform("Un séquenceur MIDI adéquat n'a pas pu être ouvert.Cette erreur peut avoir un grand nombre de causes. Exemples:\nPermissions insufisantes sur le système de son.\nCarte son nom compatible avec cette version du programme.\nJVM trop ancienne.\n");
            }
        }
    }
}//GEN-LAST:event_jButtonPlayNotesActionPerformed

private void jListAnalysisMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jListAnalysisMouseClicked
    if (evt.getClickCount() >= 2) {
        this.jButtonPlayNotesActionPerformed(null);
    }
}//GEN-LAST:event_jListAnalysisMouseClicked

private void jMenuItemInstrChangeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemInstrChangeActionPerformed
    String resp = JOptionPane.showInputDialog(this, "Veuillez saisir le code d'instrument MIDI à utiliser.\nL'auteur s'excuse pour l'horrible pauvreté de cette option d'interface...\nAh oui, les codes vont de 0 à 96.", Integer.toString(this.instrument));
    if (resp != null && !resp.isEmpty()) {
        try {
            int instr = Integer.parseInt(resp);
            if (instr <= 0 || instr > 96) {
                throw new NumberFormatException();
            }
            this.instrument = instr;
            props.setInstrument(instrument);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Numéro d'instrument incorrect. Modification non pris en compte.", "Erreurr", JOptionPane.ERROR_MESSAGE);
        }
    }
}//GEN-LAST:event_jMenuItemInstrChangeActionPerformed

private void jMenuItemNewAnalyseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemNewAnalyseActionPerformed
    // Demander le nom de fichier, sans .xml...
    File curDir = new File(this.workingDir.getPath() + File.separator + "extrait.xml");
    int iter = 1;
    while (true) {
        curDir = new File(this.workingDir.getPath() + File.separator + "extrait_" + iter + ".xml");
        if (!curDir.exists()) {
            break;
        }
        iter++;
    }
    String resp = JOptionPane.showInputDialog(this, "Veuillez saisir le nom du fichier à créer (en terminant par .xml).\nIl apparaîtra ensuite dans la liste des fichiers d'analyse.", curDir.getName());
    if (resp != null && !resp.isEmpty()) {
        // Vérifier si le fichier existe :
        File fil = new File(this.workingDir.getPath() + File.separator + resp);
        curDir = null;
        if (fil.exists()) {
            int respo = JOptionPane.showConfirmDialog(this, "Le fichier XML cible de l'analyse existe déjà.\nVoulez-vous l'écraser ?\nCliquez sur non pour annuler l'analyse.", "Fichier d'analyse existant", JOptionPane.YES_NO_OPTION);
            if (respo == JOptionPane.NO_OPTION) {
                return;
            }
        }
        // Créer le fichier, avec une liste de notes vide.
        MusicScore score = new MusicScore(fil);
        score.setNotes(new ArrayList<Note>());
        try {
            score.notesToXML(120, "not created from recording");
            inform("Fichier d'analyse vierge " + fil.getName() + " créé.");
        } catch (FileNotFoundException ex) {
            JOptionPane.showMessageDialog(this, "Fichier inutilisable... Veuillez réessayer avec un autre nom de fichier.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erreur d'entrée sortie à l'écriture du fichier.\nVeuillez réessayer, éventuellement avec un autre nom de fichier.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Normalement à partir d'ici le fichier est créé...
        EditScoreDialog edit = new EditScoreDialog(this, true, score);
        edit.setLocationRelativeTo(null);
        edit.setVisible(true);
        this.fillAnalList();
    }
}//GEN-LAST:event_jMenuItemNewAnalyseActionPerformed

private void jMenuItemChangeWorkingDirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemChangeWorkingDirActionPerformed
    JFileChooser fc = new JFileChooser();
    fc.setCurrentDirectory(workingDir);
    fc.setMultiSelectionEnabled(false);
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fc.setAcceptAllFileFilterUsed(false);
    int ret = fc.showOpenDialog(this);
    if (ret == JFileChooser.APPROVE_OPTION) {
        if (fc.getSelectedFile().isDirectory()) {
            this.workingDir = fc.getSelectedFile();
            props.setWorkingDir(workingDir);
            this.fillAnalList();
            this.fillFileList();
            inform("Changement de répertoire de travail vers : " + this.workingDir.getPath());
        }
    }
}//GEN-LAST:event_jMenuItemChangeWorkingDirActionPerformed

private void jMenuItemConfigIAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemConfigIAActionPerformed
    ConfigureAIDialog conf = new ConfigureAIDialog(this, true);
    conf.setLocationRelativeTo(null);
    conf.setVisible(true);
}//GEN-LAST:event_jMenuItemConfigIAActionPerformed

private void jCheckBoxMenuItemIAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemIAActionPerformed
    if (this.props.getPerceptronList()[0] == null) {
        JOptionPane.showMessageDialog(this, "Ce mode ne fonctionne pas sans configuration préalable.\nConfigurez le contenu harmonique depuis le menu Analyse ->\nConfigurer IA harmonique...", "Attention", JOptionPane.WARNING_MESSAGE);
        this.jCheckBoxMenuItemIA.setSelected(false);
        this.props.setUseAI(false);
    } else {
        if (this.jCheckBoxMenuItemIA.isSelected()) {
            this.props.setUseAI(true);
            inform("Mode IA de reconnaissance actif.\nVeuillez noter que les corrections particulières du menu analyse n'ont aucun effet en mode IA.");
        } else {
            this.props.setUseAI(false);
            inform("Mode IA de reconnaissance désactivé.");
        }
    }
}//GEN-LAST:event_jCheckBoxMenuItemIAActionPerformed

private void jButtonComposeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonComposeActionPerformed
    if (!this.jListAnalysis.isSelectionEmpty()) {
        Object[] selFiles = this.jListAnalysis.getSelectedValues();
        if (selFiles == null) {
            return;
        }
        if (selFiles.length < 2) {
            JOptionPane.showMessageDialog(this, "Il est recommandé de sélectionner plus d'un fichier d'anlyse avant de cliquer sur composer.\nVous pouvez néanmoins continuer mais... Le résultat risque d'être particulier.", "Attention", JOptionPane.WARNING_MESSAGE);
        }
        MusicScore[] initPop = new MusicScore[selFiles.length];
        for (int x = 0; x < selFiles.length; x++) {
            File fil = new File(this.workingDir.getPath() + File.separator + (String) selFiles[x]);
            MusicScore score = new MusicScore(fil);
            score.XMLToNotes();
            initPop[x] = score;
        }
        ComposeDialog c = new ComposeDialog(this, true, initPop);
        c.setLocationRelativeTo(null);
        c.setVisible(true);
    }
}//GEN-LAST:event_jButtonComposeActionPerformed

private void jCheckBoxMenuItemProlongActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxMenuItemProlongActionPerformed
    props.setEchProlongation(this.jCheckBoxMenuItemProlong.isSelected());
}//GEN-LAST:event_jCheckBoxMenuItemProlongActionPerformed

private void jRadioButtonMenuItemNoCorrecActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMenuItemNoCorrecActionPerformed
    props.setActiveCorrectionCode(soundAnalyser.NO_CORREC);
}//GEN-LAST:event_jRadioButtonMenuItemNoCorrecActionPerformed

private void jRadioButtonMenuItemConcFluteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMenuItemConcFluteActionPerformed
    props.setActiveCorrectionCode(soundAnalyser.CONC_FLUTE_CORREC);
}//GEN-LAST:event_jRadioButtonMenuItemConcFluteActionPerformed

private void jRadioButtonMenuItemCrudeCorrecActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMenuItemCrudeCorrecActionPerformed
    props.setActiveCorrectionCode(soundAnalyser.CRUDE_CORREC);
}//GEN-LAST:event_jRadioButtonMenuItemCrudeCorrecActionPerformed

private void jRadioButtonMenuItemTrumpCorrecActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonMenuItemTrumpCorrecActionPerformed
    props.setActiveCorrectionCode(soundAnalyser.TRUMP_CORREC);
}//GEN-LAST:event_jRadioButtonMenuItemTrumpCorrecActionPerformed

private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
    // Sauver les propriétés...
    try {
        this.props.writeToDisk(propFil);
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Les propriétés de l'application n'ont pas pu être sauvées.\nVeuillez vérifier les droits en écriture sur le répertoire de l'application.", "Erreur IO", JOptionPane.ERROR_MESSAGE);
    }
    System.exit(0);
}//GEN-LAST:event_formWindowClosing

private void jMenuItemTroubleshootActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemTroubleshootActionPerformed
    this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    HelpDialog diag = new HelpDialog(this, false, HelpDialog.TROUBLESHOOT);
    this.setCursor(Cursor.getDefaultCursor());
    diag.setLocationRelativeTo(null);
    diag.setVisible(true);
}//GEN-LAST:event_jMenuItemTroubleshootActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroupCorrec;
    private javax.swing.JButton jButtonAnalyse;
    private javax.swing.JButton jButtonAnalyseAndSave;
    private javax.swing.JButton jButtonCompose;
    private javax.swing.JButton jButtonDelete;
    private javax.swing.JButton jButtonDeleteNotes;
    private javax.swing.JButton jButtonEdit;
    private javax.swing.JButton jButtonPlay;
    private javax.swing.JButton jButtonPlayNotes;
    private javax.swing.JButton jButtonRecord;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemIA;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItemProlong;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabelCounter;
    private javax.swing.JLabel jLabelMetronome;
    private javax.swing.JLabel jLabelState;
    private javax.swing.JList jListAnalysis;
    private javax.swing.JList jListFiles;
    private javax.swing.JMenu jMenuAnalyse;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenu jMenuHelp;
    private javax.swing.JMenuItem jMenuItemChangeWorkingDir;
    private javax.swing.JMenuItem jMenuItemClearText;
    private javax.swing.JMenuItem jMenuItemConfig;
    private javax.swing.JMenuItem jMenuItemConfigIA;
    private javax.swing.JMenuItem jMenuItemHelp;
    private javax.swing.JMenuItem jMenuItemInstrChange;
    private javax.swing.JMenuItem jMenuItemNewAnalyse;
    private javax.swing.JMenuItem jMenuItemQuit;
    private javax.swing.JMenuItem jMenuItemRefresh;
    private javax.swing.JMenuItem jMenuItemSeuil;
    private javax.swing.JMenuItem jMenuItemTessiture;
    private javax.swing.JMenuItem jMenuItemTroubleshoot;
    private javax.swing.JMenuItem jMenuItemTuner;
    private javax.swing.JMenu jMenuPartCorrections;
    private javax.swing.JMenu jMenuTools;
    private javax.swing.JMenu jMenuView;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanelAnal;
    private javax.swing.JPanel jPanelNotif;
    private javax.swing.JPanel jPanelRecord;
    private javax.swing.JPanel jPanelRecs;
    private javax.swing.JPanel jPanelStatus;
    private javax.swing.JProgressBar jProgressBarShit;
    private javax.swing.JRadioButtonMenuItem jRadioButtonMenuItemConcFlute;
    private javax.swing.JRadioButtonMenuItem jRadioButtonMenuItemCrudeCorrec;
    private javax.swing.JRadioButtonMenuItem jRadioButtonMenuItemNoCorrec;
    private javax.swing.JRadioButtonMenuItem jRadioButtonMenuItemTrumpCorrec;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JTextArea jTextAreaNotif;
    private javax.swing.JTextField jTextFieldTempo;
    private javax.swing.JToggleButton jToggleButtonMetronome;
    // End of variables declaration//GEN-END:variables

    public javax.swing.JLabel getJLabelCounter() {
        return jLabelCounter;
    }

    public void setJLabelCounter(javax.swing.JLabel jLabelCounter) {
        this.jLabelCounter = jLabelCounter;
    }

    public Mixer getPlayer() {
        return player;
    }

    public void setPlayer(Mixer player) {
        this.player = player;
    }

    public Mixer getRecorder() {
        return recorder;
    }

    public void setRecorder(Mixer recorder) {
        this.recorder = recorder;
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

    public boolean isTunerActive() {
        return tunerActive;
    }

    public void setTunerActive(boolean tunerActive) {
        this.tunerActive = tunerActive;
    }

    public int getInstrument() {
        return instrument;
    }

    public void setInstrument(int instrument) {
        this.instrument = instrument;
    }

    public File getWorkingDir() {
        return this.workingDir;
    }

    public void setWorkingDir(File fil) {
        this.workingDir = fil;
    }

    public AppProperties getProps() {
        return props;
    }

    public void setProps(AppProperties props) {
        this.props = props;
    }

    /**
     * @return the jProgressBarShit
     */
    public javax.swing.JProgressBar getjProgressBarShit() {
        return jProgressBarShit;
    }
}
