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

		String psfxName = batchControl.getBatchParameters().getMasterPSFX();
		if (psfxName == null) {
			return;
		}
		File psfxFile = new File(psfxName);
		if (psfxFile.exists() == false) {
			return;
		}

		PamSettingsGroup settingsGroup = PSFXReadWriter.getInstance().loadFileSettings(psfxFile);

		loadSettingsGroup(settingsGroup);

		extractTasks();

	}

	private void extractTasks() {
		ArrayList<OfflineTask> taskList = extConfiguration.getAllOfflineTasks();
		taskList = OfflineTaskManager.getManager().getAllOfflineTasks();
		taskDataBlock.clearAll();
		for (OfflineTask task : taskList) {
			OfflineTaskDataUnit du = new OfflineTaskDataUnit(System.currentTimeMillis(), task);
			taskDataBlock.addPamData(du);
		}
	}


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
		
		for (PamControlledUnit unit : extConfiguration.getPamControlledUnits()) {
			if (unit instanceof PamSettings == false) {
				continue;
			}
			PamSettings pamSettings = (PamSettings) unit;
			PamControlledUnitSettings settings = settingsGroup.findUnitSettings(pamSettings.getUnitType(), pamSettings.getUnitName());
			pamSettings.restoreSettings(settings);
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
