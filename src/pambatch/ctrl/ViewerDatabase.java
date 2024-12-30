package pambatch.ctrl;

import java.io.File;
import java.util.ArrayList;

import Acquisition.AcquisitionControl;
import Acquisition.AcquisitionParameters;
import Acquisition.FolderInputParameters;
import Acquisition.FolderInputSystem;
import PamController.PSFXReadWriter;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingsGroup;
import PamController.UsedModuleInfo;
import PamModel.PamModel;
import PamModel.PamModuleInfo;
import binaryFileStorage.BinaryStore;
import binaryFileStorage.BinaryStoreSettings;
import generalDatabase.DBControlSettings;
import generalDatabase.DBSettingsStore;
import generalDatabase.LogSettings;
import generalDatabase.PamConnection;
import pambatch.BatchControl;
import pambatch.config.SettingsObservers;

/**
 * Some functions for viewer databases to extract latest configuration, 
 * possibly save as a psfx, and get fundamentals such as binary store
 * and source locations. Based around an existing PamDatabase class, but
 * there may be multiples of these in operation at once, so avoiding
 * turning it into a controlledunit. 
 * @author dg50
 *
 */
public class ViewerDatabase {

	private String databaseName;

	private DBControlSettings dbCtrlSettings;

	private PamSettingsGroup dbSettings;

	private BatchControl batchControl;

	public ViewerDatabase(BatchControl batchControl, String databaseName) {
		this.batchControl = batchControl;
		this.databaseName = databaseName;
		dbCtrlSettings = new DBControlSettings();
	}

	private PamSettingsGroup getSettings() {
		if (dbSettings == null) {
			dbSettings = extractSettings(databaseName);
		}
		return dbSettings;
	}

	private PamSettingsGroup extractSettings(String databaseName) {
		boolean dbOpen = dbCtrlSettings.openDatabase(databaseName);
		if (dbOpen == false) {
			return null;
		}
		PamConnection con = dbCtrlSettings.getConnection();
		if (con == null) {
			return null;
		}
		ArrayList<PamControlledUnitSettings> settings = dbCtrlSettings.loadSettingsFromDB(con);
		dbCtrlSettings.pamClose();
		return new PamSettingsGroup(System.currentTimeMillis(), settings);
	}
	
	/**
	 * Does it have a module ? This should be listed in the PamController settings. 
	 * @param unitType
	 * @param unitName
	 * @return
	 */
	public boolean hasModule(String unitType, String unitName) {

		ArrayList<UsedModuleInfo> moduleInfo = getModulesList();
		if (moduleInfo == null) {
			return false;
		}

		for (UsedModuleInfo aModule: moduleInfo) {
			if (aModule.getUnitType().equals(unitType) && aModule.getUnitName().equals(unitName)) {
				return true;
			}
		}
		return false;
 	}
	
	/**
	 * Get the modules list which is extracted from the PamController settings. 
	 * @return
	 */
	public ArrayList<UsedModuleInfo> getModulesList() {		
		PamControlledUnitSettings cuSet = findSettings(PamController.getInstance().getUnitType(), PamController.getInstance().getUnitName());
		if (cuSet == null) {
			return null;
		}
		ArrayList<UsedModuleInfo> moduleInfo = null;
		try {
			moduleInfo = (ArrayList<UsedModuleInfo>) cuSet.getSettings();
		}
		catch (ClassCastException e) {
			return null;
		}
		return moduleInfo;
	}

	public boolean addModule(String unitType, String unitName, String className) {		
		ArrayList<UsedModuleInfo> moduleInfo = getModulesList();
		if (moduleInfo == null) {
			return false;
		}
		PamModuleInfo modInf = PamModuleInfo.findModuleInfo(className);
		if (modInf == null) {
			System.out.printf("Unable to find info for module class %s\n", className);
		}
		
		// need to find the moduleinfo from the PamModel. 
//		PamModel.getPamModel().
		UsedModuleInfo newModule = new UsedModuleInfo(className, unitType, unitName);
		moduleInfo.add(newModule);
		return true;
	}

