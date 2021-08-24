package AI;

import java.util.*;
import artificialcomposer.*;

/**
 *
 * @author William
 */
public class Ecosystem {

    private ArrayList<MusicGenome> pop = new ArrayList<MusicGenome>();
    private ArrayList<MusicGenome> anteriorPop = null;
    private int generation = 0;

    public Ecosystem(ArrayList<MusicGenome> pop) {
        this.pop = pop;
    }

    public Ecosystem() {
        // Rien.
    }
    
    private boolean trueBoolTab(boolean[] boolTab) {
        for (int x = 0; x < boolTab.length; x++) {
            if (boolTab[x] == false) {
                return false;
            }
        }
        return true;
    }

    private MusicGenome breed(MusicGenome[] subPop) {
        // But : Produire une génome à partir d'un sous ensemble.
        // Sortie de reproduction à plusieurs...
        if (subPop.length < 2) {
            return null;
        }
        ArrayList<Note> notes = new ArrayList<Note>();
        int[] positions = new int[subPop.length];
        for (int x = 0; x < positions.length; x++) {
            positions[x] = 0;
        }
        // Calculer la longueur du futur enfant :
        int max = 0;
        int totalTempo = 0;
        double[] probs = {0.0, 0.0, 0.0};
        for (int x = 0; x < subPop.length; x++) {
            int sz = subPop[x].getNotes().size();
            for (int i = 0; i < subPop[x].getReprodTypeP().length; i++) {
                probs[i] += subPop[x].getReprodTypeP()[i];
            }
            totalTempo += subPop[x].getTempo();
            if (sz > max) {
                max = sz;
            }
        }
        for (int x = 0; x < probs.length; x++) {
            probs[x] /= subPop.length;
        }
        int tempo = totalTempo / subPop.length;
        Random rand = new Random();
        // On va allonger ou réduire l'affaire de manière aléatoire...
        // Première sorte d'espèce de mutation...
        int limit = max / 5;
        if (limit == 0) {
            limit = 1;
        }
        int mod = rand.nextInt(limit);
        // Permet d'avoir du négatif :
        // Avec plus de chances de pas avoir de négatif...
        mod -= limit / 3;
        int genSize = max + mod;
        MusicGenome child = new MusicGenome(notes, tempo);
        child.setReprodTypeP(probs);
        // Maintenant faut recomposer l'enroule...
        boolean[] partTaken = new boolean[subPop.length];
        for (int x = 0; x < partTaken.length; x++) {
            partTaken[x] = false;
        }
        int x = 0;
        while (x < genSize) {
            // Faudrais chopper des morceaux égaux de génome chez chaque extrait...
            // Ou euh... Un nombre égal de patchs tirés quelque soit la technique
            // (Dawa, Measure, etc).
            // Choix de l'extrait dont on va chopper un bout :
            if (!this.trueBoolTab(partTaken)) {
                int extract = -1;
                for (int i = 0; i < subPop.length * 2; i++) {
                    int cand = rand.nextInt(subPop.length);
                    if (partTaken[cand] == false) {
                        // On peut sélectionner cet extrait...
                        extract = cand;
                        break;
                    }
                }
                if (extract == -1) {
                    for (int i = 0; i < subPop.length; i++) {
                        // L'aléatoire a rien trouvé, on alloue à l'arrache :
                        if (partTaken[i] == false) {
                            // Voilà, trouvé.
                            extract = i;
                        }
                    }
                }
                // Il est un peu moche ce code...
                partTaken[extract] = true;
                // Choppons un extrait...
                // Ca doit dépendre probabilistiquement des probas résultat.
                // On commence par choisir un des trois caractères :
                boolean found = false;
                while (!found) {
                    int car = rand.nextInt(child.getReprodTypeP().length);
                    double dice = rand.nextInt(1000);
                    // Favoriser les hautes valeurs sur dice.
                    if (dice < 0.9) {
                        double modP = rand.nextDouble();
                        if (modP > 0.3) {
                            // Augmentation de dice.
                            // Risque pas de dépasser 1.
                            dice += dice / 2;
                        }
                    }
                    if (subPop[extract].getReprodTypeP()[car] > dice) {
                        found = true;
                        ArrayList<Note> patch = new ArrayList<Note>();
                        // On choisit cette caractéristique là.
                        int cur = 0;
                        int pos = 0;
                        int leng = 0;
                        int tot = 0;
                        boolean ordered = false;
                        switch (car) {
                            case MusicGenome.MEASURE:
                                // Copie par combines de longueur 2 * 16 min,
                                // 4 * 16 max.
                                leng = rand.nextInt(4) + 1;
                                leng *= 16;
                                // Chopper un bout au hazard ?
                                // Si on arrive à la fin de l'extrait on complète
                                // en prenant au début.
                                // Proba. haute de prendre les patches dans l'ordre...
                                // Proba. faible que la position soit aléatoire.
                                if (rand.nextDouble() >= 0.3) {
                                    // 70% de chances de prendre dans l'ordre.
                                    pos = positions[extract];
                                    ordered = true;
                                } else {
                                    pos = rand.nextInt(subPop[extract].getNotes().size());
                                    ordered = false;
                                }
                                tot = 0;
                                cur = 0;
                                while (tot < leng) {
                                    Note toAdd = subPop[extract].getNotes().get(pos + cur);
                                    patch.add(toAdd);
                                    tot += toAdd.getNoteValue();
                                    cur++;
                                    if (pos + cur >= subPop[extract].getNotes().size()) {
                                        pos = 0;
                                        cur = 0;
                                        if (ordered) {
                                            positions[extract] = 0;
                                            continue;
                                        }
                                    }
                                    if (ordered) {
                                        positions[extract]++;
                                    }
                                }
                                break;
                            case MusicGenome.MOREM:
                                leng = rand.nextInt(4) + 1;
                                leng *= 16;
                                int more = rand.nextInt(5) + 1;
                                leng *= more;
                                if (rand.nextDouble() >= 0.3) {
                                    // 70% de chances de prendre dans l'ordre.
                                    pos = positions[extract];
                                    ordered = true;
                                } else {
                                    pos = rand.nextInt(subPop[extract].getNotes().size());
                                    ordered = false;
                                }
                                tot = 0;
                                cur = 0;
                                while (tot < leng) {
                                    Note toAdd = subPop[extract].getNotes().get(pos + cur);
                                    patch.add(toAdd);
                                    tot += toAdd.getNoteValue();
                                    cur++;
                                    if (pos + cur >= subPop[extract].getNotes().size()) {
                                        pos = 0;
                                        cur = 0;
                                        if (ordered) {
                                            positions[extract] = 0;
                                            continue;
                                        }
                                    }
                                    if (ordered) {
                                        positions[extract]++;
                                    }
                                }
                                break;
                            case MusicGenome.DAWA:
                                // Juste chopper quelques notes tout à fait au
                                // hazard dans l'affaire et les ajouter...
                                int nbr = rand.nextInt(6);
                                // Le DAWA est jamais pris dans l'ordre.
                                for (int i = 0; i <= nbr; i++) {
                                    pos = rand.nextInt(subPop[extract].getNotes().size());
                                    patch.add(subPop[extract].getNotes().get(pos));
                                }
                                break;
                        }
                        // Ajouter le patch à l'extrait enfant.
                        for (int i = 0; i < patch.size(); i++) {
                            child.getNotes().add(patch.get(i));
                            x++;
                        }
                    }
                }
            } else {
                // On a pris des patches sur chaque extrait.
                for (int y = 0; y < partTaken.length; y++) {
                    partTaken[y] = false;
                }
            }
        }
        return child;
    }

