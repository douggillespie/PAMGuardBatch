package pambatch.config;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import Array.ArrayManager;
import Array.ArrayParameters;
import Array.PamArray;
import PamController.PSFXReadWriter;
import PamController.PamConfiguration;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingManager;
import PamController.PamSettings;
import PamController.PamSettingsGroup;
import PamController.UsedModuleInfo;
import PamModel.PamModel;
import PamModel.PamModuleInfo;
import offlineProcessing.OfflineTask;
import offlineProcessing.OfflineTaskManager;
import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.tasks.OfflineTaskDataBlock;
import pambatch.tasks.OfflineTaskDataUnit;

public class ExternalConfiguration implements SettingsObserver {

	private BatchControl batchControl;

	private PamConfiguration extConfiguration;

	private OfflineTaskDataBlock taskDataBlock;

	private PamSettingsGroup settingsGroup;

	public ExternalConfiguration(BatchControl batchControl) {
		super();
		this.batchControl = batchControl;
		extConfiguration = new PamConfiguration();
		batchControl.getSettingsObservers().addObserver(this);
		taskDataBlock = new OfflineTaskDataBlock(batchControl.getBatchProcess());
	}


	@Override
	public void settingsUpdate(int changeType) {
		switch (changeType) {
		case SettingsObservers.CHANGE_CONFIG:
			loadExtConfig();
			break;
		}
	}

	/**
	 * Load the configuration set as the external config in batch mode. 
	 */
	public void loadExtConfig() {

		extConfiguration.getPamControlledUnits().clear();

		settingsGroup = getSettingsGroup(true);
		if (settingsGroup == null) {
			return;
		}

		loadSettingsGroup(settingsGroup);
		
		

		extractTasks();

	}
	
	/**
	 * Save settings back to the selected psfx file. 
	 * @return true on success.
	 */
	public boolean saveExtConfig() {
		return saveExtConfig(batchControl.getBatchParameters().getMasterPSFX());
	}
	
	/**
	 * Save the configuration settings back into the psfx. 
	 * @param psfxName
	 * @return true on success.
	 */
	public boolean saveExtConfig(String psfxName) {
		if (settingsGroup == null) {
			return false;
		}
		settingsGroup.setSettingsTime(System.currentTimeMillis());
		return PSFXReadWriter.getInstance().writePSFX(psfxName, settingsGroup);
	}
	
	/**
	 * Get the currently loaded settings group. Will need to extract specific
	 * module settings from within this. 
	 * @return
	 */
	public PamSettingsGroup getSettingsGroup() {
		return getSettingsGroup(true);
	}
	
	/**
	 * Get the settings group, force reload if wanted. 
	 * @param forceReload
	 * @return
	 */
	public PamSettingsGroup getSettingsGroup(boolean forceReload) {		
		if (forceReload) {
			settingsGroup = null;
		}
		if (settingsGroup == null) {		
			String psfxName = batchControl.getBatchParameters().getMasterPSFX();
			if (psfxName == null) {
				return null;
			}
			File psfxFile = new File(psfxName);
			if (psfxFile.exists() == false) {
				return null;
			}
			settingsGroup = PSFXReadWriter.getInstance().loadFileSettings(psfxFile);
		}
		return settingsGroup;
	}

	private void extractTasks() {
		ArrayList<OfflineTask> taskList = extConfiguration.getAllOfflineTasks();
//		taskList = OfflineTaskManager.getManager().getAllOfflineTasks();
		taskDataBlock.clearAll();
		for (OfflineTask task : taskList) {
			OfflineTaskDataUnit du = new OfflineTaskDataUnit(System.currentTimeMillis(), task);
			taskDataBlock.addPamData(du);
		}
	}


