package filefilters;

import java.io.File;
import java.io.FileFilter;

/**
 * in a copy dir operation, we can exclude files to copy to dest with this class
 * @author fruizdearcaute
 */
public class ExcludeDirFilter implements FileFilter {
    protected final String dirName;
    
    public ExcludeDirFilter(String dirName) {
    	this.dirName = dirName;
	}

	@Override
	public boolean accept(File file) {
		if(file.isDirectory() && file.getName().equalsIgnoreCase(this.dirName)){
			return false;
		}
		return true;
	}
}
