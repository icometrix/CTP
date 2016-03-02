package main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class CtpUpdater {
	
	static Logger logger;
	static String log4jFile = "log4j.properties";
	
	/**
	 * Class coordinating the different agents. Communicates with external processes
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String rootCtp = args[0];
		int ctpPort = Integer.parseInt(args[1]);
		boolean ctpSsl = Boolean.parseBoolean(args[2]);
		String lockFileCtpUpdater = args[3];
		
		configureLog4j(); //this breaks HARD
		try {
			
			logger.info("Received the following");
			logger.info(rootCtp);
			logger.info(ctpPort);
			logger.info(ctpSsl);
			logger.info(lockFileCtpUpdater);
			logger.info("moving on ...");
			
			setupFileLock(new File(lockFileCtpUpdater)); //fail hard if not working
			
			CtpAgent ctpAgent = new CtpAgent(rootCtp, ctpPort, ctpSsl);
			ctpAgent.waitForCtpTo("stop");
			CtpUpdateAgent updateAgent = new CtpUpdateAgent(rootCtp);
			updateAgent.udpateCtp();
			logger.info("udpated CTP, moving on to starting it up!");
			ctpAgent.startCtpRobust();
			
		} catch (Exception e) {
			logger.error("Fatal error: ", e);
		}
	}
	
	public static void setupFileLock(File lockFile) throws Exception {
		
		logger.info("Locking CTP updater");
		if (!lockFile.exists()) {
			// acceptable here because the only for one thread
			lockFile.createNewFile(); 
		}
		@SuppressWarnings("resource")
		FileOutputStream lockStream = new FileOutputStream(lockFile);
		
		final FileLock lock = lockStream.getChannel().lock();
		
		//will even handle on a SIGTERM, SIGINT but not a SIGKILL/Hard crash
		Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
            	try {
            		logger.info("Releasing lock...");
					lock.release();
				} catch (IOException e) {
					logger.error("Got error unlocking file...", e);
				}
            }
        });
	}
	
	public static void configureLog4j() throws IOException {
		File logProps = new File(log4jFile);
		String propsPath = logProps.getAbsolutePath();
		if (!logProps.exists()) {
			System.out.println("Logger configuration file: " + propsPath);
			System.out.println("Logger configuration file not found.");
			throw new IOException();
		}
		PropertyConfigurator.configure(propsPath);
		logger = Logger.getLogger(CtpUpdater.class);
	}
}
