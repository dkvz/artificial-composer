package AI;

import java.io.Serializable;

/**
 *
 * @author William
 */
public class FreqPerceptron implements Serializable {

    private double learningRate = 0.0000001d;
    private double[] weights;
    private double[] entries;
    private boolean hasLearned = false;
    private double resultTresh = 0.1;
    private int maxLoop = 10000;

    public FreqPerceptron(int dim, double[] initWeights) {
        if (dim <= 0)
            dim = 1;
        entries = new double[dim];
        if (initWeights.length == dim) {
            weights = initWeights;
        } else {
            weights = new double[dim];
            for (int x = 0; x < dim; x++) {
                weights[x] = 0;
            }
        }
    }

    public double processEntries() {
        double out = 0.0;
        for (int x = 0; x < entries.length; x++) {
            out += entries[x] * weights[x];
        }
        return out;
    }

    public void learnUntilConvergence(double expectedResult) throws ArithmeticException {
        // Mettre une protection sur le nombre d'itération.
        // Lancement d'exception.
        hasLearned = true;
        double result = this.processEntries();
        int count = 0;
        while (result < expectedResult - this.resultTresh || result > expectedResult + this.resultTresh) {
            for (int x = 0; x < weights.length; x++) {
                weights[x] = weights[x] + learningRate * (expectedResult - result) * entries[x];
            }
            result = this.processEntries();
            count++;
            if (count >= maxLoop) {
                throw new ArithmeticException("Boucle infinie.");
            }
        }
    }

    public void learnOneStep(double expectedResult) {
        double result = this.processEntries();
        hasLearned = true;
        if (result > expectedResult - this.resultTresh && result < expectedResult + this.resultTresh) {
            // C'est déjà bon...
            return;
        }
        for (int x = 0; x < weights.length; x++) {
            weights[x] = weights[x] + learningRate * (expectedResult - result) * entries[x];
        }
    }

    public double getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    public double[] getWeights() {
        return weights;
    }

    public void setWeights(double[] weights) {
        this.weights = weights;
    }

    public double[] getEntries() {
        return entries;
    }

    public void setEntries(double[] entries) {
        this.entries = entries;
    }

    public boolean isHasLearned() {
        return hasLearned;
    }

    public void setHasLearned(boolean hasLearned) {
        this.hasLearned = hasLearned;
    }

    public double getResultTresh() {
        return resultTresh;
    }

    public void setResultTresh(double resultTresh) {
        this.resultTresh = resultTresh;
    }

    public int getMaxLoop() {
        return maxLoop;
    }

    public void setMaxLoop(int maxLoop) {
        this.maxLoop = maxLoop;
    }

}
