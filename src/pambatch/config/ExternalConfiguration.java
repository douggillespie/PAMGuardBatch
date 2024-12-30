package pambatch.config;

import java.io.File;
import java.util.ArrayList;

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
		 * This will put main settings into PAMControlledUnits, but will NOT do anything else for
		 * each module. This isn't great since many modules have multiple settings objects, which are
		 * all controlled from the global settings list that they will have all registered with. In batch mode, where
		 * we're trying to have multiple settings loaded in the same model, a lot of those settings will have gotten 
		 * overwritten when the modle is loaded, which messed everything up big time. 
		 */
		for (PamControlledUnit unit : extConfiguration.getPamControlledUnits()) {
			if (unit instanceof PamSettings == false) {
				continue;
			}
			PamSettings pamSettings = (PamSettings) unit;
			PamControlledUnitSettings settings = settingsGroup.findUnitSettings(pamSettings.getUnitType(), pamSettings.getUnitName());
			if (settings != null) {
				pamSettings.restoreSettings(settings);
			}
			else {
				System.out.println("No external settings for " + unit.getUnitName());
			}
		}
		
		extConfiguration.notifyModelChanged(PamController.INITIALIZATION_COMPLETE);
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




}