    public void advanceOneGeneration() {
        if (getPop().size() < 2) {
            return;
        }
        anteriorPop = new ArrayList<MusicGenome>(pop);
        generation++;
        // Bon euh... Faut choisir qui se reproduit en fonction du fitness...
        // En fait les hauts fitness devraient avoir plus de chance de participer
        // a plein de reproductions...
        // Sélection par tournament.
        // Y a aussi sélection type bouc : le meilleur se reproduit avec tous les
        // autres...
        Random rand = new Random();
        // On va créer une population de x * (x - 1) nouveaux individus.
        // Ou bien, x * (x - 1) / 2... Commençons par ça.
        int expectedPop = (pop.size() * pop.size() - pop.size()) / 2;
        int count = 0;
        // 1/5 de ça c'est des multicombines de tout sauf les fitness vachement bas...
        int nbrMultiCombine = expectedPop / 5;
        ArrayList<MusicGenome> newPop = new ArrayList<MusicGenome>();
        for (int x = 0; x < nbrMultiCombine; x++) {
            ArrayList<MusicGenome> combine = new ArrayList<MusicGenome>();
            double dice = rand.nextDouble();
            // Donnons un peu plus de chances à tout le monde...
            dice -= dice / 2;
            for (int i = 0; i < pop.size(); i++) {
                if (pop.get(i).getFitness() - dice >= 0) {
                    combine.add(pop.get(i));
                }
            }
            if (combine.size() >= 2) {
                MusicGenome[] supo = new MusicGenome[combine.size()];
                for (int y = 0; y < supo.length; y++) {
                    supo[y] = combine.get(y);
                }
                MusicGenome offspring = this.breed(supo);
                offspring.setName("Enfant " + count + " gen " + this.getGeneration());
                newPop.add(offspring);
                count++;
            }
        }
        // Pour le reste (4/5 expectedPop) on prend des couples.
        while (count <= expectedPop) {
            // Breeder plein de trucs deux par deux.
            MusicGenome[] couple = new MusicGenome[2];
            int found = 0;
            double dice = rand.nextDouble();
            int loops = 0;
            while (found < 2) {
                int extract = rand.nextInt(pop.size());
                if (pop.get(extract).getFitness() - dice >= 0) {
                    couple[found] = pop.get(extract);
                    found++;
                }
                // Protection anti boucles infinies.
                loops++;
                if (loops >= pop.size() * 3) {
                    dice = 0;
                } else if (loops >= pop.size() * 2) {
                    dice = rand.nextDouble();
                }
            }
            count++;
            MusicGenome offspring = this.breed(couple);
            offspring.setName("Enfant " + count + " gen " + this.getGeneration());
            newPop.add(offspring);
        }
        this.pop = newPop;
    }

    public void stepBack() {
        if (generation > 0) {
            generation--;
            pop = new ArrayList<MusicGenome>(anteriorPop);
        }
    }

    public ArrayList<MusicGenome> getPop() {
        return pop;
    }

    public void setPop(ArrayList<MusicGenome> pop) {
        this.pop = pop;
    }

    public int getGeneration() {
        return generation;
    }
    
}
