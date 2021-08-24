/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package UI;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author William
 */
public class ExtensionFilter extends FileFilter {

    private String extension;
    private String descr;

    public ExtensionFilter(String ext, String descr) {
        this.extension = ext;
        this.descr = descr;
    }

    @Override
    public boolean accept(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        if (ext != null) {
            if (ext.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return descr;
    }
}