	/**
	 * From the settings group, create all the modules for PAMGuard so that they can be interrogated for offline 
	 * tasks, etc. 
	 * @param settingsGroup
	 */
	private void loadSettingsGroup(PamSettingsGroup settingsGroup) {
		if (settingsGroup == null) {
			return;
		}
		PamController pamController = PamController.getInstance();
		// need to find the settings for PamController in there. 
		PamControlledUnitSettings controllerSettings = settingsGroup.findUnitSettings(pamController.getUnitType(), pamController.getUnitName());
		if (controllerSettings == null) {
			return;
		}
		ArrayList<UsedModuleInfo> modulesList;
		try {
			modulesList = (ArrayList<UsedModuleInfo>) controllerSettings.getSettings();
		}
		catch (ClassCastException e) {
			e.printStackTrace();
			return;
		}

		extConfiguration.getSettingsOwners().clear();
		PamSettingManager.getInstance().setSecondaryConfiguration(extConfiguration);

		PamModel pamModel = PamController.getInstance().getModelInterface();
		for (UsedModuleInfo unit : modulesList) {
			PamModuleInfo mi = PamModuleInfo.findModuleInfo(unit.className);
			if (mi == null) {
				System.out.println("Unable to find module info for " + unit.className);
				continue;
			}
			PamControlledUnit module = mi.create(extConfiguration, unit.unitName);
			if (module == null) {
				continue;
			}
			module.setPamConfiguration(extConfiguration);
			extConfiguration.addControlledUnit(module);
		}
		
		/**
		 * Now have a list of settings owners as well as the actual settings, 
		 * so should be able to push settings to every owner. 
		 */
		ArrayList<PamSettings> owners = extConfiguration.getSettingsOwners();
		for (PamSettings owner : owners) {
			//skip the main controller. 
			if (owner.getClass() == PamController.class) {
				continue;
			}
			PamControlledUnitSettings settings = settingsGroup.findUnitSettings(owner.getUnitType(), owner.getUnitName());
			if (settings != null) {
				owner.restoreSettings(settings);
			}
			else {
				System.out.printf("Ext config can't find settings for %s : %s\n", owner.getUnitType(), owner.getUnitName());
			}
		}
		/**
		 * This will put main settings into PAMControlledUnits, but will NOT do anything else for
		 * each module. This isn't great since many modules have multiple settings objects, which are
		 * all controlled from the global settings list that they will have all registered with. In batch mode, where
		 * we're trying to have multiple settings loaded in the same model, a lot of those settings will have gotten 
		 * overwritten when the model is loaded, which messed everything up big time. 
		 */
//		for (PamControlledUnit unit : extConfiguration.getPamControlledUnits()) {
//			if (unit instanceof PamSettings == false) {
//				continue;
//			}
//			PamSettings pamSettings = (PamSettings) unit;
//			PamControlledUnitSettings settings = settingsGroup.findUnitSettings(pamSettings.getUnitType(), pamSettings.getUnitName());
//			if (settings != null) {
//				pamSettings.restoreSettings(settings);
//			}
//			else {
//				System.out.println("No external settings for " + unit.getUnitName());
//			}
//		}
		
		extConfiguration.notifyModelChanged(PamController.INITIALIZATION_COMPLETE);
		
		PamSettingManager.getInstance().setSecondaryConfiguration(null);
	}


	/**
	 * @return the extConfiguration
	 */
	public PamConfiguration getExtConfiguration() {
		return extConfiguration;
	}


	/**
	 * @return the taskDataBlock
	 */
	public OfflineTaskDataBlock getTaskDataBlock() {
		return taskDataBlock;
	}

	/**
	 * Pull the settings pertinent to this module back out of the configuration and back into 
	 * the settings list. 
	 * @param parentModule
	 */
	public int pullSettings(PamControlledUnit parentModule) {
		if (settingsGroup == null || parentModule == null) {
			return -1;
		}
		String unitName = parentModule.getUnitName();
		ArrayList<PamControlledUnitSettings> allSettings = settingsGroup.getUnitSettings();
		ListIterator<PamControlledUnitSettings> it = allSettings.listIterator();
		int changes = 0;
		while (it.hasNext()) {
			PamControlledUnitSettings aSet = it.next();
			if (aSet.getUnitName().equals(unitName) == false) {
				continue;
			}
			/*
			 *  Can work now since the extConfiguration contains a list of owners. 
			 */
			PamSettings owner = extConfiguration.findSettingOwner(aSet.getUnitType(), aSet.getUnitName());
			if (owner == null) {
				System.out.printf("No owner to update settings for %s:%s\n", aSet.getUnitType(), aSet.getUnitName());
				continue;
			}
			Serializable newSettings = owner.getSettingsReference();
			// and update these in the settings
			aSet.setSettings(newSettings);
			changes++;
		}
		return changes;
	}

	/**
	 * Find settings with a specific type and name within the external config
	 * @param unitType
	 * @param unitName
	 * @return
	 */
	public Object findSettings(String unitType, String unitName) {
		if (settingsGroup == null) {
			return null;
		}
		PamControlledUnitSettings sg = settingsGroup.findUnitSettings(unitType, unitName);
		if (sg == null) {
			return null;
		}
		return sg.getSettings();
	}

	public PamArray findArrayData() {
		Object arrayObj = findSettings(ArrayManager.arrayManagerType, ArrayManager.arrayManagerType);
		if (arrayObj == null) {
			return null;
		}
		ArrayList<PamArray> arrayArray;
		//		PamArray array = ArrayManager.getArrayManager().unpackArrayObject(arrayObj);
		if (arrayObj instanceof ArrayParameters) {
			arrayArray = ((ArrayParameters) arrayObj).getArrayList();
		}
		else {
			arrayArray =  (ArrayList<PamArray>) arrayObj;
		}
		if (arrayArray.size() == 0) {
			return null;
		}
		try {
			return (PamArray) arrayArray.get(0);
		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Update the array data in the external settings. 
	 * @param nextJob
	 */
	public PamSettingsGroup updateJobSettings(BatchDataUnit nextJob) {
		PamSettingsGroup sg = getSettingsGroup(true);
		BatchJobInfo jobInfo = nextJob.getBatchJobInfo();
		if (jobInfo.arrayData == null) {
			return sg;
		}
		PamControlledUnitSettings ps = sg.findUnitSettings(ArrayManager.arrayManagerType, ArrayManager.arrayManagerType);
		ArrayParameters ap = (ArrayParameters) ps.getSettings();
		ArrayList<PamArray> arrays = ap.getArrayList();
		arrays.clear();
		arrays.add(jobInfo.arrayData);
		return sg;
	}


}
