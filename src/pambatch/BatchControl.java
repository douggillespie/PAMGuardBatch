package pambatch;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JPopupMenu;

import Acquisition.FolderInputSystem;
import Acquisition.pamAudio.PamAudioFileFilter;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamGUIManager;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamController.command.BatchStatusCommand;
import PamController.command.ExitCommand;
import PamController.fileprocessing.ReprocessStoreChoice;
import PamView.PamTabPanel;
import PamguardMVC.dataOffline.OfflineDataLoadInfo;
import binaryFileStorage.BinaryStore;
import generalDatabase.DBControl;
import generalDatabase.DBControlUnit;
import networkTransfer.send.NetworkSender;
import pambatch.comms.BatchMulticastController;
import pambatch.config.BatchJobInfo;
import pambatch.config.BatchParameters;
import pambatch.config.ExternalConfiguration;
import pambatch.config.SettingsObservers;
import pambatch.ctrl.BatchState;
import pambatch.ctrl.BatchStateObserver;
import pambatch.remote.RemoteAgentHandler;
import pambatch.swing.BatchSetDialog;
import pambatch.swing.BatchTabPanel;
import pambatch.swing.CheckExistingDialog;
import pambatch.swing.JobDialog;
import pambatch.swing.SwingMenus;
import pamguard.Pamguard;

public class BatchControl extends PamControlledUnit implements PamSettings {

	public static final String unitType = "Batch Processing";
	
	private BatchParameters batchParameters = new BatchParameters();
	
	private BatchTabPanel batchTabPanel;
	
	private BatchProcess batchProcess;
	
	private SwingMenus swingMenus;
	
	private PamAudioFileFilter audioFileFilter = new PamAudioFileFilter();
	
//	private BatchMulticastRX batchMulticastRX;
	private BatchMulticastController multicastController;
	
	private RemoteAgentHandler remoteAgentHandler;
	
	private Random randomJobId;
	
	private ArrayList<BatchStateObserver> batchStateObservers = new ArrayList();
	
	private SettingsObservers settingsObservers = new SettingsObservers();
	
	private ExternalConfiguration externalConfiguration;
		
	/**
	 * @return the batchProcess
	 */
	public BatchProcess getBatchProcess() {
		return batchProcess;
	}

	private static final String DEFAULTWINDOWSEXE = "C:\\Program Files\\Pamguard\\Pamguard.exe";
	
