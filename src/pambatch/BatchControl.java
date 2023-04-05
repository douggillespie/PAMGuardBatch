package pambatch;

import java.awt.Desktop;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JPopupMenu;

import Acquisition.FolderInputSystem;
import Acquisition.pamAudio.PamAudioFileFilter;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamGUIManager;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamController.fileprocessing.ReprocessStoreChoice;
import PamView.PamTabPanel;
import PamguardMVC.dataOffline.OfflineDataLoadInfo;
import binaryFileStorage.BinaryStore;
import generalDatabase.DBControl;
import generalDatabase.DBControlUnit;
import pambatch.comms.BatchMulticastRX;
import pambatch.config.BatchJobInfo;
import pambatch.config.BatchParameters;
import pambatch.ctrl.JobController;
import pambatch.swing.BatchSetDialog;
import pambatch.swing.BatchTabPanel;
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
	
	private BatchMulticastRX batchMulticastRX;
	
	/**
	 * @return the batchProcess
	 */
	public BatchProcess getBatchProcess() {
		return batchProcess;
	}

	private static final String DEFAULTWINDOWSEXE = "C:\\Program Files\\Pamguard\\Pamguard.exe";
	
	public BatchControl(String unitName) {
		super(unitType, unitName);
		System.out.println("Exe command is " + findStartExecutable());
		System.out.println("Java command is " + findJavaCommand());
		swingMenus = new SwingMenus(this);
		batchProcess = new BatchProcess(this);
		addPamProcess(batchProcess);
		if (PamGUIManager.getGUIType() != PamGUIManager.NOGUI) {
			batchTabPanel = new BatchTabPanel(this);
		}
		PamSettingManager.getInstance().registerSettings(this);
		if (batchTabPanel != null) {
			batchTabPanel.setParams(batchParameters);
		}
		batchMulticastRX = new BatchMulticastRX(this);
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
			loadExistingJobs();
		}
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
	 * @param batchJobInfo
	 * @return
	 */
	public ArrayList<String> getBatchJobLaunchParams(BatchJobInfo batchJobInfo) {
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
		command.add(batchJobInfo.soundFileFolder);
		if (batchJobInfo.outputBinaryFolder != null) {
			command.add(BinaryStore.GlobalFolderArg);
			command.add(batchJobInfo.outputBinaryFolder);
		}
		if (batchJobInfo.outputDatabaseName != null) {
			command.add(DBControl.GlobalDatabaseNameArg);
			command.add(batchJobInfo.outputDatabaseName);
		}
		command.add("-autostart");
		/**
		 * Probably don't want to auto exit so that the monitor can check the job status
		 * and see clearly that it's finished rather than crashed, then tell it to exit.
		 */
		command.add("-autoexit"); 
		command.add(ReprocessStoreChoice.paramName);
		command.add("OVERWRITEALL");
		
		
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
		batchProcess.getBatchLogging().logData(DBControlUnit.findConnection(), newJobData);
		boolean ok = JobDialog.showDialog(getGuiFrame(), this, newJobData); 
		if (ok) {
			batchProcess.getBatchDataBlock().addPamData(newJobData);
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
		batchMulticastRX.stopReceiving();
	}

	
}
;