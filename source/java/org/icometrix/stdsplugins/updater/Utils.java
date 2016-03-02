package org.icometrix.stdsplugins.updater;

/**
 * Utils stuff
 * @author fruizdearcaute
 *
 */
public class Utils {
	public static void runProcess(String[] command) throws Exception{
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.inheritIO(); //so you  see std output
		Process process = processBuilder.start();
		if( process.getErrorStream().read() != -1 ){
			throw new Exception(process.getErrorStream().toString());
		}
	}
}
