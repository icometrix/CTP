package main;

import org.apache.log4j.Logger;

/**
 * Utils stuff
 * @author fruizdearcaute
 *
 */
public class Utils {
	final static Logger logger = Logger.getLogger(Utils.class);
	public static void runProcess(String[] command) throws Exception{
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.inheritIO(); //so you  see std output
		Process process = processBuilder.start();
		if( process.getErrorStream().read() != -1 ){
			throw new Exception(process.getErrorStream().toString());
		}
	}
}
