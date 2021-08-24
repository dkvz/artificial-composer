package artificialcomposer;

import shared.array.ComplexArray;
import shared.array.RealArray;
import java.util.ArrayList;

/**
 *
 * @author William
 */
public class soundAnalyser {

    public static final int CONC_FLUTE_CORREC = 1;
    public static final int CRUDE_CORREC = 2;
    public static final int NO_CORREC = 0;
    public static final int TRUMP_CORREC = 3;
    public static boolean TFAFF = false;

    public static int findMaxAmplitude(byte[] audioBytes) {
        int[] audioData = new int[audioBytes.length / 4];
        int max = 0;
        for (int x = 0; x < (audioBytes.length / 2); x += 2) {
            // En little endian, le poid faible vient d'abord.
            byte octet1 = audioBytes[x];   // Poids faible.

            byte octet2;
            if ((x + 1) >= audioBytes.length) {
                // Ajout d'un byte vide.
                octet2 = (byte) 0x00;          // Poids fort.

            } else {
                octet2 = audioBytes[x + 1];   // Poids fort.

            }
            // Combination magique :
            audioData[x / 2] = ((octet1 & 0xFF) | (octet2 << 8));
            if (Math.abs(audioData[x / 2]) > max) {
                max = Math.abs(audioData[x / 2]);
            }
        }
        return max;
    }
    
    public static boolean compareMaxims(int[] maxims1, int[] maxims2, int tresh) {
//        System.out.println("Comparaison de " + maxims1[0] + " " + maxims1[1] + " " + maxims1[2] + " " + maxims1[3]);
//        System.out.println("à: " + maxims2[0] + " " + maxims2[1] + " " + maxims2[2] + " " + maxims2[3]);
        if (maxims1.length == maxims2.length) {
            for (int x = 0; x < maxims1.length; x++) {
                if (maxims1[x] <= maxims2[x] - tresh || maxims1[x] > maxims2[x] + tresh) {
                    return false;
                }
            }
            // On a tout passé en revue.
            return true;
        }
        return false;
    }

    public static double[] audioByteToData(byte[] audioBytes, int len) {
        double[] audioData = new double[len];
        for (int x = 0; x < audioBytes.length; x += 2) {
            // En little endian, le poid faible vient d'abord.
            byte octet1 = audioBytes[x];   // Poids faible.
            byte octet2;
            if ((x + 1) >= audioBytes.length) {
                // Ajout d'un byte vide.
                octet2 = (byte) 0x00;          // Poids fort.
            } else {
                octet2 = audioBytes[x + 1];   // Poids fort.

            }
            // Combination magique :
            audioData[x / 2] = (double) ((octet1 & 0xFF) | (octet2 << 8));
        //System.out.println(audioData[x / 2]);
        }
        return audioData;
    }