	public BatchControl(String unitName) {
		super(unitType, unitName);
//		System.out.println("Exe command is " + findStartExecutable());
//		System.out.println("Java command is " + findJavaCommand());
		
		externalConfiguration = new ExternalConfiguration(this);
		
		PamSettingManager.getInstance().registerSettings(this);
		swingMenus = new SwingMenus(this);
		batchProcess = new BatchProcess(this);
		addPamProcess(batchProcess);
		remoteAgentHandler = new RemoteAgentHandler(this);
		if (PamGUIManager.getGUIType() != PamGUIManager.NOGUI) {
			batchTabPanel = new BatchTabPanel(this);
		}
		if (batchTabPanel != null) {
			batchTabPanel.setParams(batchParameters);
		}
		multicastController = new BatchMulticastController(this);
		
		randomJobId = new Random(System.currentTimeMillis());
		
//		batchMulticastRX = new BatchMulticastRX(this);
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
	public void notifyModelChanged(int changeType) {
		super.notifyModelChanged(changeType);
		if (changeType == PamController.INITIALIZATION_COMPLETE) {
			updateObservers(BatchState.INITIALISATIONCOMPLETE);
			loadExistingJobs(); // loads from database
			checkRunningJobs();
			externalConfiguration.settingsUpdate(SettingsObservers.CHANGE_CONFIG);
		}
	}

	/**
	 * Wait a bit to see if anything is running and trying to send
	 * status data in 
	 */
	private void checkRunningJobs() {
		/**
		 * this is only called at startup, so nothing shold be running unless it
		 * crashed half way through. If this job crashed, then it's not actually Running, 
		 * so set it to unknown. If this crashed, then perhaps it is running !
		 * first set all jobs that are running to unknown.  
		 */
		ArrayList<BatchDataUnit> jobsList = getBatchProcess().getBatchDataBlock().getDataCopy();
		int nRunning = 0;
		for (BatchDataUnit job : jobsList) {
			if (job.getBatchJobInfo().jobStatus == BatchJobStatus.RUNNING) {
				job.getBatchJobInfo().jobStatus = BatchJobStatus.UNKNOWN;
				getBatchProcess().getBatchDataBlock().updatePamData(job, System.currentTimeMillis());
				nRunning++;
			}
		}
		if (nRunning > 0) {
			// then open the progress dialog to block things for a bit ...
			CheckExistingDialog.showDialog(getGuiFrame(), this, 15);
		}
	}

	@Override
	public boolean restoreSettings(PamControlledUnitSettings pamControlledUnitSettings) {
		this.batchParameters = (BatchParameters) pamControlledUnitSettings.getSettings();
		return true;
	}
	
	/**
	 * Get the name of the local machine this is running one. 
	 * @return name of machine we're running on. 
	 */
	public static String getLocalMachineName() {
		String computerName = "This PC";
		try {
			// this gets the name of the computer, not an ip address, something like PC22586 for my laptop. 
			computerName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			
		}
		return computerName;
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
		
		// first look to see if we're running from the IDE and if so, also launch
		// new configs from the ide
		if (isBuildEnvironment()) {
			String ideStr = eclipseLaunchLine();
			if (ideStr != null) {
				return ideStr;
			}
		}
		
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
	 * Return true if running from the build environment, not from the installed executable. 
	 * @return
	 */
	private boolean isBuildEnvironment() {
		try {
			String classLocation = Pamguard.class.getProtectionDomain().getCodeSource().getLocation().toString();
			if (classLocation.contains("target/classes/")) {
				return true;
			}
		}
		catch (Exception e) {
			return false;
		}
		return false;
	}
	
	/**
	 * Copy of the Eclipse launch line. Used at the debugging stage of this. <br>
	 * Note that this is very specific to the build environment on dougs laptop and will need
	 * modifying to work anywhere else. 
	 * @return Stupidly long command line copied from the Eclipse launch configuration
	 */
	private String eclipseLaunchLine() {
		String ell = "\"C:\\Program Files\\Java\\jdk-19\\bin\\java.exe\" "
				+ "-Xmx6g "
				+ "-Djava.library.path=lib64 "
				+ "-classpath \"C:\\Users\\dg50\\source\\repos\\PAMGuardDG\\target\\classes;C:\\Users\\dg50\\.m2\\repository\\io\\github\\macster110\\jpamutils\\0.0.56\\jpamutils-0.0.56.jar;C:\\Users\\dg50\\.m2\\repository\\uk\\me\\berndporr\\iirj\\1.1\\iirj-1.1.jar;C:\\Users\\dg50\\.m2\\repository\\us\\hebi\\matlab\\mat\\mfl-core\\0.5.6\\mfl-core-0.5.6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\json\\json\\20201115\\json-20201115.jar;C:\\Users\\dg50\\.m2\\repository\\io\\github\\macster110\\jdl4pam\\0.0.94\\jdl4pam-0.0.94.jar;C:\\Users\\dg50\\.m2\\repository\\ai\\djl\\api\\0.15.0\\api-0.15.0.jar;C:\\Users\\dg50\\.m2\\repository\\com\\google\\code\\gson\\gson\\2.8.9\\gson-2.8.9.jar;C:\\Users\\dg50\\.m2\\repository\\ai\\djl\\pytorch\\pytorch-engine\\0.15.0\\pytorch-engine-0.15.0.jar;C:\\Users\\dg50\\.m2\\repository\\ai\\djl\\pytorch\\pytorch-native-auto\\1.9.1\\pytorch-native-auto-1.9.1.jar;C:\\Users\\dg50\\.m2\\repository\\ai\\djl\\tensorflow\\tensorflow-engine\\0.15.0\\tensorflow-engine-0.15.0.jar;C:\\Users\\dg50\\.m2\\repository\\ai\\djl\\tensorflow\\tensorflow-api\\0.15.0\\tensorflow-api-0.15.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\bytedeco\\javacpp\\1.5.6\\javacpp-1.5.6.jar;C:\\Users\\dg50\\.m2\\repository\\ai\\djl\\tensorflow\\tensorflow-native-auto\\2.4.1\\tensorflow-native-auto-2.4.1.jar;C:\\Users\\dg50\\.m2\\repository\\gov\\nist\\math\\jama\\1.0.3\\jama-1.0.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-controls\\16\\javafx-controls-16.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-controls\\16\\javafx-controls-16-win.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-graphics\\16\\javafx-graphics-16.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-graphics\\16\\javafx-graphics-16-win.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-base\\16\\javafx-base-16.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-base\\16\\javafx-base-16-win.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-swing\\16\\javafx-swing-16.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-swing\\16\\javafx-swing-16-win.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-media\\16\\javafx-media-16.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-media\\16\\javafx-media-16-win.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-web\\16\\javafx-web-16.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-web\\16\\javafx-web-16-win.jar;C:\\Users\\dg50\\.m2\\repository\\net\\synedra\\validatorfx\\0.4.0\\validatorfx-0.4.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-compress\\1.19\\commons-compress-1.19.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-csv\\1.7\\commons-csv-1.7.jar;C:\\Users\\dg50\\.m2\\repository\\commons-io\\commons-io\\2.6\\commons-io-2.6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-lang3\\3.9\\commons-lang3-3.9.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-math3\\3.6.1\\commons-math3-3.6.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-math\\2.2\\commons-math-2.2.jar;C:\\Users\\dg50\\.m2\\repository\\commons-net\\commons-net\\3.6\\commons-net-3.6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\controlsfx\\controlsfx\\11.0.0\\controlsfx-11.0.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\kordamp\\ikonli\\ikonli-javafx\\12.2.0\\ikonli-javafx-12.2.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\kordamp\\ikonli\\ikonli-core\\12.2.0\\ikonli-core-12.2.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\kordamp\\ikonli\\ikonli-materialdesign2-pack\\12.2.0\\ikonli-materialdesign2-pack-12.2.0.jar;C:\\Users\\dg50\\.m2\\repository\\net\\sf\\geographiclib\\GeographicLib-Java\\1.50\\GeographicLib-Java-1.50.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\gluegen\\gluegen-rt-main\\2.3.2\\gluegen-rt-main-2.3.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\gluegen\\gluegen-rt\\2.3.2\\gluegen-rt-2.3.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\gluegen\\gluegen-rt\\2.3.2\\gluegen-rt-2.3.2-natives-android-aarch64.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\gluegen\\gluegen-rt\\2.3.2\\gluegen-rt-2.3.2-natives-android-armv6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\gluegen\\gluegen-rt\\2.3.2\\gluegen-rt-2.3.2-natives-linux-amd64.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\gluegen\\gluegen-rt\\2.3.2\\gluegen-rt-2.3.2-natives-linux-armv6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\gluegen\\gluegen-rt\\2.3.2\\gluegen-rt-2.3.2-natives-linux-armv6hf.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\gluegen\\gluegen-rt\\2.3.2\\gluegen-rt-2.3.2-natives-linux-i586.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\gluegen\\gluegen-rt\\2.3.2\\gluegen-rt-2.3.2-natives-macosx-universal.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\gluegen\\gluegen-rt\\2.3.2\\gluegen-rt-2.3.2-natives-solaris-amd64.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\gluegen\\gluegen-rt\\2.3.2\\gluegen-rt-2.3.2-natives-solaris-i586.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\gluegen\\gluegen-rt\\2.3.2\\gluegen-rt-2.3.2-natives-windows-amd64.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\gluegen\\gluegen-rt\\2.3.2\\gluegen-rt-2.3.2-natives-windows-i586.jar;C:\\Users\\dg50\\.m2\\repository\\com\\healthmarketscience\\jackcess\\jackcess\\3.0.1\\jackcess-3.0.1.jar;C:\\Users\\dg50\\.m2\\repository\\commons-logging\\commons-logging\\1.2\\commons-logging-1.2.jar;C:\\Users\\dg50\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-databind\\2.10.1\\jackson-databind-2.10.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-annotations\\2.10.1\\jackson-annotations-2.10.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-core\\2.10.1\\jackson-core-2.10.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\jogl\\jogl-all-main\\2.3.2\\jogl-all-main-2.3.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\jogl\\jogl-all\\2.3.2\\jogl-all-2.3.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\jogl\\jogl-all\\2.3.2\\jogl-all-2.3.2-natives-android-aarch64.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\jogl\\jogl-all\\2.3.2\\jogl-all-2.3.2-natives-android-armv6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\jogl\\jogl-all\\2.3.2\\jogl-all-2.3.2-natives-linux-amd64.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\jogl\\jogl-all\\2.3.2\\jogl-all-2.3.2-natives-linux-armv6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\jogl\\jogl-all\\2.3.2\\jogl-all-2.3.2-natives-linux-armv6hf.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\jogl\\jogl-all\\2.3.2\\jogl-all-2.3.2-natives-linux-i586.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\jogl\\jogl-all\\2.3.2\\jogl-all-2.3.2-natives-macosx-universal.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\jogl\\jogl-all\\2.3.2\\jogl-all-2.3.2-natives-solaris-amd64.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\jogl\\jogl-all\\2.3.2\\jogl-all-2.3.2-natives-solaris-i586.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\jogl\\jogl-all\\2.3.2\\jogl-all-2.3.2-natives-windows-amd64.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jogamp\\jogl\\jogl-all\\2.3.2\\jogl-all-2.3.2-natives-windows-i586.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jflac\\jflac-codec\\1.5.2\\jflac-codec-1.5.2.jar;C:\\Users\\dg50\\.m2\\repository\\javax\\help\\javahelp\\2.0.05\\javahelp-2.0.05.jar;C:\\Users\\dg50\\.m2\\repository\\net\\java\\dev\\jna\\jna\\5.5.0\\jna-5.5.0.jar;C:\\Users\\dg50\\.m2\\repository\\net\\java\\dev\\jna\\jna-platform\\5.5.0\\jna-platform-5.5.0.jar;C:\\Users\\dg50\\.m2\\repository\\com\\jcraft\\jsch\\0.1.55\\jsch-0.1.55.jar;C:\\Users\\dg50\\.m2\\repository\\com\\fazecast\\jSerialComm\\2.5.3\\jSerialComm-2.5.3.jar;C:\\Users\\dg50\\.m2\\repository\\edu\\emory\\mathcs\\JTransforms\\2.4\\JTransforms-2.4.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\mail\\javax.mail\\1.6.2\\javax.mail-1.6.2.jar;C:\\Users\\dg50\\.m2\\repository\\javax\\activation\\activation\\1.1\\activation-1.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\diffplug\\matsim\\matfilerw\\3.1.1\\matfilerw-3.1.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\drewnoakes\\metadata-extractor\\2.12.0\\metadata-extractor-2.12.0.jar;C:\\Users\\dg50\\.m2\\repository\\com\\adobe\\xmp\\xmpcore\\6.0.6\\xmpcore-6.0.6.jar;C:\\Users\\dg50\\.m2\\repository\\mysql\\mysql-connector-java\\8.0.18\\mysql-connector-java-8.0.18.jar;C:\\Users\\dg50\\.m2\\repository\\com\\google\\protobuf\\protobuf-java\\3.17.0\\protobuf-java-3.17.0.jar;C:\\Users\\dg50\\.m2\\repository\\edu\\ucar\\netcdfAll\\4.6.14\\netcdfAll-4.6.14.jar;C:\\Users\\dg50\\.m2\\repository\\com\\opencsv\\opencsv\\5.0\\opencsv-5.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-text\\1.7\\commons-text-1.7.jar;C:\\Users\\dg50\\.m2\\repository\\commons-beanutils\\commons-beanutils\\1.9.4\\commons-beanutils-1.9.4.jar;C:\\Users\\dg50\\.m2\\repository\\commons-collections\\commons-collections\\3.2.2\\commons-collections-3.2.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-collections4\\4.4\\commons-collections4-4.4.jar;C:\\Users\\dg50\\.m2\\repository\\org\\postgresql\\postgresql\\42.2.24\\postgresql-42.2.24.jar;C:\\Users\\dg50\\.m2\\repository\\org\\checkerframework\\checker-qual\\3.5.0\\checker-qual-3.5.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-script-engine\\0.9.2725\\renjin-script-engine-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-core\\0.9.2725\\renjin-core-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-appl\\0.9.2725\\renjin-appl-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-blas\\0.9.2725\\renjin-blas-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-nmath\\0.9.2725\\renjin-nmath-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-math-common\\0.9.2725\\renjin-math-common-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-lapack\\0.9.2725\\renjin-lapack-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\gcc-runtime\\0.9.2725\\gcc-runtime-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\core\\1.1.2\\core-1.1.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-vfs2\\2.0\\commons-vfs2-2.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\maven\\scm\\maven-scm-api\\1.4\\maven-scm-api-1.4.jar;C:\\Users\\dg50\\.m2\\repository\\org\\codehaus\\plexus\\plexus-utils\\1.5.6\\plexus-utils-1.5.6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\maven\\scm\\maven-scm-provider-svnexe\\1.4\\maven-scm-provider-svnexe-1.4.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\maven\\scm\\maven-scm-provider-svn-commons\\1.4\\maven-scm-provider-svn-commons-1.4.jar;C:\\Users\\dg50\\.m2\\repository\\regexp\\regexp\\1.3\\regexp-1.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\tukaani\\xz\\1.8\\xz-1.8.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-asm\\5.0.4b\\renjin-asm-5.0.4b.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-guava\\17.0b\\renjin-guava-17.0b.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\codemodel\\codemodel\\2.6\\codemodel-2.6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\stats\\0.9.2725\\stats-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-gnur-runtime\\0.9.2725\\renjin-gnur-runtime-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\methods\\0.9.2725\\methods-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\tools\\0.9.2725\\tools-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\datasets\\0.9.2725\\datasets-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\utils\\0.9.2725\\utils-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\grDevices\\0.9.2725\\grDevices-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jfree\\jfreesvg\\3.3\\jfreesvg-3.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\graphics\\0.9.2725\\graphics-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\compiler\\0.9.2725\\compiler-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\net\\sourceforge\\f2j\\arpack_combined_all\\0.1\\arpack_combined_all-0.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_ref-osx-x86_64\\1.1\\netlib-native_ref-osx-x86_64-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\native_ref-java\\1.1\\native_ref-java-1.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\jniloader\\1.1\\jniloader-1.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_ref-linux-x86_64\\1.1\\netlib-native_ref-linux-x86_64-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_ref-linux-i686\\1.1\\netlib-native_ref-linux-i686-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_ref-win-x86_64\\1.1\\netlib-native_ref-win-x86_64-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_ref-win-i686\\1.1\\netlib-native_ref-win-i686-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_ref-linux-armhf\\1.1\\netlib-native_ref-linux-armhf-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_system-osx-x86_64\\1.1\\netlib-native_system-osx-x86_64-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\native_system-java\\1.1\\native_system-java-1.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_system-linux-x86_64\\1.1\\netlib-native_system-linux-x86_64-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_system-linux-i686\\1.1\\netlib-native_system-linux-i686-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_system-linux-armhf\\1.1\\netlib-native_system-linux-armhf-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_system-win-x86_64\\1.1\\netlib-native_system-win-x86_64-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_system-win-i686\\1.1\\netlib-native_system-win-i686-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\org\\slf4j\\slf4j-api\\1.8.0-beta4\\slf4j-api-1.8.0-beta4.jar;C:\\Users\\dg50\\.m2\\repository\\org\\slf4j\\slf4j-nop\\1.8.0-beta4\\slf4j-nop-1.8.0-beta4.jar;C:\\Users\\dg50\\.m2\\repository\\org\\docx4j\\docx4j-JAXB-ReferenceImpl\\11.1.3\\docx4j-JAXB-ReferenceImpl-11.1.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\docx4j\\docx4j-core\\11.1.3\\docx4j-core-11.1.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\docx4j\\docx4j-openxml-objects\\11.1.3\\docx4j-openxml-objects-11.1.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\docx4j\\docx4j-openxml-objects-pml\\11.1.3\\docx4j-openxml-objects-pml-11.1.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\docx4j\\docx4j-openxml-objects-sml\\11.1.3\\docx4j-openxml-objects-sml-11.1.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\plutext\\jaxb-svg11\\1.0.2\\jaxb-svg11-1.0.2.jar;C:\\Users\\dg50\\.m2\\repository\\net\\engio\\mbassador\\1.3.2\\mbassador-1.3.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\slf4j\\jcl-over-slf4j\\1.7.26\\jcl-over-slf4j-1.7.26.jar;C:\\Users\\dg50\\.m2\\repository\\commons-codec\\commons-codec\\1.12\\commons-codec-1.12.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.5.8\\httpclient-4.5.8.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.4.11\\httpcore-4.4.11.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\xmlgraphics\\xmlgraphics-commons\\2.3\\xmlgraphics-commons-2.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\docx4j\\org\\apache\\xalan-interpretive\\11.0.0\\xalan-interpretive-11.0.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\docx4j\\org\\apache\\xalan-serializer\\11.0.0\\xalan-serializer-11.0.0.jar;C:\\Users\\dg50\\.m2\\repository\\net\\arnx\\wmf2svg\\0.9.8\\wmf2svg-0.9.8.jar;C:\\Users\\dg50\\.m2\\repository\\org\\antlr\\antlr-runtime\\3.5.2\\antlr-runtime-3.5.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\antlr\\stringtemplate\\3.2.1\\stringtemplate-3.2.1.jar;C:\\Users\\dg50\\.m2\\repository\\antlr\\antlr\\2.7.7\\antlr-2.7.7.jar;C:\\Users\\dg50\\.m2\\repository\\com\\google\\errorprone\\error_prone_annotations\\2.3.3\\error_prone_annotations-2.3.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\jaxb\\jaxb-runtime\\2.3.2\\jaxb-runtime-2.3.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\jaxb\\txw2\\2.3.2\\txw2-2.3.2.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\istack\\istack-commons-runtime\\3.0.8\\istack-commons-runtime-3.0.8.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jvnet\\staxex\\stax-ex\\1.8.1\\stax-ex-1.8.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\xml\\fastinfoset\\FastInfoset\\1.2.16\\FastInfoset-1.2.16.jar;C:\\Users\\dg50\\.m2\\repository\\jakarta\\activation\\jakarta.activation-api\\1.2.1\\jakarta.activation-api-1.2.1.jar;C:\\Users\\dg50\\.m2\\repository\\jakarta\\xml\\bind\\jakarta.xml.bind-api\\2.3.2\\jakarta.xml.bind-api-2.3.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\xerial\\sqlite-jdbc\\3.28.0\\sqlite-jdbc-3.28.0.jar;C:\\Users\\dg50\\.m2\\repository\\net\\sf\\ucanaccess\\ucanaccess\\4.0.4\\ucanaccess-4.0.4.jar;C:\\Users\\dg50\\.m2\\repository\\org\\hsqldb\\hsqldb\\2.3.1\\hsqldb-2.3.1.jar;C:\\Users\\dg50\\.m2\\repository\\nz\\ac\\waikato\\cms\\weka\\weka-dev\\3.7.7\\weka-dev-3.7.7.jar;C:\\Users\\dg50\\.m2\\repository\\net\\sf\\squirrel-sql\\thirdparty-non-maven\\java-cup\\0.11a\\java-cup-0.11a.jar;C:\\Users\\dg50\\.m2\\repository\\org\\pentaho\\pentaho-commons\\pentaho-package-manager\\1.0.3\\pentaho-package-manager-1.0.3.jar;C:\\Users\\dg50\\.m2\\repository\\javax\\vecmath\\vecmath\\1.5.2\\vecmath-1.5.2.jar;C:\\Users\\dg50\\.m2\\repository\\pamguard\\org\\x3\\2.2.2\\x3-2.2.2.jar;C:\\Users\\dg50\\.m2\\repository\\it\\sauronsoftware\\jave\\1.0.2\\jave-1.0.2.jar;C:\\Users\\dg50\\.m2\\repository\\com\\synthbot\\jasiohost\\1.0.0\\jasiohost-1.0.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\springframework\\spring-core\\5.2.3.RELEASE\\spring-core-5.2.3.RELEASE.jar;C:\\Users\\dg50\\.m2\\repository\\org\\springframework\\spring-jcl\\5.2.3.RELEASE\\spring-jcl-5.2.3.RELEASE.jar;C:\\Users\\dg50\\.m2\\repository\\com\\1stleg\\jnativehook\\2.1.0\\jnativehook-2.1.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\swinglabs\\swingx\\swingx-all\\1.6.5-1\\swingx-all-1.6.5-1.jar;C:\\Users\\dg50\\.m2\\repository\\io\\github\\mkpaz\\atlantafx-base\\1.0.0\\atlantafx-base-1.0.0.jar\" "
				+ " pamguard.Pamguard "
				+ "-smru";
		return ell;
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

	/**
	 * open PAMGuard in a new program instance, 
	 * @param psfxFile psfx file
	 * @param withComms with comms for external control. false for now, will probably eventually be a reference to a job set. 
	 */
	public void launchPamguard(String psfxFile, boolean withComms) {
		String pgExe = "C:\\Program Files\\Pamguard\\Pamguard.exe";

		final ArrayList<String> command = new ArrayList<String>();
		command.add(pgExe);
		command.add("-psf");
		command.add(psfxFile);
		
//		if (2>1) {
//			String oneCmd = batchProcess.makeOneLinecommand(command);
//			Desktop.getDesktop().
//		}
//		else {

		final ProcessBuilder builder = new ProcessBuilder(command);
		try {
			builder.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		}
		
	}
	
	/**
	 * This is all launch commands APART from those needed to start PAMGaurd and anything 
	 * to do with control specific params such as UDP settings, etc. 
	 * @param nextJob
	 * @return
	 */
	public ArrayList<String> getBatchJobLaunchParams(BatchDataUnit nextJob) {
		switch (batchParameters.getBatchMode()) {
		case NORMAL:
			return getNormalJobLaunchParams(nextJob);
		case VIEWER:
			return getViewerJobLaunchParams(nextJob);
		default:
			return null;		
		}
	}
	
	/**
	 * Command line options for running offline tasks. These are quite different
	 * to the command line options for running normal mode since we're passing 
	 * a database and also an updated psfx so that the remote jobs can all get their
	 * updated settings for the various tasks. 
	 * @param nextJob
	 * @return
	 */
	private ArrayList<String> getViewerJobLaunchParams(BatchDataUnit nextJob) {
		// TODO Auto-generated method stub		
		BatchJobInfo jobInfo = nextJob.getBatchJobInfo();
		String pgExe = findStartExecutable();
		if (pgExe == null) {
			return null;
		}
		ArrayList<String> command = new ArrayList<>();
		command.add("-v"); // viewer mode
		command.add(DBControl.GlobalDatabaseNameArg); // viewer database. 
		command.add(jobInfo.outputDatabaseName);
		// and the psf since it will need to take the settings from it to override what's in the database. 
		command.add("-psf");
		String psf = batchParameters.getMasterPSFX();
		command.add(psf);

		if (jobInfo.soundFileFolder != null && jobInfo.soundFileFolder.length() > 0) {
			command.add(psf);
			command.add(FolderInputSystem.GlobalWavFolderArg);
			command.add(jobInfo.soundFileFolder);
		}
		if (jobInfo.outputBinaryFolder != null && jobInfo.outputBinaryFolder.length() > 0) {
			command.add(BinaryStore.GlobalFolderArg);
			command.add(jobInfo.outputBinaryFolder);
		}
		
		//the job id stuff
		command.add("-multicast");
		command.add(batchParameters.getMulticastAddress());
		command.add(String.format("%d", batchParameters.getMulticastPort()));
		command.add(NetworkSender.ID1);
		command.add(String.format("%d", nextJob.getDatabaseIndex()));
		int jobId2 = randomJobId.nextInt(10000); // generate a new random up to 4 digit integer
		command.add(NetworkSender.ID2);
		command.add(String.format("%d", jobId2));
		jobInfo.setJobId2(jobId2);
		
		
		
		return command;
	}

	/**
	 * Command line options for normal mode operations, i.e. processing a ton of 
	 * sound files with a standard psfx file. 
	 * @param nextJob
	 * @return
	 */
	public ArrayList<String> getNormalJobLaunchParams(BatchDataUnit nextJob) {
		
		BatchJobInfo jobInfo = nextJob.getBatchJobInfo();
		String pgExe = findStartExecutable();
		if (pgExe == null) {
			return null;
		}
		ArrayList<String> command = new ArrayList<>();
//		command.add(pgExe);
		command.add("-psf");
		String psf = batchParameters.getMasterPSFX();
		if (psf == null) {
			return null;
		}
		command.add(psf);
		command.add(FolderInputSystem.GlobalWavFolderArg);
		command.add(jobInfo.soundFileFolder);
		if (jobInfo.outputBinaryFolder != null) {
			command.add(BinaryStore.GlobalFolderArg);
			command.add(jobInfo.outputBinaryFolder);
		}
		if (jobInfo.outputDatabaseName != null) {
			command.add(DBControl.GlobalDatabaseNameArg);
			command.add(jobInfo.outputDatabaseName);
		}
		command.add("-autostart");
		/**
		 * Probably don't want to auto exit so that the monitor can check the job status
		 * and see clearly that it's finished rather than crashed, then tell it to exit.
		 */
		if (batchParameters.isNoGUI()) {
			command.add("-nogui");
		}
//		command.add("-autoexit"); 
		command.add(ReprocessStoreChoice.paramName);
		String repChoice = batchParameters.getReprocessChoice().name();
		command.add(repChoice);
		command.add("-multicast");
		command.add(batchParameters.getMulticastAddress());
		command.add(String.format("%d", batchParameters.getMulticastPort()));
		command.add(NetworkSender.ID1);
		command.add(String.format("%d", nextJob.getDatabaseIndex()));
		int jobId2 = randomJobId.nextInt(10000); // generate a new random up to 4 digit integer
		command.add(NetworkSender.ID2);
		command.add(String.format("%d", jobId2));
		jobInfo.setJobId2(jobId2);
		
		return command;
	}
	
	/**
	 * Quick find of database controller. 
	 * @return database control or null
	 */
	public DBControlUnit findDatabaseControl() {
		return DBControlUnit.findDatabaseControl();
	}
	
	/**
	 * Quick find of binary store. 
	 * @return
	 */
	public BinaryStore findBinaryStore() {
		return  BinaryStore.findBinaryStoreControl();
	}

	/**
	 * Load all existing jobs from database. 
	 */
	private void loadExistingJobs() {
//		batchProcess.getBatchDataBlock().loadViewerData(0, Long.MAX_VALUE, null);
		DBControlUnit dbControl = findDatabaseControl();
		if (dbControl == null) {
			return;
		}
		OfflineDataLoadInfo dlinf = new OfflineDataLoadInfo(0, Long.MAX_VALUE);
		dbControl.loadData(batchProcess.getBatchDataBlock(), dlinf, null);
		checkConflictingJobs();
	}

	/**
	 * Create a new job. 
	 * Show a dialog to get required parameters. 
	 */
	public void createJob() {
		BatchDataUnit newJobData = new BatchDataUnit(System.currentTimeMillis(), null);
//		batchProcess.getBatchLogging().logData(DBControlUnit.findConnection(), newJobData);
		boolean ok = JobDialog.showDialog(getGuiFrame(), this, newJobData); 
		if (ok) {
			batchProcess.getBatchDataBlock().addPamData(newJobData);
			batchProcess.getBatchDataBlock().updatePamData(newJobData, newJobData.getTimeMilliseconds());
//			batchProcess.getBatchLogging().reLogData(DBControlUnit.findConnection(), newJobData);

		}
		else {
			batchProcess.getBatchLogging().deleteData(newJobData);
		}
		checkConflictingJobs();
	}

	/**
	 * Create a set of batch jobs based on a common folder structure. 
	 * i.e. it gets a source folder, then generates a set for each sub folder of data. 
	 */
	public void createJobSet() {
		ArrayList<BatchJobInfo> jobSets = BatchSetDialog.showDialog(this.getGuiFrame(), this);
		if (jobSets == null) {
			return;
		}
		for (BatchJobInfo jobSet : jobSets) {
			BatchDataUnit newJobData = new BatchDataUnit(System.currentTimeMillis(), jobSet);
//			batchProcess.getBatchLogging().logData(DBControlUnit.findConnection(), newJobData);
			batchProcess.getBatchDataBlock().addPamData(newJobData);
		}
		checkConflictingJobs();
	}
	
	/**
	 * Check to see if any jobs are conflicting in having the same source or output. 
	 * @return
	 */
	private int checkConflictingJobs() {
		ArrayList<BatchDataUnit> allJobs = batchProcess.getBatchDataBlock().getDataCopy();
		int conflicts = 0;
		for (int i = 0; i < allJobs.size(); i++) {
			allJobs.get(i).setConflictingJob(null);
		}
		for (int i = 0; i < allJobs.size(); i++) {
			for (int j = i+1; j < allJobs.size(); j++) {
				if (hasConflict(allJobs.get(i), allJobs.get(j))) {
					conflicts ++;
					allJobs.get(i).setConflictingJob(allJobs.get(j));
					allJobs.get(j).setConflictingJob(allJobs.get(i));
				}
			}
		}
		return conflicts;
	}

	/**
	 * Jobs are said to conflict if any of the three folders  / files is the same. 
	 * @param batchDataUnit1
	 * @param batchDataUnit2
	 * @return
	 */
	private boolean hasConflict(BatchDataUnit batchDataUnit1, BatchDataUnit batchDataUnit2) {
		if (batchDataUnit1 == null || batchDataUnit2 == null) {
			return false;
		}
		BatchJobInfo jobInf1 = batchDataUnit1.getBatchJobInfo();
		BatchJobInfo jobInf2 = batchDataUnit2.getBatchJobInfo();
		return hasConflict(jobInf1, jobInf2);
	}

	/**
	 * REturn true if info is conflicting
	 * @param jobInf1
	 * @param jobInf2
	 * @return
	 */
	private boolean hasConflict(BatchJobInfo jobInf1, BatchJobInfo jobInf2) {
		if (isSame(jobInf1.soundFileFolder, jobInf2.soundFileFolder)) {
			return true;
		}
		if (isSame(jobInf1.outputBinaryFolder, jobInf2.outputBinaryFolder)) {
			return true;
		}
		if (isSame(jobInf1.outputDatabaseName, jobInf2.outputDatabaseName)) {
			return true;
		}
		return false;
	}

	/**
	 * Check to see if two paths are the same
	 * @param path1
	 * @param path2
	 * @return
	 */
	private boolean isSame(String path1, String path2) {
		if (path1 == null || path2 == null) {
			return false;
		}
		if (path1.length() == 0 && path2.length() == 0) {
			return false;
		}
		return path1.equalsIgnoreCase(path2);
	}

	/**
	 * Make a swing menu for use probably in the table view of jobs. 
	 * @param dataUnit
	 * @return
	 */
	public JPopupMenu getSwingPopupMenu(BatchDataUnit dataUnit) {
		return swingMenus.getSwingPopupMenu(dataUnit);
	}

	/**
	 * Delete a job and remove it from the database. 
	 * @param dataUnit
	 */
	public void deleteJob(BatchDataUnit dataUnit) {
		batchProcess.getBatchDataBlock().remove(dataUnit, true);
		checkConflictingJobs();
		batchProcess.getBatchDataBlock().notifyObservers();
	}

	/**
	 * Delete a job and remove it from the database. 
	 * @param dataUnit
	 */
	public void editJob(BatchDataUnit dataUnit) {
		boolean ok = JobDialog.showDialog(getGuiFrame(), this, dataUnit); 
		if (ok) {
			batchProcess.getBatchDataBlock().updatePamData(dataUnit, System.currentTimeMillis());
		}
	}

	/**
	 * Function to find sub folders of a source folder which contain one or more sound files. 
	 * @param sourceFolder
	 * @return 
	 */
	public ArrayList<SourceSubFolderInfo> findSourceSubFolders(File sourceFolder) {

		ArrayList<SourceSubFolderInfo> sourceSubFolders = new ArrayList<>();
		/**
		 * first list sub folders within the sourceFolder. ....
		 */
		File[] subs = sourceFolder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File pathname) {
				return pathname.isDirectory();
			}
		});
		if (subs == null) {
			return sourceSubFolders;
		}
		for (int i = 0; i < subs.length; i++) {
			/*
			 *  see if there are ANY files in that folder.
			 *  Don't want to catalogue them all since that may take ages, so just see if there 
			 *  is one or more.  
			 */
			if (hasAnyInput(subs[i])) {
				sourceSubFolders.add(new SourceSubFolderInfo(subs[i], -1));
			}
		}
		
		return sourceSubFolders;
	}

