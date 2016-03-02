package org.icometrix.stdsplugins.updater;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import net.lingala.zip4j.core.ZipFile;

/**
 * This agent is responsible for starting/stopping the updater. The agent waits
 * for a new updater to be published, and starts it.
 * The flow is strongly inspired on 
 * http://stackoverflow.com/questions/4002462/how-can-i-write-a-java-application-that-can-update-itself-at-runtime
 * @author fruizdearcaute
 *
 */
public class CtpUpdaterAgent{
	final static Logger logger = Logger.getLogger(CtpUpdaterAgent.class);
	
	String VERSIONFILE = "version.info";
	String SCRIPTSDIR = "ctp_updater_scripts";
	String UNIXSTARTSCRIPT = SCRIPTSDIR + "/start_updater.sh";
	String WINDOWSSTARTSCRIPT = SCRIPTSDIR + "\\start_updater.bat";
	File TARGETDOWNLOADDIR = new File("target-download-updater");
	String UPDATERLOCKFILE = "updater.lock";

	File rootCtp;
	
	ReleasesDao releasesDao = null;
	int portCtp;
	Boolean ssl;
	

	public CtpUpdaterAgent(File rootCtp, int portCtp, Boolean ssl, ReleasesDao dao) {
		this.rootCtp = rootCtp;
		this.releasesDao = dao;
		this.ssl = ssl;
		this.portCtp = portCtp;
	}

	public void runRobustUpdateProcedure() {
		// this must be super robust, keeps trying, even if encounters errors
		while (true) {
			try {
				runUpdateProcedure();
				return;
			} catch (Exception e) {
				logger.error("Caucht error whilst running updateProcedure", e);
			}
		}
	}
	
	public void runUpdateProcedure() throws Exception{
		logger.info("starting update procedure");
		
		Path updaterRoot = pollForNewUpdate(this.getCurrentTag());
		String lockFileCtpUpdater = Paths.get(updaterRoot.toAbsolutePath().toString(), UPDATERLOCKFILE)
				.toAbsolutePath().toString();
		String[] command = buidCtpUpdaterStartCommand(updaterRoot.toAbsolutePath().toString(),
				this.rootCtp.getAbsolutePath().toString(), 
				Integer.toString(this.portCtp), 
				Boolean.toString(this.ssl),
				lockFileCtpUpdater);
		startUpdater(command, lockFileCtpUpdater, updaterRoot);
	}

	public Path pollForNewUpdate(String currentTag) throws Exception {		
		boolean hasUpdate = false;
		
		TARGETDOWNLOADDIR.mkdir();
		while (!hasUpdate) {
			Thread.sleep(4000);
			
			String latest_update_tag = this.releasesDao.getReleaseTagLatest();
			
			if(!latest_update_tag.trim().equalsIgnoreCase(currentTag.trim())){
				logger.info("Found new update");
				hasUpdate = true;
			}
			
			logger.debug("Current tag " + currentTag + ", found tag " + latest_update_tag);
		}

		String zipFilePath = this.releasesDao.getReleaseFileLatest(this.TARGETDOWNLOADDIR).getAbsolutePath();
		ZipFile zipFile = new ZipFile(zipFilePath);
		Path targetDir = Files.createTempDirectory(null);
		logger.info(targetDir.toAbsolutePath().toString());
		zipFile.extractAll(targetDir.toAbsolutePath().toString());

		// clean up
		new File(zipFilePath).delete();

		return targetDir;
	}

	public void startUpdater(String[] command, String lockFilePath, Path updaterRoot) throws Exception {
		try {
			
			logger.info("starting Updater");
			Thread updater = new Thread(new CtpUpdaterStarter(command));
			updater.start();
			waitForUpdaterToStart(lockFilePath);
			
		} catch (Exception e) {
			logger.error("Got errors starting updater", e);
			logger.info("cleaning up....");
			FileUtils.deleteDirectory(updaterRoot.toFile());
			throw e;
		}
	}

	public void waitForUpdaterToStart(String lockFilePath) throws Exception {
		Boolean locked = false;
		int maxRetrys = 10;
		int retryCounter = 0;
		int sleepCycle = 5000;

		while (retryCounter <= maxRetrys) {

			locked = this.isUpdaterRunning(lockFilePath);
			if (locked) {
				break;
			}
			logger.info("updater is still busy starting, sleeping " + sleepCycle);
			Thread.sleep(sleepCycle);
			retryCounter++;
		}

		if (!locked) {
			throw new Exception("After " + retryCounter + " retries, gave up waiting for Updater to start");
		}
	}

	public boolean isUpdaterRunning(String lockFile) throws Exception {
		// implementation based on
		// http://stackoverflow.com/questions/128038/how-can-i-lock-a-file-using-java-if-possible
		FileOutputStream lockFileStream = new FileOutputStream(new File(lockFile));
		try {
			FileLock lock = lockFileStream.getChannel().tryLock();
			if (lock != null) {
				logger.info("is not running!!");
				lock.release();
				return false;
			}
			logger.info("is running!!");
			return true;
		} finally {
			lockFileStream.close();
		}
	}

	private String[] buidCtpUpdaterStartCommand(String rootUpdater, String rootCtp, 
			                                    String port, String ssl, String lockFileCtpUpdater) {
		String osName = System.getProperty("os.name").toLowerCase();
		if(osName.contains("windows")){
			return new String[] {this.WINDOWSSTARTSCRIPT, rootUpdater, rootCtp, port, ssl, lockFileCtpUpdater };
		}
		return new String[] { "/bin/bash", this.UNIXSTARTSCRIPT, rootUpdater, rootCtp, port, ssl, lockFileCtpUpdater };
	}
	
	private String getCurrentTag() throws Exception{
		logger.info("Reading version file...");
		Properties prop = new Properties();
		InputStream input = new FileInputStream(VERSIONFILE);
		try{
			prop.load(input);
			return prop.getProperty("tag");
		}
		finally{
			input.close();
		}
	}

	/****
	 * Internal class responsible for starting a separate thread for CTP
	 * 
	 * @author fruizdearcaute
	 */
	private class CtpUpdaterStarter implements Runnable {
		String[] command;

		public CtpUpdaterStarter(String[] command) {
			this.command = command;
		}

		public void run() {
			try {
				Utils.runProcess(this.command);

			} catch (Exception e) {
				logger.error("Errors starting CTP controller", e);
			}

		}
	}
}