    public static Spectrum findReducedSpectrum(byte[] audioBytes, int freqEch, int maxAmpl, int nbrMax, boolean prolong) {
        int audioDataLen = audioBytes.length / 2; 
        int len;
        if (!prolong) {
            len = audioDataLen;
        } else {
            len = audioDataLen * 8;
        }
        double[] audioData = soundAnalyser.audioByteToData(audioBytes, len);
        if (prolong) {
            boolean croissant = false;
            int copyPos = -1;
            if (audioData[audioDataLen - 2] > audioDataLen - 1) {
                croissant = true;
            }
            for (int x = 0; x < audioDataLen; x++) {
                if (audioData[x] >= (audioData[audioDataLen - 1] - 200) && audioData[x] <= (audioData[audioDataLen - 1] + 200)) {
                    if (x != audioData.length - 1) {
                        if (croissant && (audioData[x + 1] > audioData[x])) {
                            // Position acceptée...
                            copyPos = x;
                            break;
                        } else if (!croissant && (audioData[x + 1] < audioData[x])) {
                            copyPos = x;
                            break;
                        }
                    }
                }
            }
            if (copyPos == -1) {
                System.out.println("Point de raccrochage non trouvé...");
                copyPos = 0;
            }
            int actPos = copyPos;
            for (int x = audioDataLen; x < audioData.length; x++) {
                audioData[x] = audioData[actPos];
                if (actPos == (audioDataLen - 1)) {
                    actPos = copyPos;
                } else {
                    actPos++;
                }
            }
        }
        // Chopper intelligemment les 4 plus grands max (dim des weights du perceptron
        // plutôt que 4).
        // En plus faut qu'ils soient ordonnés...
        int[] maxims = new int[nbrMax];
        for (int x = 0; x < maxims.length; x++) {
            maxims[x] = 0;
        }
        double[] values = new double[maxims.length];
        for (int x = 1; x < maxims.length; x++) {
            values[x] = 0;
        }
        // Vérifier si on est pas sous le seuil d'ampl.
        long moyenne = 0;
        int maxIter = audioData.length / 4;
        int counter;
        for (counter = 0; counter < maxIter; counter++) {
            moyenne += Math.abs(audioData[counter]);
        }
        moyenne /= counter;
        if (moyenne > maxAmpl) {
            // Extrait non négligeable. 
            // Effectuer la fft :
            int maxStep = (int) (((audioData.length) / (double) freqEch) * 2500);
            double[] mods = new double[maxStep];
            RealArray ar = new RealArray(audioData);
            ComplexArray result = ar.rfft();
            // Calculer les modules :
            int ultimateMax = 0;
            double megaMax = 0;
            for (int x = 1; x < maxStep; x++) {
                // Trouver d'abord le max ultime. Pour plus tard pouvoir nier
                // ce qui est directement autour.
                mods[x] = Math.sqrt((result.get(x, 0) * result.get(x, 0)) + (result.get(x, 1) * result.get(x, 1)));
                if (mods[x] > megaMax) {
                    ultimateMax = x;
                    megaMax = mods[x];
                }
            }
            // On recommence...
            maxims[0] = ultimateMax;
            values[0] = megaMax;
            for (int x = 1; x < maxStep; x++) {
                for (int y = 0; y < maxims.length; y++) {
                    if (mods[x] > values[y]) {
                        if (x < ultimateMax - 10 || x > ultimateMax + 10) {
                            // On prend pas ce qui est à côté du max ultime.
                            // Repoussage des anciens max :
                            for (int z = (maxims.length - 1); z >= (y + 1); z--) {
                                maxims[z] = maxims[z - 1];
                                values[z] = values[z - 1];
                            }
                            values[y] = mods[x];
                            maxims[y] = x;
                            break;
                        }
                    }
                }
            }
            // Si le premier max est immensément balaise par rapport aux autres,
            // Faut nulifier les autres...
            double tresh = values[0] / 9;
            for (int x = 1; x < maxims.length; x++) {
                if (values[x] <= tresh) {
                    maxims[x] = 0;
                    values[x] = 0;
                } else if (maxims[x] > maxims[0] + 100) {
                    // On est très loin du premier max trouvé.
                    // C'est une harmonique à deux balles.
                    for (int p = maxims.length - 1; p > x; p--) {
                        maxims[p - 1] = maxims[p];
                        values[p - 1] = values[p];
                        maxims[p] = 0;
                        values[p] = 0;
                    }
                    maxims[x] = 0;
                    values[x] = 0;

                }
            }
        }
        Spectrum spectre = new Spectrum(maxims, values);
        // On renvoie juste les indices.
        return spectre;
    }