	/**
	 * Return true if the folder or any sub folder have sound files. 
	 * @param file
	 * @return
	 */
	private boolean hasAnyInput(File folder) {
		File[] files = folder.listFiles(audioFileFilter);
		if (files == null) {
			return false;
		}
		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				boolean hasSound = hasAnyInput(files[i]);
				if (hasSound) {
					return true;
				}
			}
			else {
				// if there is a file which isn't a folder, then it should be a
				// audio file. 
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the batchParameters
	 */
	public BatchParameters getBatchParameters() {
		return batchParameters;
	}

	@Override
	public void pamClose() {
		super.pamClose();
		multicastController.close();
	}

	/**
	 * @return the multicastController
	 */
	public BatchMulticastController getMulticastController() {
		return multicastController;
	}

	/**
	 * Status data returned from a multicast request to all PAMGuard processes. 
	 * @param packet
	 */
	public void newStatusPacket(DatagramPacket packet) {
		if (packet == null || packet.getLength() == 0) {
			return;
		}
		String data = new String(packet.getData(), 0, packet.getLength());
		/*
		 *  these strings coming back from multicast should be in the form
		 *  commandname
		 *  id1
		 *  id1
		 *  data fields ...
		 *  separated by commas
		 */
		String[] commandBits = data.split(",");
		if (commandBits.length < 4) {
			System.out.println("Unknown data format from UDP: " + data);
			return;
		}
		String command = commandBits[0].trim();
		int id1 = 0, id2 = 0;
		try {
			id1 = Integer.valueOf(commandBits[1]);
			id2 = Integer.valueOf(commandBits[2]);			
		}
		catch(NumberFormatException e) {
			System.out.println("Invalid station id fields in : " + data);
			return;
		}
		// now find the processing data unit for id1
		BatchDataUnit jobData = batchProcess.getBatchDataBlock().findByDatabaseIndex(id1);
		if (jobData == null) {
			System.out.println("Unable to find batch job data for job id " + id1);
			return;
		}
//		System.out.printf("Update for job id %d: %s\n", id1, data);
		updateJobStatus(jobData, commandBits);
		
	}
	
	/**
	 * Called when there is a significant change type, e.g. mode change or 
	 * new configuration, so that various bits of the batch processor can respond. 
	 * @param changeType
	 */
	public void settingsChange(int changeType) {
		settingsObservers.notifyObservers(changeType);
	}
	
	/**
	 * Get the list of settings observers and add yourself to it or remove yourself. 
	 * @return
	 */
	public SettingsObservers getSettingsObservers() {
		return settingsObservers;
	}

	/**
	 * update job data and where necessary act on it. 
	 * @param jobData
	 * @param commandBits
	 */
	private void updateJobStatus(BatchDataUnit jobData, String[] commandBits) {
		if (commandBits[0].trim().equals(BatchStatusCommand.commandId)) {
			int nFiles = -1;
			int iFile = 0;
			int status = -1;
			if (commandBits.length < 5) {
				System.out.println("Command is too short");
				return;
			}
			try {
				nFiles = Integer.valueOf(commandBits[3]);
				iFile = Integer.valueOf(commandBits[4]);
				status = Integer.valueOf(commandBits[5]);
			}
			catch (NumberFormatException e) {
			}
			boolean complete = iFile >= nFiles;
			BatchJobStatus jobStatus = BatchJobStatus.UNKNOWN;
			if (iFile == 0) {
				jobStatus = BatchJobStatus.NOTSTARTED;
			}
			if (iFile >= nFiles) {
				jobStatus = BatchJobStatus.COMPLETE;
				closeJob(jobData);
			}
			else {
				jobStatus = BatchJobStatus.RUNNING;
			}
			double percent = iFile * 100. / nFiles;
			percent = Math.min(percent, 100);
			BatchJobInfo jobInfo = jobData.getBatchJobInfo();
			jobInfo.jobStatus = jobStatus;
			jobInfo.percentDone = percent;
			batchProcess.updateJobStatus(jobData);
			
		}
	}

	/**
	 * A job is complete so tell it to exit.  
	 * @param jobData
	 */
	private void closeJob(BatchDataUnit jobData) {
		BatchJobInfo jobInfo = jobData.getBatchJobInfo();
		multicastController.targetCommand(jobData.getDatabaseIndex(), jobInfo.getJobId2(), ExitCommand.commandId);
	}

	/**
	 * Stop and cancel a running job. 
	 * @param dataUnit
	 */
	public void cancelJob(BatchDataUnit dataUnit) {
		closeJob(dataUnit);
		dataUnit.getBatchJobInfo().jobStatus = BatchJobStatus.CANCELLED;
		batchProcess.updateJobStatus(dataUnit);
	}

	public void reprocessJob(BatchDataUnit dataUnit) {
		dataUnit.getBatchJobInfo().jobStatus = BatchJobStatus.NOTSTARTED;
		batchProcess.updateJobStatus(dataUnit);	
	}

	/**
	 * @return the remoteAgentHandler
	 */
	public RemoteAgentHandler getRemoteAgentHandler() {
		return remoteAgentHandler;
	}

	/**
	 * Add an observer to get notifications whenever anything changes. 
	 * @param stateObserver
	 */
	public void addStateObserver(BatchStateObserver stateObserver) {
		batchStateObservers.add(stateObserver);
	}

	/**
	 * Remove an observer that got notifications whenever anything changes. 
	 * @param stateObserver
	 */
	public boolean removeStateObserver(BatchStateObserver stateObserver) {
		return batchStateObservers.remove(stateObserver);
	}
	
	/**
	 * notify all state observers with null data. 
	 * @param batchState new state
	 */
	public void updateObservers(BatchState batchState) {
		updateObservers(batchState, null);
	}
	/**
	 * notify all state observers. 
	 * @param batchState new state
	 * @param data state data (generally null)
	 */
	public void updateObservers(BatchState batchState, Object data) {
		for (BatchStateObserver obs : batchStateObservers) {
			obs.update(batchState, data);
		}
	}

	/**
	 * @return the externalConfiguration
	 */
	public ExternalConfiguration getExternalConfiguration() {
		return externalConfiguration;
	}
}
