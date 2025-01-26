package pambatch;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Random;

import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.w3c.dom.Document;

import Acquisition.FolderInputSystem;
import Acquisition.pamAudio.PamAudioFileFilter;
import Array.ArrayDialog;
import Array.ArrayManager;
import Array.ArrayParameters;
import Array.PamArray;
import PamController.PSFXReadWriter;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamGUIManager;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamController.PamSettingsGroup;
import PamController.command.BatchCommand;
import PamController.command.BatchStatusCommand;
import PamController.command.CommandManager;
import PamController.command.ExitCommand;
import PamController.command.SetSerializedSettingsCommand;
import PamController.command.SetXMLSettings;
import PamController.command.StartCommand;
import PamController.command.TerminalController;
import PamController.fileprocessing.ReprocessStoreChoice;
import PamController.settings.output.xml.PamguardXMLWriter;
import PamUtils.PamFileChooser;
import PamUtils.PamFileFilter;
import PamView.PamTabPanel;
import PamView.dialog.warn.WarnOnce;
import PamguardMVC.dataOffline.OfflineDataLoadInfo;
import binaryFileStorage.BinaryStore;
import generalDatabase.DBControl;
import generalDatabase.DBControlUnit;
import metadata.MetaDataContol;
import metadata.PamguardMetaData;
import networkTransfer.send.NetworkSender;
import offlineProcessing.OfflineTask;
import offlineProcessing.OfflineTaskManager;
import pambatch.comms.BatchMulticastController;
import pambatch.config.BatchJobInfo;
import pambatch.config.BatchMode;
import pambatch.config.BatchParameters;
import pambatch.config.ExternalConfiguration;
import pambatch.config.SettingsObservers;
import pambatch.config.ViewerDatabase;
import pambatch.ctrl.BatchState;
import pambatch.ctrl.BatchStateObserver;
import pambatch.remote.RemoteAgentHandler;
import pambatch.swing.NormalSetDialog;
import pambatch.swing.BatchTabPanel;
import pambatch.swing.CheckExistingDialog;
import pambatch.swing.JobDialog;
import pambatch.swing.OfflineJobDialog;
import pambatch.swing.OnlineJobDialog;
import pambatch.swing.SwingMenus;
import pambatch.swing.ViewerSetDialog;
import pambatch.tasks.OfflineTaskDataBlock;
import pambatch.tasks.OfflineTaskDataUnit;
import pambatch.tasks.OfflineTaskFunctions;
import pambatch.tasks.TaskSelection;
import pamguard.GlobalArguments;
import pamguard.Pamguard;
import tethys.TethysControl;

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

	private PSFXMonitor psfxMonitor;
	
	private OfflineTaskFunctions offlineTaskFunctions;

	/**
	 * @return the batchProcess
	 */
	public BatchProcess getBatchProcess() {
		return batchProcess;
	}
	/**
	 * For dev, add following to PAMModel
		mi = PamModuleInfo.registerControlledUnit(BatchControl.class.getName(), BatchControl.unitType);
		mi.setToolTipText("Batch processing control");
		mi.setModulesMenuGroup(utilitiesGroup);
		mi.setMaxNumber(1);
	 */

	private static final String DEFAULTWINDOWSEXE = "C:\\Program Files\\Pamguard\\Pamguard.exe";

	public BatchControl(String unitName) {
		super(unitType, unitName);
		//		System.out.println("Exe command is " + findStartExecutable());
		//		System.out.println("Java command is " + findJavaCommand());

		externalConfiguration = new ExternalConfiguration(this);
		
		offlineTaskFunctions = new OfflineTaskFunctions(this);

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

		audioFileFilter.addFileType(".glf");

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
		if (changeType == PamController.PROJECT_META_UPDATE) {
			intlMetaDataUpdate();
		}
	}

	/**
	 * Meta data in this batch processor config has been changed. Should this
	 * be pushed to the external configuration ? 
	 */
	private void intlMetaDataUpdate() {
		PamguardMetaData metaData = MetaDataContol.getMetaDataControl().getMetaData();
		if (metaData == null) {
			return; // I don't this this is possible, but just in case. 
		}
		if (isPSFXOpen()) {
			WarnOnce.showWarning("Project metadata", 
					"Meta data cannot be pushed to external configuration because the external contriguration is currently open", WarnOnce.WARNING_MESSAGE);
			return;
		}
		// otherwise push these changes to the external congig. 
		int ans = WarnOnce.showWarning("Project metadata", 
				"Do you want to push your changes to the project metadata to the external configuration?", WarnOnce.YES_NO_OPTION);
		if (ans == WarnOnce.OK_OPTION) {
			// push the internal meta to external. 
			externalConfiguration.pushMetaData(metaData);
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
	public List<String> findStartExecutable() {

		// first look to see if we're running from the IDE and if so, also launch
		// new configs from the ide
		if (isBuildEnvironment()) {
			List<String> ideStr = eclipseLaunchLine();
			if (ideStr != null) {
				return ideStr;
			}
			else {
				List<String> arg = new ArrayList();
				arg.add("C:\\Program Files\\Pamguard\\Pamguard.exe");
				return arg;
			}
		}

		// first look in the current directory and see if it's there. 
		List<String> arg = new ArrayList();
		String userDir = System.getProperty("user.dir");
		if (userDir != null) {
			File userFile = new File(userDir+File.separator+"Pamguard.exe");
			if (userFile.exists()) {
				arg.add(userFile.getAbsolutePath());
				return arg;
			}
			System.out.println("Unable to find PAMGuard executable in user.dir: " + userFile.getAbsolutePath());
		}
		// try the default location. 
		File tryDir = new File(DEFAULTWINDOWSEXE);
		if (tryDir.exists()) {
			arg.add(tryDir.getAbsolutePath());
			return arg;
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
	private List<String> eclipseLaunchLine() {
		ArrayList<String> args = new ArrayList();
		args.add("C:\\Program Files\\Java\\ojdk-21.0.1\\bin\\java.exe");
		args.add("-Xmx2g");
		args.add("-Djava.library.path=lib64");
		//		args.add("-classpath \"C:\\Users\\dg50\\source\\repos\\PAMGuardPAMGuard\\target\\classes;C:\\Users\\dg50\\source\\repos\\PAMGuardPAMGuard\\target\\classes;C:\\Users\\dg50\\.m2\\repository\\io\\github\\macster110\\jpamutils\\0.0.59e\\jpamutils-0.0.59e.jar;C:\\Users\\dg50\\.m2\\repository\\uk\\me\\berndporr\\iirj\\1.7\\iirj-1.7.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\wendykierp\\JTransforms\\3.1\\JTransforms-3.1-with-dependencies.jar;C:\\Users\\dg50\\.m2\\repository\\pl\\edu\\icm\\JLargeArrays\\1.5\\JLargeArrays-1.5.jar;C:\\Users\\dg50\\.m2\\repository\\us\\hebi\\matlab\\mat\\mfl-core\\0.5.6\\mfl-core-0.5.6.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\psambit9791\\jdsp\\2.0.1\\jdsp-2.0.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\knowm\\xchart\\xchart\\3.8.1\\xchart-3.8.1.jar;C:\\Users\\dg50\\.m2\\repository\\de\\erichseifert\\vectorgraphics2d\\VectorGraphics2D\\0.13\\VectorGraphics2D-0.13.jar;C:\\Users\\dg50\\.m2\\repository\\de\\rototor\\pdfbox\\graphics2d\\0.32\\graphics2d-0.32.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\pdfbox\\pdfbox\\2.0.24\\pdfbox-2.0.24.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\pdfbox\\fontbox\\2.0.24\\fontbox-2.0.24.jar;C:\\Users\\dg50\\.m2\\repository\\com\\madgag\\animated-gif-lib\\1.4\\animated-gif-lib-1.4.jar;C:\\Users\\dg50\\.m2\\repository\\ca\\umontreal\\iro\\simul\\ssj\\3.3.1\\ssj-3.3.1.jar;C:\\Users\\dg50\\.m2\\repository\\jfree\\jfreechart\\1.0.12\\jfreechart-1.0.12.jar;C:\\Users\\dg50\\.m2\\repository\\jfree\\jcommon\\1.0.15\\jcommon-1.0.15.jar;C:\\Users\\dg50\\.m2\\repository\\colt\\colt\\1.2.0\\colt-1.2.0.jar;C:\\Users\\dg50\\.m2\\repository\\concurrent\\concurrent\\1.3.4\\concurrent-1.3.4.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\rwl\\optimization\\1.3\\optimization-1.3.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\psambit9791\\wavfile\\0.1\\wavfile-0.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\junit\\platform\\junit-platform-surefire-provider\\1.0.0\\junit-platform-surefire-provider-1.0.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\junit\\platform\\junit-platform-launcher\\1.11.3\\junit-platform-launcher-1.11.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\maven\\surefire\\surefire-api\\2.19.1\\surefire-api-2.19.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\maven\\surefire\\common-java5\\2.19.1\\common-java5-2.19.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\json\\json\\20201115\\json-20201115.jar;C:\\Users\\dg50\\.m2\\repository\\io\\github\\macster110\\jdl4pam\\0.0.99e\\jdl4pam-0.0.99e.jar;C:\\Users\\dg50\\.m2\\repository\\ai\\djl\\api\\0.30.0\\api-0.30.0.jar;C:\\Users\\dg50\\.m2\\repository\\com\\google\\code\\gson\\gson\\2.11.0\\gson-2.11.0.jar;C:\\Users\\dg50\\.m2\\repository\\ai\\djl\\pytorch\\pytorch-engine\\0.30.0\\pytorch-engine-0.30.0.jar;C:\\Users\\dg50\\.m2\\repository\\ai\\djl\\tensorflow\\tensorflow-engine\\0.30.0\\tensorflow-engine-0.30.0.jar;C:\\Users\\dg50\\.m2\\repository\\ai\\djl\\tensorflow\\tensorflow-api\\0.30.0\\tensorflow-api-0.30.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\bytedeco\\javacpp\\1.5.10\\javacpp-1.5.10.jar;C:\\Users\\dg50\\.m2\\repository\\ai\\djl\\tflite\\tflite-engine\\0.26.0\\tflite-engine-0.26.0.jar;C:\\Users\\dg50\\.m2\\repository\\gov\\nist\\math\\jama\\1.0.3\\jama-1.0.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-controls\\21\\javafx-controls-21.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-controls\\21\\javafx-controls-21-win.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-graphics\\21\\javafx-graphics-21.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-graphics\\21\\javafx-graphics-21-win.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-base\\21\\javafx-base-21.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-base\\21\\javafx-base-21-win.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-swing\\21\\javafx-swing-21.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-swing\\21\\javafx-swing-21-win.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-media\\21\\javafx-media-21.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-media\\21\\javafx-media-21-win.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-web\\21\\javafx-web-21.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-web\\21\\javafx-web-21-win.jar;C:\\Users\\dg50\\.m2\\repository\\com\\formdev\\flatlaf\\3.5.1\\flatlaf-3.5.1.jar;C:\\Users\\dg50\\.m2\\repository\\net\\synedra\\validatorfx\\0.4.2\\validatorfx-0.4.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-compress\\1.19\\commons-compress-1.19.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-csv\\1.7\\commons-csv-1.7.jar;C:\\Users\\dg50\\.m2\\repository\\commons-io\\commons-io\\2.6\\commons-io-2.6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-lang3\\3.9\\commons-lang3-3.9.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-math3\\3.6.1\\commons-math3-3.6.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-math\\2.2\\commons-math-2.2.jar;C:\\Users\\dg50\\.m2\\repository\\commons-net\\commons-net\\3.6\\commons-net-3.6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\controlsfx\\controlsfx\\11.2.0\\controlsfx-11.2.0.jar;C:\\Users\\dg50\\.m2\\repository\\io\\github\\furstenheim\\copy_down\\1.1\\copy_down-1.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jsoup\\jsoup\\1.15.2\\jsoup-1.15.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\kordamp\\ikonli\\ikonli-javafx\\12.3.1\\ikonli-javafx-12.3.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\kordamp\\ikonli\\ikonli-core\\12.3.1\\ikonli-core-12.3.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\kordamp\\ikonli\\ikonli-swing\\12.3.1\\ikonli-swing-12.3.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\kordamp\\ikonli\\ikonli-materialdesign2-pack\\12.3.1\\ikonli-materialdesign2-pack-12.3.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\kordamp\\ikonli\\ikonli-fileicons-pack\\12.3.1\\ikonli-fileicons-pack-12.3.1.jar;C:\\Users\\dg50\\.m2\\repository\\net\\sf\\geographiclib\\GeographicLib-Java\\1.50\\GeographicLib-Java-1.50.jar;C:\\Users\\dg50\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-databind\\2.10.1\\jackson-databind-2.10.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-annotations\\2.10.1\\jackson-annotations-2.10.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\fasterxml\\jackson\\core\\jackson-core\\2.10.1\\jackson-core-2.10.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jflac\\jflac-codec\\1.5.2\\jflac-codec-1.5.2.jar;C:\\Users\\dg50\\.m2\\repository\\javax\\help\\javahelp\\2.0.05\\javahelp-2.0.05.jar;C:\\Users\\dg50\\.m2\\repository\\net\\java\\dev\\jna\\jna\\5.5.0\\jna-5.5.0.jar;C:\\Users\\dg50\\.m2\\repository\\net\\java\\dev\\jna\\jna-platform\\5.5.0\\jna-platform-5.5.0.jar;C:\\Users\\dg50\\.m2\\repository\\com\\jcraft\\jsch\\0.1.55\\jsch-0.1.55.jar;C:\\Users\\dg50\\.m2\\repository\\com\\fazecast\\jSerialComm\\2.11.0\\jSerialComm-2.11.0.jar;C:\\Users\\dg50\\.m2\\repository\\edu\\emory\\mathcs\\JTransforms\\2.4\\JTransforms-2.4.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\mail\\javax.mail\\1.6.2\\javax.mail-1.6.2.jar;C:\\Users\\dg50\\.m2\\repository\\com\\drewnoakes\\metadata-extractor\\2.12.0\\metadata-extractor-2.12.0.jar;C:\\Users\\dg50\\.m2\\repository\\com\\adobe\\xmp\\xmpcore\\6.0.6\\xmpcore-6.0.6.jar;C:\\Users\\dg50\\.m2\\repository\\mysql\\mysql-connector-java\\8.0.18\\mysql-connector-java-8.0.18.jar;C:\\Users\\dg50\\.m2\\repository\\com\\google\\protobuf\\protobuf-java\\3.17.0\\protobuf-java-3.17.0.jar;C:\\Users\\dg50\\.m2\\repository\\edu\\ucar\\netcdfAll\\4.6.14\\netcdfAll-4.6.14.jar;C:\\Users\\dg50\\.m2\\repository\\com\\opencsv\\opencsv\\5.0\\opencsv-5.0.jar;C:\\Users\\dg50\\.m2\\repository\\commons-beanutils\\commons-beanutils\\1.9.4\\commons-beanutils-1.9.4.jar;C:\\Users\\dg50\\.m2\\repository\\commons-logging\\commons-logging\\1.2\\commons-logging-1.2.jar;C:\\Users\\dg50\\.m2\\repository\\commons-collections\\commons-collections\\3.2.2\\commons-collections-3.2.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-collections4\\4.4\\commons-collections4-4.4.jar;C:\\Users\\dg50\\.m2\\repository\\org\\postgresql\\postgresql\\42.2.24\\postgresql-42.2.24.jar;C:\\Users\\dg50\\.m2\\repository\\org\\checkerframework\\checker-qual\\3.5.0\\checker-qual-3.5.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-script-engine\\0.9.2725\\renjin-script-engine-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-core\\0.9.2725\\renjin-core-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-appl\\0.9.2725\\renjin-appl-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-blas\\0.9.2725\\renjin-blas-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-nmath\\0.9.2725\\renjin-nmath-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-math-common\\0.9.2725\\renjin-math-common-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-lapack\\0.9.2725\\renjin-lapack-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\gcc-runtime\\0.9.2725\\gcc-runtime-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\core\\1.1.2\\core-1.1.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-vfs2\\2.0\\commons-vfs2-2.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\maven\\scm\\maven-scm-api\\1.4\\maven-scm-api-1.4.jar;C:\\Users\\dg50\\.m2\\repository\\org\\codehaus\\plexus\\plexus-utils\\1.5.6\\plexus-utils-1.5.6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\maven\\scm\\maven-scm-provider-svnexe\\1.4\\maven-scm-provider-svnexe-1.4.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\maven\\scm\\maven-scm-provider-svn-commons\\1.4\\maven-scm-provider-svn-commons-1.4.jar;C:\\Users\\dg50\\.m2\\repository\\regexp\\regexp\\1.3\\regexp-1.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\tukaani\\xz\\1.8\\xz-1.8.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-asm\\5.0.4b\\renjin-asm-5.0.4b.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-guava\\17.0b\\renjin-guava-17.0b.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\codemodel\\codemodel\\2.6\\codemodel-2.6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\stats\\0.9.2725\\stats-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\renjin-gnur-runtime\\0.9.2725\\renjin-gnur-runtime-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\methods\\0.9.2725\\methods-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\tools\\0.9.2725\\tools-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\datasets\\0.9.2725\\datasets-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\utils\\0.9.2725\\utils-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\grDevices\\0.9.2725\\grDevices-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jfree\\jfreesvg\\3.3\\jfreesvg-3.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\graphics\\0.9.2725\\graphics-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\org\\renjin\\compiler\\0.9.2725\\compiler-0.9.2725.jar;C:\\Users\\dg50\\.m2\\repository\\net\\sourceforge\\f2j\\arpack_combined_all\\0.1\\arpack_combined_all-0.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_ref-osx-x86_64\\1.1\\netlib-native_ref-osx-x86_64-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\native_ref-java\\1.1\\native_ref-java-1.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\jniloader\\1.1\\jniloader-1.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_ref-linux-x86_64\\1.1\\netlib-native_ref-linux-x86_64-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_ref-linux-i686\\1.1\\netlib-native_ref-linux-i686-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_ref-win-x86_64\\1.1\\netlib-native_ref-win-x86_64-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_ref-win-i686\\1.1\\netlib-native_ref-win-i686-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_ref-linux-armhf\\1.1\\netlib-native_ref-linux-armhf-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_system-osx-x86_64\\1.1\\netlib-native_system-osx-x86_64-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\native_system-java\\1.1\\native_system-java-1.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_system-linux-x86_64\\1.1\\netlib-native_system-linux-x86_64-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_system-linux-i686\\1.1\\netlib-native_system-linux-i686-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_system-linux-armhf\\1.1\\netlib-native_system-linux-armhf-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_system-win-x86_64\\1.1\\netlib-native_system-win-x86_64-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\com\\github\\fommil\\netlib\\netlib-native_system-win-i686\\1.1\\netlib-native_system-win-i686-1.1-natives.jar;C:\\Users\\dg50\\.m2\\repository\\org\\slf4j\\slf4j-api\\1.8.0-beta4\\slf4j-api-1.8.0-beta4.jar;C:\\Users\\dg50\\.m2\\repository\\org\\slf4j\\slf4j-nop\\1.8.0-beta4\\slf4j-nop-1.8.0-beta4.jar;C:\\Users\\dg50\\.m2\\repository\\org\\docx4j\\docx4j-JAXB-ReferenceImpl\\11.1.3\\docx4j-JAXB-ReferenceImpl-11.1.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\docx4j\\docx4j-core\\11.1.3\\docx4j-core-11.1.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\docx4j\\docx4j-openxml-objects\\11.1.3\\docx4j-openxml-objects-11.1.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\docx4j\\docx4j-openxml-objects-pml\\11.1.3\\docx4j-openxml-objects-pml-11.1.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\docx4j\\docx4j-openxml-objects-sml\\11.1.3\\docx4j-openxml-objects-sml-11.1.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\plutext\\jaxb-svg11\\1.0.2\\jaxb-svg11-1.0.2.jar;C:\\Users\\dg50\\.m2\\repository\\net\\engio\\mbassador\\1.3.2\\mbassador-1.3.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\slf4j\\jcl-over-slf4j\\1.7.26\\jcl-over-slf4j-1.7.26.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\httpcomponents\\httpclient\\4.5.8\\httpclient-4.5.8.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\httpcomponents\\httpcore\\4.4.11\\httpcore-4.4.11.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\xmlgraphics\\xmlgraphics-commons\\2.3\\xmlgraphics-commons-2.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\docx4j\\org\\apache\\xalan-interpretive\\11.0.0\\xalan-interpretive-11.0.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\docx4j\\org\\apache\\xalan-serializer\\11.0.0\\xalan-serializer-11.0.0.jar;C:\\Users\\dg50\\.m2\\repository\\net\\arnx\\wmf2svg\\0.9.8\\wmf2svg-0.9.8.jar;C:\\Users\\dg50\\.m2\\repository\\org\\antlr\\antlr-runtime\\3.5.2\\antlr-runtime-3.5.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\antlr\\stringtemplate\\3.2.1\\stringtemplate-3.2.1.jar;C:\\Users\\dg50\\.m2\\repository\\antlr\\antlr\\2.7.7\\antlr-2.7.7.jar;C:\\Users\\dg50\\.m2\\repository\\com\\google\\errorprone\\error_prone_annotations\\2.3.3\\error_prone_annotations-2.3.3.jar;C:\\Users\\dg50\\.m2\\repository\\jakarta\\xml\\bind\\jakarta.xml.bind-api\\2.3.2\\jakarta.xml.bind-api-2.3.2.jar;C:\\Users\\dg50\\.m2\\repository\\jakarta\\activation\\jakarta.activation-api\\1.2.1\\jakarta.activation-api-1.2.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\xerial\\sqlite-jdbc\\3.45.3.0\\sqlite-jdbc-3.45.3.0.jar;C:\\Users\\dg50\\.m2\\repository\\net\\sf\\ucanaccess\\ucanaccess\\4.0.4\\ucanaccess-4.0.4.jar;C:\\Users\\dg50\\.m2\\repository\\org\\hsqldb\\hsqldb\\2.3.1\\hsqldb-2.3.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\healthmarketscience\\jackcess\\jackcess\\2.1.11\\jackcess-2.1.11.jar;C:\\Users\\dg50\\.m2\\repository\\commons-lang\\commons-lang\\2.6\\commons-lang-2.6.jar;C:\\Users\\dg50\\.m2\\repository\\nz\\ac\\waikato\\cms\\weka\\weka-dev\\3.7.7\\weka-dev-3.7.7.jar;C:\\Users\\dg50\\.m2\\repository\\net\\sf\\squirrel-sql\\thirdparty-non-maven\\java-cup\\0.11a\\java-cup-0.11a.jar;C:\\Users\\dg50\\.m2\\repository\\org\\pentaho\\pentaho-commons\\pentaho-package-manager\\1.0.3\\pentaho-package-manager-1.0.3.jar;C:\\Users\\dg50\\.m2\\repository\\javax\\vecmath\\vecmath\\1.5.2\\vecmath-1.5.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\eclipse\\persistence\\org.eclipse.persistence.moxy\\2.5.0\\org.eclipse.persistence.moxy-2.5.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\eclipse\\persistence\\org.eclipse.persistence.core\\2.5.0\\org.eclipse.persistence.core-2.5.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\eclipse\\persistence\\org.eclipse.persistence.asm\\2.5.0\\org.eclipse.persistence.asm-2.5.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\eclipse\\persistence\\org.eclipse.persistence.antlr\\2.5.0\\org.eclipse.persistence.antlr-2.5.0.jar;C:\\Users\\dg50\\.m2\\repository\\javax\\xml\\bind\\jaxb-api\\2.2.11\\jaxb-api-2.2.11.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\jaxb\\jaxb-runtime\\2.4.0-b180830.0438\\jaxb-runtime-2.4.0-b180830.0438.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\jaxb\\txw2\\2.4.0-b180830.0438\\txw2-2.4.0-b180830.0438.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\istack\\istack-commons-runtime\\3.0.7\\istack-commons-runtime-3.0.7.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jvnet\\staxex\\stax-ex\\1.8\\stax-ex-1.8.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\xml\\fastinfoset\\FastInfoset\\1.2.15\\FastInfoset-1.2.15.jar;C:\\Users\\dg50\\.m2\\repository\\javax\\activation\\javax.activation-api\\1.2.0\\javax.activation-api-1.2.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\jaxb\\jaxb-xjc\\2.4.0-b180830.0438\\jaxb-xjc-2.4.0-b180830.0438.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\jaxb\\xsom\\2.4.0-b180830.0438\\xsom-2.4.0-b180830.0438.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\jaxb\\codemodel\\2.4.0-b180830.0438\\codemodel-2.4.0-b180830.0438.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\xml\\bind\\external\\rngom\\2.4.0-b180830.0438\\rngom-2.4.0-b180830.0438.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\xml\\dtd-parser\\dtd-parser\\1.4\\dtd-parser-1.4.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\istack\\istack-commons-tools\\3.0.7\\istack-commons-tools-3.0.7.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\ant\\ant\\1.10.2\\ant-1.10.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\ant\\ant-launcher\\1.10.2\\ant-launcher-1.10.2.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\xml\\bind\\external\\relaxng-datatype\\2.4.0-b180830.0438\\relaxng-datatype-2.4.0-b180830.0438.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\jersey\\contribs\\jersey-multipart\\1.18.1\\jersey-multipart-1.18.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\jvnet\\mimepull\\mimepull\\1.9.3\\mimepull-1.9.3.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\jersey\\jersey-core\\1.18.1\\jersey-core-1.18.1.jar;C:\\Users\\dg50\\.m2\\repository\\commons-cli\\commons-cli\\1.2\\commons-cli-1.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\poi\\poi\\3.10-beta1\\poi-3.10-beta1.jar;C:\\Users\\dg50\\.m2\\repository\\commons-codec\\commons-codec\\1.5\\commons-codec-1.5.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\jersey\\jersey-client\\1.18.1\\jersey-client-1.18.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\jersey\\contribs\\jersey-apache-client\\1.18.1\\jersey-apache-client-1.18.1.jar;C:\\Users\\dg50\\.m2\\repository\\commons-httpclient\\commons-httpclient\\3.1\\commons-httpclient-3.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\miglayout\\miglayout\\3.7.4\\miglayout-3.7.4.jar;C:\\Users\\dg50\\.m2\\repository\\ca\\juliusdavies\\not-yet-commons-ssl\\0.3.11\\not-yet-commons-ssl-0.3.11.jar;C:\\Users\\dg50\\.m2\\repository\\javax\\ws\\rs\\javax.ws.rs-api\\2.1.1\\javax.ws.rs-api-2.1.1.jar;C:\\Users\\dg50\\.m2\\repository\\com\\sun\\xml\\bind\\jaxb-impl\\2.2.11\\jaxb-impl-2.2.11.jar;C:\\Users\\dg50\\.m2\\repository\\javax\\activation\\activation\\1.1\\activation-1.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\jaxb\\jaxb-core\\2.2.11\\jaxb-core-2.2.11.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\jersey\\core\\jersey-common\\2.2\\jersey-common-2.2.jar;C:\\Users\\dg50\\.m2\\repository\\javax\\annotation\\javax.annotation-api\\1.2\\javax.annotation-api-1.2.jar;C:\\Users\\dg50\\.m2\\repository\\com\\google\\guava\\guava\\14.0.1\\guava-14.0.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\hk2\\hk2-api\\2.2.0-b14\\hk2-api-2.2.0-b14.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\hk2\\hk2-utils\\2.2.0-b14\\hk2-utils-2.2.0-b14.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\hk2\\external\\javax.inject\\2.2.0-b14\\javax.inject-2.2.0-b14.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\hk2\\hk2-locator\\2.2.0-b14\\hk2-locator-2.2.0-b14.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\hk2\\external\\asm-all-repackaged\\2.2.0-b14\\asm-all-repackaged-2.2.0-b14.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\hk2\\external\\cglib\\2.2.0-b14\\cglib-2.2.0-b14.jar;C:\\Users\\dg50\\.m2\\repository\\org\\glassfish\\hk2\\osgi-resource-locator\\1.0.1\\osgi-resource-locator-1.0.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apache\\commons\\commons-text\\1.9\\commons-text-1.9.jar;C:\\Users\\dg50\\.m2\\repository\\tethys\\org\\nilus\\3.1\\nilus-3.1.jar;C:\\Users\\dg50\\.m2\\repository\\tethys\\org\\javaclient\\3.0\\javaclient-3.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\pamguard\\x3\\2.2.8\\x3-2.2.8.jar;C:\\Users\\dg50\\.m2\\repository\\it\\sauronsoftware\\jave\\1.0.2\\jave-1.0.2.jar;C:\\Users\\dg50\\.m2\\repository\\com\\synthbot\\jasiohost\\1.0.0\\jasiohost-1.0.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\springframework\\spring-core\\5.2.3.RELEASE\\spring-core-5.2.3.RELEASE.jar;C:\\Users\\dg50\\.m2\\repository\\org\\springframework\\spring-jcl\\5.2.3.RELEASE\\spring-jcl-5.2.3.RELEASE.jar;C:\\Users\\dg50\\.m2\\repository\\com\\1stleg\\jnativehook\\2.1.0\\jnativehook-2.1.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\swinglabs\\swingx\\swingx-all\\1.6.5-1\\swingx-all-1.6.5-1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\fxyz3d\\fxyz3d\\0.6.0\\fxyz3d-0.6.0.jar;C:\\Users\\dg50\\.m2\\repository\\eu\\mihosoft\\vrl\\jcsg\\jcsg\\0.5.7\\jcsg-0.5.7.jar;C:\\Users\\dg50\\.m2\\repository\\eu\\mihosoft\\vvecmath\\vvecmath\\0.4.0\\vvecmath-0.4.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\orbisgis\\poly2tri-core\\0.1.2\\poly2tri-core-0.1.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-fxml\\17.0.2\\javafx-fxml-17.0.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\openjfx\\javafx-fxml\\17.0.2\\javafx-fxml-17.0.2-win.jar;C:\\Users\\dg50\\.m2\\repository\\io\\github\\mkpaz\\atlantafx-base\\2.0.1\\atlantafx-base-2.0.1.jar;C:\\Users\\dg50\\.m2\\repository\\javax\\servlet\\javax.servlet-api\\4.0.1\\javax.servlet-api-4.0.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\mockito\\mockito-all\\1.10.19\\mockito-all-1.10.19.jar;C:\\Users\\dg50\\.m2\\repository\\org\\powermock\\powermock-module-junit4\\1.6.6\\powermock-module-junit4-1.6.6.jar;C:\\Users\\dg50\\.m2\\repository\\junit\\junit\\4.12\\junit-4.12.jar;C:\\Users\\dg50\\.m2\\repository\\org\\powermock\\powermock-module-junit4-common\\1.6.6\\powermock-module-junit4-common-1.6.6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\powermock\\powermock-core\\1.6.6\\powermock-core-1.6.6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\javassist\\javassist\\3.21.0-GA\\javassist-3.21.0-GA.jar;C:\\Users\\dg50\\.m2\\repository\\org\\powermock\\powermock-reflect\\1.6.6\\powermock-reflect-1.6.6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\powermock\\powermock-api-mockito\\1.6.6\\powermock-api-mockito-1.6.6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\mockito\\mockito-core\\1.10.19\\mockito-core-1.10.19.jar;C:\\Users\\dg50\\.m2\\repository\\org\\objenesis\\objenesis\\2.1\\objenesis-2.1.jar;C:\\Users\\dg50\\.m2\\repository\\org\\hamcrest\\hamcrest-core\\1.3\\hamcrest-core-1.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\powermock\\powermock-api-mockito-common\\1.6.6\\powermock-api-mockito-common-1.6.6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\powermock\\powermock-api-support\\1.6.6\\powermock-api-support-1.6.6.jar;C:\\Users\\dg50\\.m2\\repository\\org\\junit\\jupiter\\junit-jupiter\\5.11.3\\junit-jupiter-5.11.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\junit\\jupiter\\junit-jupiter-api\\5.11.3\\junit-jupiter-api-5.11.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\opentest4j\\opentest4j\\1.3.0\\opentest4j-1.3.0.jar;C:\\Users\\dg50\\.m2\\repository\\org\\junit\\platform\\junit-platform-commons\\1.11.3\\junit-platform-commons-1.11.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\apiguardian\\apiguardian-api\\1.1.2\\apiguardian-api-1.1.2.jar;C:\\Users\\dg50\\.m2\\repository\\org\\junit\\jupiter\\junit-jupiter-params\\5.11.3\\junit-jupiter-params-5.11.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\junit\\jupiter\\junit-jupiter-engine\\5.11.3\\junit-jupiter-engine-5.11.3.jar;C:\\Users\\dg50\\.m2\\repository\\org\\junit\\platform\\junit-platform-engine\\1.11.3\\junit-platform-engine-1.11.3.jar\"");
		args.add("-jar C:\\Users\\dg50\\source\\repos\\PAMGuardPAMGuard\\target\\Pamguard-2.02.15.jar");
		args.add("pamguard.Pamguard");
		args.add("-smru");
		args.add("-nosplash");
		args.add("-smrudev");
		//		args.add(ell);
		args = null;
		return args;

		//		
		//		Optional<String> cl = ProcessHandle.current().info().commandLine();
		//		
		//		RuntimeMXBean rtBean = ManagementFactory.getRuntimeMXBean();
		//		String libPath = rtBean.getLibraryPath();
		//		Class<? extends RuntimeMXBean> classPath = rtBean.getClass();
		//		
		////		String bootClassPath = rtBean.getBootClassPath();
		//		List<String> inArgs = rtBean.getInputArguments();
		//		return inArgs;
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
	 * open PAMGuard in a new program instance to view and modify the configuration.
	 * If doing viewer stuff it opens in a slightly 'funny' mode to make sure that
	 * viewer tasks are loaded. 
	 * @param psfxFile psfx file
	 * @param withComms with comms for external control. false for now, will probably eventually be a reference to a job set. 
	 */
	public void launchPamguard(String psfxFile, boolean withComms) {
		List<String> pgExe = findStartExecutable();
		if (pgExe == null) {
			pgExe = new ArrayList<>();
			pgExe.add("C:\\Program Files\\Pamguard\\Pamguard.exe");
		}

		final ArrayList<String> command = new ArrayList<String>();
		command.addAll(pgExe);
		command.add("-psf");
		command.add(psfxFile);
		if (batchParameters.getBatchMode() == BatchMode.VIEWER) {
			command.add(GlobalArguments.BATCHVIEW); // batchview mode. Will load the psfx, but then pretend it's in viewer mode.  
		}
		File pFile = new File(psfxFile);
		long pModified = 0;
		try {
			pModified = pFile.lastModified();
		}
		catch (Exception e) {
			System.err.println(e.getMessage());
		}

		final ProcessBuilder builder = new ProcessBuilder(command);
		Process process = null;
		try {
			process = builder.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// monitor the process and the psfx file, being prepared to update the internal config if the file changes. 
		Thread t = new Thread(psfxMonitor = new PSFXMonitor(process, pFile, pModified));
		t.start();

	}
	
	/**
	 * Check a database name. In normal batch mode, these can 
	 * be without their .sqlite ending, so don't exist. This causes
	 * trouble when you switch to Viewer mode<p>
	 * Not creating, but looking to see if we can find something. 
	 * @param startName name which may not have  afile end. 
	 * @return name with file end if found, otherwis original. 
	 */
	public String checkDatabasePath(String startPath) {
		File aFile = new File(startPath);
		if (aFile.exists()) {
			return startPath;
		}
		String[] possEnds = {".sqlite3"};
		for (int i = 0; i < possEnds.length; i++) {
			File newPath = PamFileFilter.checkFileEnd(aFile, possEnds[i], true);
			if (newPath.exists()) {
				return newPath.getAbsolutePath();
			}
		}
		return startPath;
	}

	public void launchViewer(String outputDatabaseName) {
		List<String> pgExe = findStartExecutable();
		if (pgExe == null) {
			pgExe = new ArrayList<>();
			pgExe.add("C:\\Program Files\\Pamguard\\Pamguard.exe");
		}
		String path = checkDatabasePath(outputDatabaseName);
		File dbFile = new File(path);
		if (dbFile.exists() == false) {
			WarnOnce.showWarning("Can't launch PAMGuard viewer", "The database " + path + " does not exist", WarnOnce.WARNING_MESSAGE);
			return;
		}

		final ArrayList<String> command = new ArrayList<String>();
		command.addAll(pgExe);
		command.add("-v");
		command.add(DBControl.GlobalDatabaseNameArg);
		command.add(path);
//		if (batchParameters.getBatchMode() == BatchMode.VIEWER) {
//			command.add(GlobalArguments.BATCHVIEW); // batchview mode. Will load the psfx, but then pretend it's in viewer mode.  
//		}

		final ProcessBuilder builder = new ProcessBuilder(command);
		Process process = null;
		try {
			process = builder.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Try to work out whether or not the sample psfx is open, so might 
	 * be about to rewrite the psfx file. 
	 * @return true if it's been opened from within Batch Processor. Can't 
	 * do much if it was opened independently. 
	 */
	private boolean isPSFXOpen() {
		if (psfxMonitor == null) {
			return false;
		}
		try {
			return psfxMonitor.process.isAlive();
		}
		catch (Exception e) {
			return false;
		}
	}
	/**
	 * Monitor the psfx file and update internal settings for the remote config if it
	 * changes. This is particularly important with offline tasks, since their configuration
	 * is very dependent on the internal copy of the external configuration. 
	 * @author dg50
	 *
	 */
	private class PSFXMonitor implements Runnable {

		private File pFile;
		private long lastModified;
		private Process process;

		public PSFXMonitor(Process process, File pFile, long lastModified) {
			super();
			this.process = process;
			this.pFile = pFile;
			this.lastModified = lastModified;
		}

		@Override
		public void run() {
			// while the external copy of PAMGuard is running. 
			// note that if the psfx was loaded independenlty, then changes may not 
			// get picked up. 
			while (process.isAlive()) {
				try {
					Thread.sleep(1000);
					checkModified();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			/*
			 *  call one last time just incase through some freak of timing the last
			 *  update to settings as process exited didn't come while it was alive.  
			 *  Probably not possible, but ...
			 */
			checkModified();
		}

		/**
		 * See if the psfx file has been modified and if it has, reload it's
		 * configuration. This will always occurr when the external pamguard
		 * exists, and may occurr if user does a save config in the external config. 
		 */
		private void checkModified() {
			long modified = pFile.lastModified();
			if (modified != lastModified) {
				// need to reload all the modules from that psfx. 
				settingsChange(SettingsObservers.CHANGE_CONFIG);
				lastModified = modified;
			}			
		}

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
		List<String> pgExe = findStartExecutable();
		if (pgExe == null) {
			return null;
		}
		ArrayList<String> command = new ArrayList<>();
		command.add("-v"); // viewer mode
		command.add(GlobalArguments.BATCHFLAG);
		command.add(DBControl.GlobalDatabaseNameArg); // viewer database. 
		command.add(checkDatabasePath(jobInfo.outputDatabaseName));
		// and the psf since it will need to take the settings from it to override what's in the database. 
//		command.add("-psf");
//		String psf = batchParameters.getMasterPSFX();
//		command.add(psf);

		if (jobInfo.soundFileFolder != null && jobInfo.soundFileFolder.length() > 0) {
//			command.add(psf);
			command.add(FolderInputSystem.GlobalWavFolderArg);
			command.add(jobInfo.soundFileFolder);
		}
		if (jobInfo.outputBinaryFolder != null && jobInfo.outputBinaryFolder.length() > 0) {
			command.add(BinaryStore.GlobalFolderArg);
			command.add(jobInfo.outputBinaryFolder);
		}

		if (batchParameters.isNoGUI()) {
			command.add("-nogui");
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

		command.add(PamController.AUTOSTART);

		/*
		 * And add offline tasks to the list of commands. 
		 */
		ArrayList<OfflineTaskDataUnit> allTasks = externalConfiguration.getTaskDataBlock().getDataCopy();
		for (OfflineTaskDataUnit aTaskUnit : allTasks) {
			TaskSelection sel = batchParameters.getTaskSelection(aTaskUnit.getOfflineTask());
			if (sel.selected == false) {
				continue;
			}
			command.add(OfflineTaskManager.commandFlag);
			command.add(aTaskUnit.getOfflineTask().getLongName());
		}

		/**
		 * Command with a couple of different jobs set up may end up something like:
		 * [-v, -batch, -databasefile, C:\PAMGuardTest\Batch Procesing\ClickReprocess\STMORLAIS_WP1a_Dep1_00database.sqlite3, 
		 * -psf, C:\PAMGuardTest\Batch Procesing\Morlais3_14.psfx, -binaryfolder, 
		 * C:\PAMGuardTest\Batch Procesing\ClickReprocess\STMORLAIS_WP1a_DepSample00Binary, 
		 * -multicast, 230.1.1.1, 12346, -netSend.id1, 2, -netSend.id2, 2874, 
		 * -offlinetask, SoundTrap Click Detector:ST Click Detector:Reclassify Clicks, 
		 * -offlinetask, Click Detector:Minke Detector:Reclassify Clicks, 
		 * -offlinetask, Click Detector:Minke Detector:Echo Detection]
		 * 
		 * Note the three offline tasks, two associated with Minke Detector and one with click detector. Need to parse these up into 
		 * some kink of task manager that will know it has two groups to run and the groups have 1 and 2 tasks respectively. 
		 */


		return command;
	}

	/**
	 * Command line options for normal mode operations, i.e. processing a ton of 
	 * sound files with a single standard psfx file. 
	 * @param nextJob
	 * @return
	 */
	public ArrayList<String> getNormalJobLaunchParams(BatchDataUnit nextJob) {

		String psf = makeJobPSFX(nextJob);

		BatchJobInfo jobInfo = nextJob.getBatchJobInfo();
		List<String> pgExe = findStartExecutable();
		if (pgExe == null) {
			return null;
		}
		ArrayList<String> command = new ArrayList<>();
		//		command.add(pgExe);
		command.add(GlobalArguments.BATCHFLAG);
		command.add("-psf");
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
		command.add(PamController.AUTOSTART);
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
	 * Make a job specific psfx. This will just be a copy of the current
	 * master unless the array is being changed, in which case that at least
	 * will be updated. 
	 * @param nextJob
	 */
	private String makeJobPSFX(BatchDataUnit nextJob) {
		String jobPSFX = getJobPSFXFileName(nextJob);
		PamSettingsGroup settings = externalConfiguration.updateJobSettings(nextJob);
		PSFXReadWriter.getInstance().writePSFX(jobPSFX, settings);
		return jobPSFX;
	}

	/**
	 * Get a name for what will be a temporary psfx file that will get modified configuration
	 * settings for the launch of each job. This is easier than trying to change settings once the
	 * job is launched. 
	 * @param batchData
	 * @return modified job specific name. 
	 */
	private String getJobPSFXFileName(BatchDataUnit batchData) {
		String psfx = batchParameters.getMasterPSFX();
		if (psfx == null) {
			return null;
		}
		String lPSFX = psfx.toLowerCase();
		int endDot = lPSFX.lastIndexOf(".psfx");
		if (endDot > 0) {
			psfx = psfx.substring(0, endDot);
		}
		psfx = psfx + String.format("_Job%d", batchData.getDatabaseIndex()) + ".psfx";
		return psfx;
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
		//		boolean ok = OnlineJobDialog.showDialog(getGuiFrame(), this, newJobData);
		boolean ok = showJobDialog(newJobData);
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
	 * Open one of two nearly identical dialogs, the main difference
	 * being the order information is controlled in and the 
	 * behaviour of the Offline one when a new database is selected
	 * since it will try to extract binary and source data automatically
	 * @param jobData
	 * @return
	 */
	private boolean showJobDialog(BatchDataUnit jobData) {
		BatchMode batchMode = getBatchParameters().getBatchMode();
		if (batchMode == BatchMode.NORMAL) {
			return OnlineJobDialog.showDialog(getGuiFrame(), this, jobData);
		}
		else {
			return OfflineJobDialog.showDialog(getGuiFrame(), this, jobData);
		}
	}

	/**
	 * Create a set of batch jobs based on a common folder structure. 
	 * i.e. it gets a source folder, then generates a set for each sub folder of data. 
	 */
	public void createJobSet() {
		BatchMode batchMode = getBatchParameters().getBatchMode();
		if (batchMode == BatchMode.NORMAL) {
			createNormalJobSet();
		}
		else {
			createViewerJobSet();
		}

	}

	/**
	 * Show dialog to create jobs for normal mode. Will ask for source folder and 
	 * dest folders for binary and database output
	 */
	private void createNormalJobSet() {		
		ArrayList<BatchJobInfo> jobSets = NormalSetDialog.showDialog(this.getGuiFrame(), this);
		if (jobSets == null) {
			return;
		}
		for (BatchJobInfo jobSet : jobSets) {
			BatchDataUnit newJobData = new BatchDataUnit(System.currentTimeMillis(), jobSet);
			//		batchProcess.getBatchLogging().logData(DBControlUnit.findConnection(), newJobData);
			batchProcess.getBatchDataBlock().addPamData(newJobData);
		}
		checkConflictingJobs();
	}

	/**
	 * Show dialog to create jobs for batch mode. Will ask for a folder of databases, then 
	 * extract binary and source folders from the individual databases. 
	 */
	private void createViewerJobSet() {
		ArrayList<BatchJobInfo> viewerSets = ViewerSetDialog.showDialog(this.getGuiFrame(), this); 
		if (viewerSets == null) {
			return;
		}
		for (BatchJobInfo jobSet : viewerSets) {
			BatchDataUnit newJobData = new BatchDataUnit(System.currentTimeMillis(), jobSet);
			//		batchProcess.getBatchLogging().logData(DBControlUnit.findConnection(), newJobData);
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
	public JPopupMenu getJobsPopupMenu(BatchDataUnit dataUnit) {
		return swingMenus.getJobsPopupMenu(dataUnit);
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
		//		boolean ok = OnlineJobDialog.showDialog(getGuiFrame(), this, dataUnit); 
		boolean ok = showJobDialog(dataUnit);
		if (ok) {
			batchProcess.getBatchDataBlock().updatePamData(dataUnit, System.currentTimeMillis());
			checkConflictingJobs();
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
		updateJobStatus(jobData, data);

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
	 * @param data
	 */
	private void updateJobStatus(BatchDataUnit jobData, String data) {
		//		System.out.println(data);
		String[] commandBits = data.split(",");
		BatchJobInfo jobInfo = jobData.getBatchJobInfo();
		if (commandBits[0].trim().equals(BatchStatusCommand.commandId)) {
			int nFiles = -1;
			int iFile = 0;
			int status = -1;
			if (commandBits.length < 5) {
				System.out.println("Command is too short: " + data);
				return;
			}
			try {
				nFiles = Integer.valueOf(commandBits[3]);
				iFile = Integer.valueOf(commandBits[4]);
				status = Integer.valueOf(commandBits[5]);
			}
			catch (NumberFormatException e) {
			}
			double percent = 0;
			boolean complete = iFile >= nFiles && nFiles > 0;
			BatchJobStatus jobStatus = BatchJobStatus.UNKNOWN;
			if (status == PamController.PAM_INITIALISING) {
				jobStatus = BatchJobStatus.STARTING;
				percent = 0;
			}
			//			else if (jobInfo.startSent == 0) {
			////				startProcessing(jobData);
			//				jobStatus = BatchJobStatus.RUNNING;
			//				percent = 0;
			//			}
			else if (iFile == 0) {
				jobStatus = BatchJobStatus.STARTING;
				percent = 0;
			}
			else if (iFile >= nFiles && status == PamController.PAM_IDLE) {
				jobStatus = BatchJobStatus.COMPLETE;
				percent = iFile * 100. / nFiles;
				closeJob(jobData);
			}
			else {
				jobStatus = BatchJobStatus.RUNNING;
				percent = iFile * 100. / nFiles;
			}
			//			System.out.printf("Set batch status job %d status %d file %d of %d to %s\n", 
			//					jobData.getDatabaseIndex(), status,
			//					iFile, nFiles, jobStatus.toString());
			percent = Math.min(percent, 100);
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
	 * Called once a job is set up, i.e. PAMGuard has launched, but not started. Will send a start. 
	 * @param jobData
	 */
	//	private void startProcessing(BatchDataUnit jobData) {
	//		BatchJobInfo jobInfo = jobData.getBatchJobInfo();
	//		// do some final setup. 
	//		if (jobInfo.arrayData != null && 2<1) {
	//			// get as an xml string
	//			ArrayList<PamSettings> sl = new ArrayList<>();
	//			PamControlledUnitSettings ps = new PamControlledUnitSettings(ArrayManager.arrayManagerType, ArrayManager.arrayManagerType, 
	//					ArrayParameters.class.getName(), ArrayParameters.serialVersionUID, jobInfo.arrayData);
	////			byte[] binData = ps.getNamedSerialisedByteArray();
	////			byte[] cmdData = multicastController.createBinaryCommand(jobData.getDatabaseIndex(), jobInfo.getJobId2(), SetSerializedSettingsCommand.commandId, binData);
	////			// if I try to turn that into a string ? 
	////			String asStr = new String(cmdData);
	////			String cmd = SetSerializedSettingsCommand.commandId + " " + new String(binData);
	////			SetSerializedSettingsCommand ss = new SetSerializedSettingsCommand();
	////			ss.executeBinary(cmdData);
	//			PamguardXMLWriter xmlWriter = PamguardXMLWriter.getXMLWriter();
	////			
	//			Document sampDoc = xmlWriter.writeOneModule(ArrayManager.getArrayManager(), System.currentTimeMillis());
	//			String sTxt = xmlWriter.getAsString(sampDoc);
	//			System.out.println("Sample: " + sTxt);
	//			
	//			Document doc = xmlWriter.writeSettings(ps);			
	//			String txt = xmlWriter.getAsString(doc);
	//			System.out.println("Setting array data: " + txt);
	//			String cmd = "setxmlsettings " + txt;
	//			
	//			String sentCmd = multicastController.formatBatchCommand(jobData.getDatabaseIndex(), jobInfo.getJobId2(), cmd);
	//			TerminalController tc;
	//			BatchCommand bc = new BatchCommand(tc = new TerminalController(PamController.getInstance()));
	//			tc.interpretCommand(sentCmd);
	//			
	//			SetXMLSettings ss = new SetXMLSettings();
	//			ss.execute(cmd);
	//			multicastController.targetCommand(jobData.getDatabaseIndex(), jobInfo.getJobId2(), cmd);
	//		}
	//		
	//		
	//		multicastController.targetCommand(jobData.getDatabaseIndex(), jobInfo.getJobId2(), StartCommand.commandId);
	//		jobInfo.startSent = System.currentTimeMillis();
	//	}

	/**
	 * Stop and cancel a running job. 
	 * @param dataUnit
	 */
	public void cancelJob(BatchDataUnit dataUnit) {
		closeJob(dataUnit);
		dataUnit.getBatchJobInfo().jobStatus = BatchJobStatus.CANCELLED;
		batchProcess.updateJobStatus(dataUnit);
	}

	/**
	 * Reset all jobs for reprocessing
	 */
	public void reprocessAllJobs() {
		BatchDataBlock jobs = getBatchProcess().getBatchDataBlock();
		ListIterator<BatchDataUnit> it = jobs.getListIterator(0);
		while (it.hasNext()) {
			reprocessJob(it.next());
		}
	}

	/**
	 * Reset specified job for reprocessing
	 * @param dataUnit
	 */
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

	/**
	 * When managing offline tasks, it's necessary to extract a configuration from 
	 * a representative database which will be stored as a psfx. The tasks in this 
	 * can then be modified and pushed back into each database in each task that's to 
	 * be run. 
	 * @param outputDatabaseName
	 */
	public void extractDatabaseConfiguration(String databaseName) {
		ViewerDatabase vdb = new ViewerDatabase(this, databaseName);
		vdb.extractPSFX();
	}

	/**
	 * Called from the task settings to handle updates to individual task settings
	 * through task dialogs. This cannot be done if the psfx is already open since that
	 * will override any changes as soon as it's saved. 
	 * @param offlineTask
	 */
	public boolean taskSettings(MouseEvent e, OfflineTaskDataUnit taskDataUnit) {
		OfflineTask offlineTask = taskDataUnit.getOfflineTask();
		if (offlineTask == null || offlineTask.hasSettings() == false) {
			return false; // nothing to be done anyway. 
		}
		PamControlledUnit parentModule = offlineTask.getParentControlledUnit();
		if (parentModule == null) {
			return false;
		}
		boolean psfxOpen = isPSFXOpen();
		if (psfxOpen) {
			String msg = String.format("The configuration file %s is currently open.<br>" +
					"Either make changes in the psfx file, or from the task table, but you can't do both." , batchParameters.getMasterPSFX());
			WarnOnce.showWarning("Potential onfiguration change conflict", msg, WarnOnce.WARNING_MESSAGE);
			return false;
		}
		// see if the psfx is open in a process. 
		boolean changed = offlineTask.callSettings(e.getComponent(), e.getPoint());
		if (changed) {
			/*
			 * will need to rewrite and reload the psfx file. Is this even safe to do with a half loaded
			// configuration ? The trouble is that some of the settings now only open a menu, so the 
			// changes aren't made when this returns, so really need to do this after the dialog closes. 
			// an invokelater won't do it since the menu opening and response is several awt events, so 
			// this would come far too early. Need to do something special with a special type of menu
			// or something to get settings AFTER the menu dialogs have closed.
			 * 
			 *  Probably best to just always save the task settings a) before jobs run and b) before PAMGuard exits. 
			 */
//			SwingUtilities.invokeLater(new Runnable() {
//				
//				@Override
//				public void run() {
//					System.out.println("update later is now");
//				}
//			});
			int changes = externalConfiguration.pullSettings(parentModule);
			if (changes > 0) {
				externalConfiguration.saveExtConfig();
			}
		}
		return changed;
		//	}

	}
	
	/**
	 * find which array is set for a specific job. 
	 * Will first check specific data for the job, then external confit
	 * then give up and give the batch configs array (shouldn't happen). 
	 * @param dataUnit
	 * @return
	 */
	public PamArray findJobArray(BatchDataUnit dataUnit) {
		PamArray array = dataUnit.getBatchJobInfo().arrayData;
		if (array != null) {
			return array;
		}
		if (externalConfiguration != null) {
			array = externalConfiguration.findArrayData();
			if (array != null) {
				return array;
			}
		}
		return ArrayManager.getArrayManager().getCurrentArray();
	}

	public void editJobCalibration(BatchDataUnit dataUnit) {
		PamArray array = findJobArray(dataUnit);
		if (array == null) { // this should definitely be impossible
			return;
		}
		/*
		 * Clone it, becuase if it was the default, then all jobs will
		 * be modifying the same object. 
		 * The array.clone() function should do a deep clone. 
		 */
		array = array.clone();
		// use the array dialog to make edits. 
		array = ArrayDialog.showDialog(getGuiFrame(), null, array);
		if (array != null) {
			dataUnit.getBatchJobInfo().arrayData = array;
			batchProcess.getBatchDataBlock().updatePamData(dataUnit, System.currentTimeMillis());
			batchProcess.updateJobStatus(dataUnit);
			if (batchParameters.getBatchMode() == BatchMode.VIEWER) {
				// save the change back into the viewer databse itself. 
				ViewerDatabase.rewriteArrayData(this, dataUnit.getBatchJobInfo());
			}
		}
	}

	public void deleteJobCalibration(BatchDataUnit dataUnit) {
		dataUnit.getBatchJobInfo().arrayData = null;
		batchProcess.getBatchDataBlock().updatePamData(dataUnit, System.currentTimeMillis());
		batchProcess.updateJobStatus(dataUnit);
	}

	/**
	 * Called from batchProcess.prepareProcessOK();
	 * Run checks to ensure that jobs are able to run prior to startup. 
	 * @return null if all OK, otherwise a warning message to display
	 */
	public String viewerStartChecks() {
		/*
		 *  check jobs to see that they can run. Several things to do here.
		 *  If the task is from Tethys, need to ensure that every job has a unique Array Id.  
		 */
		if (externalConfiguration == null) {
			return "No configuration for batch jobs has been set";
		}
		// pull out the tasks selected
		OfflineTaskDataBlock tasks = externalConfiguration.getTaskDataBlock();
		ArrayList<OfflineTaskDataUnit> checkedTasks = new ArrayList();
		ListIterator<OfflineTaskDataUnit> it = tasks.getListIterator(0);
		while (it.hasNext()) {
			OfflineTaskDataUnit task = it.next();
			TaskSelection taskSelection = getBatchParameters().getTaskSelection(task.getOfflineTask());
			if (taskSelection.selected) {
				checkedTasks.add(task);
			}
		}
		if (checkedTasks.size() == 0) {
			return "No offline tasks have been selected";
		}
		boolean useTethys = false;
		for (OfflineTaskDataUnit taskUnit : checkedTasks) {
			OfflineTask aTask = taskUnit.getOfflineTask();
			if (aTask.canRun() == false) {
				String whyNot = aTask.whyNot();
				if (whyNot == null) {
					whyNot = "Unknown reason";
				}
				return whyNot;
			}
			if (aTask.getParentControlledUnit() instanceof TethysControl) {
				useTethys = true;
			}
		}
		if (useTethys) {
			// check uniqueness of all array data in the list of jobs 
		}
		
		return null;
	}
	
	/**
	 * Check that the array information is provided for each job and that 
	 * the instrument id is unique for each one. 
	 * @return null if all OK, otherwise an error message. 
	 */
	public String checkJobArrays() {
		ArrayList<String> allIds = new ArrayList();
		BatchDataBlock jobsBlock = getBatchProcess().getBatchDataBlock();
		ListIterator<BatchDataUnit> it = jobsBlock.getListIterator(0);
		while (it.hasNext()) {
			BatchDataUnit jobsUnit = it.next();
			PamArray array = jobsUnit.getBatchJobInfo().arrayData;
			if (array == null) {
				return String.format("Job %d has no array data", jobsUnit.getDatabaseIndex());
			}
			String aType = array.getInstrumentType();
			String aId = array.getInstrumentId();
			if (aType == null || aId == null) {
				return String.format("Job %d does not have Array Instrument data. This must be completed before Tethys tasks can run", jobsUnit.getDatabaseIndex());
			}
			String fullId = aType + ":" + aId;
			// see if that string is in the list. 
			int exInd = allIds.indexOf(fullId);
			/*
			 *  this might be OK if it's lots of jobs of the same instrument at different time. Really this should be a warning, not an error
			 *  Meditate on this one and see how we go after feedback.  
			 */
			if (exInd >= 0) {
				String err = String .format("Job %d has the same instrument data as another job in the list. Intrument data must usually be unique for each job", jobsUnit.getDatabaseIndex());
				System.out.println(err); // don't return for now, but chaos might ensue. 
			}
		}
		
		return null;
	}
	
	/**
	 * Called from batchProcess.prepareProcessOK();
	 * Run checks to ensure that jobs are able to run prior to startup. 
	 * @return null if all OK, otherwise a warning message to display
	 */
	public String normalStartChecks() {
		if (externalConfiguration == null) {
			return "No configuration for batch jobs has been set";
		}
		return null;
	}

	public OfflineTaskFunctions getOfflineTaskFunctions() {
		return offlineTaskFunctions;
	}

}
