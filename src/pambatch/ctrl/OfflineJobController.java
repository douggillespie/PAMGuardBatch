package pambatch.ctrl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.ListIterator;

import Array.ArrayManager;
import Array.ArrayParameters;
import Array.PamArray;
import PamController.PamConfiguration;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamSettings;
import PamController.PamSettingsGroup;
import PamView.dialog.warn.WarnOnce;
import binaryFileStorage.BinaryStoreSettings;
import offlineProcessing.OfflineTask;
import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.config.BatchJobInfo;
import pambatch.config.ExternalConfiguration;
import pambatch.config.ViewerDatabase;
import pambatch.remote.RemoteAgentDataUnit;
import pambatch.tasks.OfflineTaskDataBlock;
import pambatch.tasks.OfflineTaskDataUnit;
import pambatch.tasks.TaskSelection;
import tethys.species.SpeciesMapManager;

/**
 * Job controller for offline tasks. Will require quite a different set of command line parameters
 * to the job control for normal mode. 
 * @author dg50
 *
 */
public class OfflineJobController extends LocalJobController {

	public OfflineJobController(BatchControl batchControl, RemoteAgentDataUnit remoteAgent, BatchDataUnit batchDataUnit,
			JobMonitor jobMonitor) {
		super(batchControl, remoteAgent, batchDataUnit, jobMonitor);
	}

	@Override
	public boolean launchJob(ArrayList<String> pamguardOptions) {
		/*
		 *  modify the parameters of the viewer database for the jobs  within this instance, before launching.
		 *  That should be easier than sending settings over UDP to the external viewer once it's running. 
		 *  On the other hand, if this were operating over a network, then modifying locally won't work!
		 *  Do local for now.   
		 */
		boolean modsOk = modifyDatabaseConfiguration();
		if (modsOk == false) {
			return false;
		}
		/*
		 * Will need to launch PAMGuard, then launch another thread to start the jobs in turn ?
		 * Or have command line stuff in PG so that the batch commands run automatically ? 
		 */
		
//		return false;
		return super.launchJob(pamguardOptions);
//
//		ArrayList<String> totalCommand = new ArrayList<>();
//		totalCommand.add(getBatchControl().findStartExecutable());
//		totalCommand.addAll(pamguardOptions);
//		
//		String singleLine = getOneLineCommand(totalCommand);
//		
//		String commandsOnly = getOneLineCommand(pamguardOptions);
//		return false;
	}

	/**
	 * Modify database paramaters for remote job. 
	 * @return
	 */
	private boolean modifyDatabaseConfiguration() {
		BatchJobInfo jobInfo = getBatchDataUnit().getBatchJobInfo();
		String dbName = getBatchControl().checkDatabasePath(jobInfo.outputDatabaseName);
		ViewerDatabase viewDB = new ViewerDatabase(getBatchControl(), dbName);
		
		// always write the array settings if they are included in the Batch job configuration. 
		PamArray arrayData = getBatchDataUnit().getBatchJobInfo().arrayData;
		if (arrayData != null) {
			PamControlledUnitSettings setUnit = viewDB.findSettings(ArrayManager.arrayManagerType, ArrayManager.arrayManagerType);
			Object set = setUnit.getSettings();
			if (set instanceof ArrayParameters) {
				ArrayParameters ap = (ArrayParameters) set;
				ArrayList<PamArray> arrays = ap.getArrayList();
				if (arrays.size() > 0) {
					arrays.remove(0);
				}
				arrays.add(0, arrayData);
			}
//			viewDB.rewriteArrayData(getBatchDataUnit().getBatchJobInfo());
		}
//		BinaryStoreSettings binSettings = viewDB.getBinarySettings();
//		if (binSettings == null) {
//			System.out.println("Unable to read settings from viewer database " + dbName);
//			return false;
//		}
		/*
		 * Now go through the selected jobs and check all their settings from the current psfx in the binSettings and
		 * push those settings into binSettings.  
		 */
		ExternalConfiguration externalConfiguration = getBatchControl().getExternalConfiguration();
		OfflineTaskDataBlock taskBlock = externalConfiguration.getTaskDataBlock();
		// iterate through offline tasks. 
		ListIterator<OfflineTaskDataUnit> it = taskBlock.getListIterator(0);
		while (it.hasNext()) {
			OfflineTaskDataUnit olTaskDataUnit = it.next();
			OfflineTask task = olTaskDataUnit.getOfflineTask();
			TaskSelection taskSelection = getBatchControl().getBatchParameters().getTaskSelection(task);
			if (taskSelection.selected == false) {
				continue;
			}
			checkConfiguration(viewDB, task);
		}
		// now save the settings back into the database. 
		// first need to load them into all modules, then saving should work OK. 
		viewDB.reWriteSettings();
		
		return true;
	}

