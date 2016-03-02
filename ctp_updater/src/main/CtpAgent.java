package main;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;

/**
 * This class is responsible for managing CTP: starting, stopping
 * This is agent is quite stupid so does not know anything about why it should be started or stopper
 * @author fruizdearcaute
 *
 */
public class CtpAgent {
	final static Logger logger = Logger.getLogger(CtpAgent.class);
	
	String SCRIPTSDIR = "ctp_control_scripts";
	String UNIXSTARTSCRIPT = SCRIPTSDIR + "/start_ctp.sh";
	String UNIXSTOPSCRIPT = SCRIPTSDIR + "/stop_ctp.sh";
	String WINDOWSSTARTSCRIPT = SCRIPTSDIR + "\\start_ctp.bat";
	String WINDOWSSTOPSCRIPT = SCRIPTSDIR + "\\stop_ctp.bat";
	
	String ctpDir;
	int ctpPort;
	Boolean ssl = false;
	
	public CtpAgent(String ctpDir, int ctpPort, Boolean ssl){
		this.ctpDir = ctpDir;
		this.ctpPort = ctpPort;
		this.ssl = ssl;
	}

	public void startCtpRobust() throws Exception{
		logger.info("starting CTP...");
		Thread ctpStarter = new Thread(new CtpStarter());
		ctpStarter.start();
		this.waitForCtpTo("start");
	}
	
	public void waitForCtpTo(String startStop) throws Exception{
		Boolean currentStateIsRunning = startStop.equalsIgnoreCase("start")? false : true;
		Boolean targetStateIsRunning = startStop.equalsIgnoreCase("start")? true : false;
		
		int maxRetrys = 10;
		int retryCounter = 0;
		int sleepCycle = 10000;
		
		while(retryCounter <= maxRetrys){
			
			currentStateIsRunning = this.isCtpRunning();
			if(currentStateIsRunning == targetStateIsRunning){
				break;
			}
			logger.info("Ctp is still not in target state, sleeping " + sleepCycle);
			Thread.sleep(sleepCycle);
			retryCounter++;
		}
		
		if(currentStateIsRunning != targetStateIsRunning){
			throw new Exception("After " + retryCounter + " retries, gave up waiting for ctp to " + startStop); 
		 }
		
	}
	
	/*
	 * Checks whether CTP is running.
	 * Taken from the original Runner.java
	 */
	public Boolean isCtpRunning(){
		try {
			URL url = new URL("http" + (ssl?"s":"") + "://127.0.0.1:"+ this.ctpPort);
			HttpURLConnection conn = getConnection( url );
			conn.setRequestMethod("GET");
			conn.connect();
			StringBuffer text = new StringBuffer();
			InputStream is = conn.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			int size = 256; char[] buf = new char[size]; int len;
			while ((len=isr.read(buf,0,size)) != -1) text.append(buf,0,len);
			
			return true;
		}
		catch (Exception ex) {
			return false; 
			}
	}
	
	/*
	 * Abstraction from httpUrlConnection, also taken from Runner.java
	 */
	private HttpURLConnection getConnection(URL url) throws Exception {
		String protocol = url.getProtocol().toLowerCase();
		if (!protocol.startsWith("https") && !protocol.startsWith("http")) {
			throw new Exception("Unsupported protocol ("+protocol+")");
		}
		HttpURLConnection conn;
		if (protocol.startsWith("https")) {
			HttpsURLConnection httpsConn = (HttpsURLConnection)url.openConnection();
			httpsConn.setUseCaches(false);
			httpsConn.setDefaultUseCaches(false);
			conn = httpsConn;
		}
		else conn = (HttpURLConnection)url.openConnection();
		conn.setDoOutput(true);
		conn.setDoInput(true);
		return conn;
	}

	public void stopCtp() throws Exception{
		Utils.runProcess(buildCtpStopCommand());
	}

	private String[] buildCtpStartCommand(){
		String osName = System.getProperty("os.name").toLowerCase();
		if(osName.contains("windows")){
			return new String[]{WINDOWSSTARTSCRIPT, this.ctpDir};
		}
		return new String[]{"/bin/bash",  UNIXSTARTSCRIPT, this.ctpDir};
	}

	private String[] buildCtpStopCommand(){
		String osName = System.getProperty("os.name").toLowerCase();
		if(osName.contains("windows")){
			return new String[]{WINDOWSSTOPSCRIPT, this.ctpDir};
		}
		return new String[]{"/bin/bash", UNIXSTOPSCRIPT, this.ctpDir};
	}
	
	/****
	 * Internal class responsible for starting a separate thread for CTP
	 * @author fruizdearcaute
	 *
	 */
	private class CtpStarter implements Runnable {

		public void run(){
			try{
				logger.info("IN:" + System.getProperty("user.dir"));
				logger.info("Running command" + Arrays.toString(buildCtpStartCommand()));
				Utils.runProcess(buildCtpStartCommand());

			} catch (Exception e) {
				logger.error("Errors starting CTP", e);
			}		

		}
	}

}

