package main;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import filefilters.ComposableExludeFileFilter;
import filefilters.ExcludeDirFilter;
import filefilters.ExcludeFileFilter;
/***
 * This agent is responsible updating/replacing all files from both the controller and the CTP
 * TODO: some failed update exception handler writing...
 * @author fruizdearcaute
 *
 */
public class CtpUpdateAgent {
	final static Logger logger = Logger.getLogger(CtpUpdateAgent.class);
	
	final String  UPDATECTPDIR = "ctp_files";
	
	//config files which should not be overwritten
	final String CTPCONFIGFILE = "config.xml";
	final String WINDIR = "windows";
	final String LINUXDIR = "linux";
	
	String rootCtp;
	
	public CtpUpdateAgent(String rootCtp){
		this.rootCtp = rootCtp;
	}
	
	public void udpateCtp() throws IOException{	
		ComposableExludeFileFilter filters = new ComposableExludeFileFilter();
		filters.addFilter(new ExcludeFileFilter(CTPCONFIGFILE));
		filters.addFilter(new ExcludeDirFilter(WINDIR));
		filters.addFilter(new ExcludeDirFilter(LINUXDIR));
		
		this.runUpdate(UPDATECTPDIR, rootCtp, filters, "CTP");
	}
	
	private boolean runUpdate(String source, String target, FileFilter avoidFiles, String componentName) throws IOException{
		
		logger.info("Updating " + componentName + "!!!");
		
		//first try making a backup
		File backupFolder = null;
		File sourceDir = null;
		try{
			
			sourceDir = new File(source);
			
			if(sourceDir.isDirectory() && sourceDir.list().length == 0){
				logger.info("Source " + source + "is empty, nothing to do");
				return true;
			}
			
			try{ backupFolder = createBackup(source);}
			catch(Exception e){
				logger.warn("Failed backup, no update will occur");
				return false;
			}
			
			File targetDir = new File(target);
			
			if(!freeOfCommonIOissues(targetDir)){ return false; }
			
			
			FileUtils.copyDirectory(sourceDir, targetDir, avoidFiles);
			
			logger.info("Updated CTP...");
			
			return true;
		}
		catch(Exception e){
			logger.warn("Failed updating, restoring backup");
			restoreBackup(backupFolder, sourceDir); //if this fails, call helpdesk
			return false;
		}
		finally{
			logger.info("Deleting backup folder at " + backupFolder);
			FileUtils.deleteQuietly(backupFolder); //not a disaster if this fails
		}
	}
	
	private File createBackup(String source) throws Exception{
		try{
			Path targetDir = Files.createTempDirectory(null);
			logger.info("Backing " + source + "into " + targetDir.toAbsolutePath().toString());
			FileUtils.copyDirectory(new File(source), targetDir.toFile());
			return targetDir.toFile();
		}
		catch(Exception e){
			logger.error("Error backing up " + source, e);
			throw e;
		}
	}
	
	private Boolean freeOfCommonIOissues(File dir){
		logger.info("Checking for potential IO issues...");
		
		if(!dir.canWrite()) {
			logger.error("Folder not writable, skipping update....");
			return false;
		}
		
		if((dir.getFreeSpace() /1024 /1024) < 50){
			logger.error("partition < "+ 50 + "mb. skipping update...");
			return false;
		}
		return true;
	}
	
	private void restoreBackup(File backupDir, File targerDir) throws IOException{
		logger.info("Restoring backup from " + backupDir + " to " + targerDir);
		FileUtils.cleanDirectory(targerDir);
		FileUtils.copyDirectory(backupDir, targerDir);
		logger.info("Finished restoring backup");
	}
}
