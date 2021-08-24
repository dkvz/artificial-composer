
package UI;

import java.io.*;

/**
 *
 * @author William
 */
public class xmlFilter implements FilenameFilter {

    public boolean accept(File dir, String name) {
        if (name.endsWith(".xml")) {
            // Ouvrir et matter la seconde ligne ?
            FileInputStream fstream;
            try {
                fstream = new FileInputStream(name);
                DataInputStream in = new DataInputStream(fstream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = br.readLine()) != null)   {
                    if (line.contains("<music-score>")) {
                        return true;
                    }
                }
                in.close();
            } catch (Exception ex) {
                // Prout.
            }
        } return false;
    }

}