	/**
	 * Check the task settings are all set up and correct in the remote database settings. 
	 * This requires first checking the module is there, then checking that all the settings for the 
	 * module are copied over. 
	 * @param viewDB
	 * @param task
	 */
	private void checkConfiguration(ViewerDatabase viewDB, OfflineTask<?> task) {
		PamControlledUnit taskCU = task.getTaskControlledUnit();
		PamControlledUnitSettings exSettings = viewDB.findSettings(taskCU.getUnitType(), taskCU.getUnitName());
		boolean haveModule = viewDB.hasModule(taskCU.getUnitType(), taskCU.getUnitName());
		if (haveModule == false) {
			addPAMGuardModule(viewDB, taskCU);
		}
		/*
		 *  get all settings associated with this task. This will have to be based on just the name since some tasks
		 *  are holding their own settings in different objects to the main one. e.g. if you pass the name of a click
		 *  detector, you get eight settings back, e.g.
		 *  Found 8 settings for task type Click Detector name Minke Detector
				Detector Vetoes = Minke Detector
				Click Detector = Minke Detector
				Click Train Detector = Minke Detector
				Simple Click Echo system = Minke Detector
				BasicClickIdParams = Minke Detector
				ClickSweepClassifier = Minke Detector
				Click Spectrogram Display = Minke Detector
				Wigner Plot Options = Minke Detector
		 *   
		 *   For the click classifier, it possibly three actual param sets, Click Detector, which will tell it which classifer
		 *   to use, BasicClickIdParams, OR clickSweepClassifier depending on what was selected. Most others are mostly 
		 *   display options. May not want to override vetoes though since this may have been set up differently for each job!
		 *   Hard to deal with this now unless I add a list of used settings to every batch job. 
		 *   
		 *    Do we (I) need to worry about the settings in the upstream chain ? I hope not.  
		 */
		/**
		 * Get the current main list from the loaded external configuration:
		 */
		ExternalConfiguration extConfig = getBatchControl().getExternalConfiguration();
		PamSettingsGroup extSettings = extConfig.getSettingsGroup();
		if (extSettings == null) {
			System.out.println("No external psfx settings available. Can't do this !");
			return;
		}

				
		ArrayList<PamSettings> settings = task.getSettingsProviders();
//		ArrayList<PamControlledUnitSettings> settings = extSettings.findSettingsForName(task.getUnitName());
		System.out.printf("Found %d settings for task type %s name %s\n", settings.size(), task.getUnitType(), task.getUnitName());
		for (int i = 0; i < settings.size(); i++) {
			PamSettings aSetter = settings.get(i);
			if (aSetter == null) {
				System.out.printf("Found empty (therefore corrupt) settings for task type %s name %s at index %d\n", task.getUnitType(), task.getUnitName(), i);
			}
			Serializable obj = aSetter.getSettingsReference();
			if (aSetter instanceof SpeciesMapManager) {
				System.out.println("setup species map manager");
				SpeciesMapManager globalManager = SpeciesMapManager.getInstance();
				// doesn't work because global maps can't find the datablocks to extract from. Grrrr.
				obj = globalManager.getSettingsReference(extConfig.getExtConfiguration());
//				obj = globalManager.getSettingsReference(extConfig.getExtConfiguration());
			}
			if (obj == null) continue;
			PamControlledUnitSettings aSet = new PamControlledUnitSettings(aSetter.getUnitType(), aSetter.getUnitName(), obj.getClass().getName(), aSetter.getSettingsVersion(), obj);
			boolean rep = viewDB.replaceSettings(aSet);
			System.out.printf("\t%s, %s replaced %s\n", aSet.getUnitType(), aSet.getUnitName(), rep ? "ok" : "Fail");
			// perhaps need to push these settings down to the thing using it, then they will get pu
			if (rep == false) {
				viewDB.addSettings(aSet);
			}
		}
		// settings are rewritten in function that called this one
		System.out.println("");
		
	}

	private void addPAMGuardModule(ViewerDatabase viewDB, PamControlledUnit taskCU) {
		/*
		 * Could be tricky. Will need to add the pamcontrolled unit to the list in viewDB.dbSettings object for the PAMConteller.
		 * then add all the individual settings for that task by name since there may be several.  
		 */

		boolean haveModule = viewDB.addModule(taskCU.getUnitType(), taskCU.getUnitName(), taskCU.getClass().getName());
		
	}

}
