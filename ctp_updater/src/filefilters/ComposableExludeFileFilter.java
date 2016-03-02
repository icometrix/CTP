package filefilters;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * if you want to add multiple filters
 * @author fruizdearcaute
 */
public class ComposableExludeFileFilter implements FileFilter {

    protected List<FileFilter> filters;
    
    public ComposableExludeFileFilter() {
    	this.filters = new ArrayList<FileFilter>();
	}
    
    public void addFilter(FileFilter filter){
    	this.filters.add(filter);
    }

	@Override
	public boolean accept(File file) {
		
		for(FileFilter filter: this.filters){
			if(!filter.accept(file)){
				return false;
			}
		}
		return true;
	}
}
