
package artificialcomposer;

/**
 *
 * @author William
 */
public class Spectrum {
    
    // Classe pourrie correspondant à un spectre discret de modules.

    private final int[] indices;
    private final double[] modules;
    private final int dim;
    
    public Spectrum(int[] indices, double[] modules) {
        this.indices = indices;
        this.modules = modules;
        this.dim = indices.length;
    }

    public int[] getIndices() {
        return indices;
    }

    public double[] getModules() {
        return modules;
    }

    public int getDim() {
        return dim;
    }
    
}
