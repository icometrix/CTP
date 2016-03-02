package filefilters;

import java.io.File;
import java.io.FileFilter;

/**
 * in a copy dir operation, we can exclude files to copy to dest with this class
 * @author fruizdearcaute
 */
public class ExcludeFileFilter implements FileFilter {
    protected final String fileName;
    
    public ExcludeFileFilter(String fileName) {
    	this.fileName = fileName;
	}

	@Override
	public boolean accept(File file) {
		if(file.isFile() && file.getName().equalsIgnoreCase(this.fileName)){
			return false;
		}
		return true;
	}
}