    public static double findFundamental(byte[] audioBytes, int numBytesRead, int freqEch, int correctionCode, boolean prolong) {
        int audioDataLen = audioBytes.length / 2; // numBytesRead ??
        double[] audioData;
        if (prolong) {
            audioData = new double[audioDataLen * 8];
        } else {
            audioData = new double[audioDataLen];
        }
        boolean impair = false;
        for (int x = 0; x < audioBytes.length; x += 2) {
            // En little endian, le poid faible vient d'abord.
            byte octet1 = audioBytes[x];   // Poids faible.

            byte octet2;
            if ((x + 1) >= audioBytes.length) {
                // Ajout d'un byte vide.
                octet2 = (byte) 0x00;          // Poids fort.

                impair = true;
            } else {
                octet2 = audioBytes[x + 1];   // Poids fort.

            }
            // Combination magique :
            audioData[x / 2] = (double) ((octet1 & 0xFF) | (octet2 << 8));
        //System.out.println(audioData[x / 2]);
        }
        if (impair) {   // En fait ça sert à rien ce truc !
            numBytesRead++;
        } // On en a ajouté un faux.

        // Prolongation du truc :
        if (prolong) {
            boolean croissant = false;
            int copyPos = -1;
            if (audioData[audioDataLen - 2] > audioDataLen - 1) {
                croissant = true;
            }
            for (int x = 0; x < audioDataLen; x++) {
                if (audioData[x] >= (audioData[audioDataLen - 1] - 200) && audioData[x] <= (audioData[audioDataLen - 1] + 200)) {
                    if (x != audioData.length - 1) {
                        if (croissant && (audioData[x + 1] > audioData[x])) {
                            // Position acceptée...
                            copyPos = x;
                            break;
                        } else if (!croissant && (audioData[x + 1] < audioData[x])) {
                            copyPos = x;
                            break;
                        }
                    }
                }
            }
            if (copyPos == -1) {
                System.out.println("Point de raccrochage non trouvé...");
                copyPos = 0;
            }
            int actPos = copyPos;
            for (int x = audioDataLen; x < audioData.length; x++) {
                audioData[x] = audioData[actPos];
                if (actPos == (audioDataLen - 1)) {
                    actPos = copyPos;
                } else {
                    actPos++;
                }
            }
            numBytesRead = audioData.length * 2;
        }

        // Effectuer la fft :
        int maxStep = (int) (((audioData.length) / (double) freqEch) * 3500);
        //double[] mods = new double[(audioData.length / 2) + 1];
        double[] mods = new double[maxStep];
        RealArray ar = new RealArray(audioData);
        ComplexArray result = ar.rfft();
        
        // Calculer les modules :
        for (int x = 0; x < maxStep; x++) {
            mods[x] = Math.sqrt((result.get(x, 0) * result.get(x, 0)) + (result.get(x, 1) * result.get(x, 1)));
        }
        // Chercher le plus grand :
        double indice = 1;
        if (correctionCode == soundAnalyser.NO_CORREC) {    // Mode rapide.

            double biggest = 0;
            // On va jusqu'à 3500 Hz :
            // Faudrais ptet généraliser ce truc pour le for des autres codes
            // de correction...
            for (int x = 1; x < maxStep; x++) {
                if (mods[x] > biggest) {
                    biggest = mods[x];
                    indice = x;
                }
            }
        } else {    // Modes correctifs.

            ArrayList<double[]> maxims = new ArrayList();
            double[] shit = {0, 0};
            maxims.add(shit);
            maxims.add(shit);
            maxims.add(shit);
            maxims.add(shit);
            maxims.add(shit);
            maxims.add(shit);
            maxims.add(shit);
            // Niage de la composante continue.
            for (int x = 1; x < maxStep; x++) {
                for (int y = (maxims.size() - 1); y >= 0; y--) {
                    if (maxims.get(y)[1] <= mods[x]) {
                        double[] stock = new double[2];
                        stock[0] = x;
                        stock[1] = mods[x];
                        maxims.add(y + 1, stock);
                        if (maxims.size() > 7) {
                            maxims.remove(0);
                        }
                        break;
                    }
                }
            }
            indice = maxims.get(maxims.size() - 1)[0];
            if (correctionCode == soundAnalyser.TRUMP_CORREC) {
                // Normalement on a 4 harmoniques multiples de la fondamentale...
                // En fait c'est le premier plus grand max qui compte... Pas le véritable max ultime.
                // On pourrait vérifier si les autres plus grands max sont bien
                // des multiples de celui-là.
                for (int x = 0; x < (maxims.size() - 1); x++) {
                    if (maxims.get(x)[0] < indice) {
                        int modu = (int) (maxims.get(x)[0] % indice);
                        if (modu >= maxims.get(x)[0] - 2 && modu <= maxims.get(x)[0] + 2) {
                            indice = maxims.get(x)[0];
                        }
                    }
                }
            } else if (correctionCode == soundAnalyser.CONC_FLUTE_CORREC) {
                double sum = 0;
                for (int x = 0; x < maxims.size(); x++) {
                    sum += maxims.get(x)[1];
                }
                for (int x = 0; x < (maxims.size() - 1); x++) {
                    //int intSize = (int)(Math.abs(maxims.get(maxims.size() - 1)[0] - maxims.get(x)[0]));
                    double rapport = (maxims.get(x)[1] / sum);
                    if (maxims.get(maxims.size() - 1)[0] > maxims.get(x)[0]) {
                        // Le max considéré est avant l'ultime.
                        // Faut soustraire.
                        indice -= rapport;
                    } else {
                        indice += rapport;
                    }
                }
            } else if (correctionCode == soundAnalyser.CRUDE_CORREC) {
                int negs = 0;
                int posis = 0;
                for (int x = 0; x < (maxims.size() - 1); x++) {
                    if (maxims.get(x)[0] > maxims.get(maxims.size() - 1)[0]) {
                        posis += maxims.get(x)[0];
                    } else {
                        negs += maxims.get(x)[0];
                    }
                }
                if (negs >= posis) {
                    indice--;
                } else {
                    indice++;
                }
            }
        }
        if (soundAnalyser.TFAFF && (indice >= 76 && indice <= 84)) {
            soundAnalyser.TFAFF = false;
            System.out.println(indice);
            System.out.println("DEBUT***");
            for (int x = 0; x < 1200; x++) {
                System.out.println(mods[x] + ";");
            }
            System.out.println("FIN***");
        }
        // Calcul de la fondamentale :
        return (double) indice * freqEch * 2 / (numBytesRead);
    }
}
