package net.alkalus.envirosound;

import java.io.IOException;
import java.util.Properties;

public class VersionLoader {

	private static VersionLoader INSTANCE;
	private Properties versionProperties = new Properties();
	private final String version_Internal = getVersion();
	public final String VERSION = version_Internal;
	
	public static VersionLoader getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new VersionLoader();
		}
		return INSTANCE;
	}	
	
	private VersionLoader() {
		try {
			versionProperties.load(VersionLoader.class.getResourceAsStream("resources/version.properties"));
		}
		catch (IOException e) {
			
		}
	}
	public String getVersion() {
		String y = versionProperties.getProperty("version");
		return y != null ? y : "no version";
	}
}