	/**
	 * rewrite the settings to the database. 
	 * @return true if it seems to have worked OK, false otherwise.  
	 */
	public boolean reWriteSettings() {
		boolean dbOpen = dbCtrlSettings.openDatabase(databaseName);
		if (dbOpen == false) {
			return false;
		}
		PamConnection con = dbCtrlSettings.getConnection();
		if (con == null) {
			return false;
		}
		LogSettings viewerLog = dbCtrlSettings.getDbProcess().getLogViewerSettings();
		viewerLog.saveSettings(dbSettings, System.currentTimeMillis());
//		viewerLog.logSettings(dbSettings.,  System.currentTimeMillis());
//		DBSettingsStore dbSettingsStore = dbCtrlSettings.getSettingsStore();
//		dbSettingsStore.addSettingsGroup(dbSettings); // push the updated settings to the end of the list. 
//		// and tell the database to save it's settings
//		// don't call saveSettings since that just pulls settings from the modules, which is not what we need. 
////		dbCtrlSettings.saveSettingsToDB();
//		dbSettingsStore.
		dbCtrlSettings.pamClose();
		return true;
	}

	public BinaryStoreSettings getBinarySettings() {
		PamControlledUnitSettings binSet = findSettings(BinaryStore.defUnitType, null);
		if (binSet == null) {
			return null;
		}
		BinaryStoreSettings binSettings = null;
		try {
			binSettings = (BinaryStoreSettings) binSet.getSettings();
		}
		catch (ClassCastException e) {
			return null;
		}
		return binSettings;
	}

	public FolderInputParameters getDaqSettings() {
		PamControlledUnitSettings aSet = findSettings(AcquisitionControl.unitType, null);
		if (aSet == null) {
			return null;
		}
		AcquisitionParameters daqSettings = null;
		try {
			daqSettings = (AcquisitionParameters) aSet.getSettings();
		}
		catch (ClassCastException e) {
			return null;
		}
		if (daqSettings == null) {
			return null;
		}
		String sysType = daqSettings.getDaqSystemType();
		if (FolderInputSystem.sysType.equalsIgnoreCase(sysType) == false) {
			return null;
		}
		aSet = findSettings(FolderInputSystem.daqType, null);
		if (aSet == null) {
			return null;
		}
		Object settings = aSet.getSettings();
		if (settings instanceof FolderInputParameters) {
			return (FolderInputParameters) settings;
		}
		return null;
	}

	/**
	 * Find some settings.  
	 * @param unitType unit type for settings. 
	 * @param unitName unit name. Can be null and first of correct type will be reutrned. 
	 * @return found settings, or null. 
	 */
	public PamControlledUnitSettings findSettings(String unitType, String unitName) {
		dbSettings = getSettings();
		if (dbSettings == null) {
			return null;
		}
		for (PamControlledUnitSettings aSet : dbSettings.getUnitSettings()) {
			if (aSet.getUnitType().equalsIgnoreCase(unitType) == false) {
				continue;
			}
			if (unitName != null && aSet.getUnitName().equalsIgnoreCase(unitName) == false) {
				continue;
			}
			return aSet;
		}
		return null;
	}

	/**
	 * Get all unit settings for a given unit name. 
	 * @param unitName unit name
	 * @return arrray list of settings. 
	 */
	public ArrayList<PamControlledUnitSettings> findSettingsForName(String unitName) {
		ArrayList<PamControlledUnitSettings> sets = new ArrayList<PamControlledUnitSettings>();
		for (PamControlledUnitSettings aSet : dbSettings.getUnitSettings()) {
			if (aSet.getUnitName().equalsIgnoreCase(unitName)) {
				sets.add(aSet);
			}
		}
		return sets;
	}

	public void extractPSFX() {
		dbSettings = extractSettings(databaseName);
		if (dbSettings == null) {
			return;
		}
		PSFXReadWriter writer = PSFXReadWriter.getInstance();
		File aName = writer.selectSettingsFile(PamController.getMainFrame(), false);
		if (aName == null) {
			return;
		}
		boolean wrote = writer.writePSFX(aName.getAbsolutePath(), dbSettings);
		if (wrote) {
			// set this as the main settings file for the batch system. 
			batchControl.getBatchParameters().setMasterPSFX(aName.getAbsolutePath());
			batchControl.settingsChange(SettingsObservers.CHANGE_CONFIG);
		}

	}

	/**
	 * Replace the settings in the configuration with ones from the one we want to run. 
	 * @param aSet
	 */
	public boolean replaceSettings(PamControlledUnitSettings aSet) {
		return dbSettings.replaceSettings(aSet);
	}

	/**
	 * Add settings to the database list. This will happen if a new module was added to 
	 * the configuration and should put the required settings in their place. 
	 * @param aSet
	 */
	public void addSettings(PamControlledUnitSettings aSet) {
		dbSettings.addSettings(aSet);
	}




}
