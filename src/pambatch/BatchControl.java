package pambatch;

import java.io.File;
import java.io.Serializable;
import java.net.URL;

import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamSettings;
import PamView.PamTabPanel;
import pambatch.config.BatchParameters;
import pambatch.swing.BatchTabPanel;
import pamguard.Pamguard;

public class BatchControl extends PamControlledUnit implements PamSettings {

	public static final String unitType = "Batch Processing";
	
	private BatchParameters batchParameters = new BatchParameters();
	
	private BatchTabPanel batchTabPanel;
	
	private static final String DEFAULTWINDOWSEXE = "C:\\Program Files\\Pamguard\\Pamguard.exe";
	
	public BatchControl(String unitName) {
		super(unitType, unitName);
		System.out.println("Exe command is " + findStartExecutable());
		System.out.println("Java command is " + findJavaCommand());
	}

	@Override
	public Serializable getSettingsReference() {
		return batchParameters;
	}

	@Override
	public long getSettingsVersion() {
		return BatchParameters.serialVersionUID;
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		this.batchParameters = (BatchParameters) pamControlledUnitSettings.getSettings();
		return true;
	}

	@Override
	public synchronized PamTabPanel getTabPanel() {
		if (batchTabPanel == null) {
			batchTabPanel = new BatchTabPanel(this);
		}
		return batchTabPanel;
	}

	/**
	 * PAMGuard is normally launched from an exe file pamguard.exe. Try to find it in the 
	 * working folder. 
	 * @return null or the pamguard.exe file. 
	 */
	public String findStartExecutable() {
		// first look in the current directory and see if it's there. 
		String userDir = System.getProperty("user.dir");
		if (userDir != null) {
			File userFile = new File(userDir+File.pathSeparator+"Pamguard.exe");
			if (userFile.exists()) {
				return userFile.getAbsolutePath();
			}
		}
		// try the default location. 
		File tryDir = new File(DEFAULTWINDOWSEXE);
		if (tryDir.exists()) {
			return tryDir.getAbsolutePath();
		}
		// give up !
		return null;
	}
	
	/**
	 * Work out a java command which could be used to start PAMGuard. 
	 * @return
	 */
	public String findJavaCommand() {
		String javaHome = System.getProperty("java.home");
		if (javaHome == null) {
			return null;
		}
		File javaExe = new File(javaHome + File.pathSeparator + "bin" + File.pathSeparator + "java.exe");
		if (javaExe == null) {
			return null;
		}
		// get the name of the current PAMGuard jar file. 
		String jarFile = null;
		try {
			// get the java runnable file name. 
			//	    	http://stackoverflow.com/questions/4294522/jar-file-name-form-java-code
			URL jarURL = Pamguard.class.getProtectionDomain().getCodeSource().getLocation();
			if (jarURL != null) {
				jarFile = jarURL.getFile();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (jarFile == null) {
			return null;
		}
		String cmd = String.format("\"%s\" -jar \"%s\"", javaExe.getAbsolutePath(), jarFile);
		
		
		return cmd;
	}
	
}
