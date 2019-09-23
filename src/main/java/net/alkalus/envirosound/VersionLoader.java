package net.alkalus.envirosound;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

public class VersionLoader {

	private static VersionLoader INSTANCE;
	public final String VERSION;
	
	public static VersionLoader getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new VersionLoader();
		}
		return INSTANCE;
	}	
	
	private VersionLoader() {
		File mcmod = new File(getClass().getClassLoader().getResource("mcmod.info").getFile());		
		if (mcmod == null || !mcmod.exists()) {
			URL temp = getClass().getResource("mcmod.info");
			if (temp != null) {
				try {
					FileUtils.copyURLToFile(temp, mcmod);
				}
				catch (IOException e) {
					mcmod = new File("resources/mcmod.info");
				}
			}
			else {
				mcmod = new File("resources/mcmod.info");
			}
		}		
		String VERSION_TEMP = "no version";
		try (BufferedReader br = new BufferedReader(new FileReader(mcmod))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		       if (line != null && line.length() > 0) {
		    	   if (line.contains("\"version\": \"")) {
		    		   VERSION_TEMP = line;
		    	   }
		       }
		    }
		}
		catch (FileNotFoundException e) {
		}
		catch (IOException e) {
		}
		if (!VERSION_TEMP.equals("no version")) {
			String temp1 = VERSION_TEMP.replace("", "");
			temp1 = temp1.replaceAll(" ", "").replace("version", "").replaceAll(":", "").replaceAll("\"", "").replaceAll(",", "");
			VERSION_TEMP = temp1;
		}
		VERSION = VERSION_TEMP;
	}
	public String getVersion() {
		return VERSION;
	}
}
