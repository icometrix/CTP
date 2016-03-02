package org.icometrix.stdsplugins.updater;

import java.io.File;

import org.apache.log4j.Logger;
import org.rsna.ctp.plugin.AbstractPlugin;
import org.w3c.dom.Element;

/***
 * This is the main class. Coordinates both agents and communicates with
 * external processes.
 * 
 * @author fruizdearcaute
 */
public class CtpUpdaterPlugin extends AbstractPlugin {
	static final Logger logger = Logger.getLogger(CtpUpdaterPlugin.class);
	Element updaterConfig;
	
	public CtpUpdaterPlugin(Element element) {
		super(element);
		this.updaterConfig = element;
	}
	
	/**
	 * Start the plugin.
	 */
	public void start() {
		Thread procedure = new Thread(new CtpUpdaterProcedureStarter());
		procedure.start();
	}
	
	private ReleasesDao buildDao(Element config){
		String proxy = config.getAttribute("proxy");
		if(proxy.length() == 0){
			return new ReleasesDao(config.getAttribute("host"), config.getAttribute("protocol"), 
					config.getAttribute("baseUrl"),  Integer.parseInt(config.getAttribute("port")));
		}
		return new ReleasesDao(config.getAttribute("host"), config.getAttribute("protocol"), 
				config.getAttribute("baseUrl"), Integer.parseInt(config.getAttribute("port")),
				config.getAttribute("proxy"), Integer.parseInt(config.getAttribute("proxyPort")));
	}
	
	private CtpUpdaterAgent buildUpdaterAgent(Element config, ReleasesDao dao){
		File rootCtp = new File(System.getProperty("user.dir"));
		int portCtp = Integer.parseInt(config.getAttribute("ctpPort"));
		Boolean ssl = Boolean.parseBoolean(config.getAttribute("ctpSsl"));
		return new CtpUpdaterAgent(rootCtp, portCtp, ssl, dao);
	}
	
	/****
	 * Internal class responsible for starting a separate thread for CTPUpdateProcedure
	 * @author fruizdearcaute
	 *
	 */
	private class CtpUpdaterProcedureStarter implements Runnable {

		public void run(){
			try{
				logger.info("Starting ctpUpdater!");
				
				// init updater agent
				ReleasesDao releasDao = buildDao(updaterConfig);
				CtpUpdaterAgent updaterAgent = buildUpdaterAgent(updaterConfig, releasDao);
				updaterAgent.runRobustUpdateProcedure();
				
				logger.info("Found update, exiting...");
				System.exit(0);
			} catch (Exception e) {
				logger.error("Errors starting CTP", e);
			}		

		}
	}

}
