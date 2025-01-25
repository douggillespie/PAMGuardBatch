package pambatch.config;

import java.io.File;
import java.util.ArrayList;

import Acquisition.AcquisitionControl;
import Acquisition.AcquisitionParameters;
import Acquisition.FolderInputParameters;
import Acquisition.FolderInputSystem;
import Array.ArrayManager;
import Array.ArrayParameters;
import Array.PamArray;
import PamController.PSFXReadWriter;
import PamController.PamControlledUnit;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingsGroup;
import PamController.UsedModuleInfo;
import PamModel.PamModel;
import PamModel.PamModuleInfo;
import PamView.dialog.warn.WarnOnce;
import binaryFileStorage.BinaryStore;
import binaryFileStorage.BinaryStoreSettings;
import generalDatabase.DBControlSettings;
import generalDatabase.DBSettingsStore;
import generalDatabase.LogSettings;
import generalDatabase.PamConnection;
import pambatch.BatchControl;

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
		this.databaseName = batchControl.checkDatabasePath(databaseName);;
		dbCtrlSettings = new DBControlSettings();
	}

	private PamSettingsGroup getSettings() {
		if (dbSettings == null) {
			dbSettings = extractSettings(databaseName);
		}
		return dbSettings;
	}

	private PamSettingsGroup extractSettings(String databaseName) {
		databaseName = batchControl.checkDatabasePath(databaseName);
		boolean dbOpen = dbCtrlSettings.openDatabase(databaseName);
		if (dbOpen == false) {
			return null;
		}
		PamConnection con = dbCtrlSettings.getConnection();
		if (con == null) {
			return null;
		}
		ArrayList<PamControlledUnitSettings> settings = dbCtrlSettings.loadSettingsFromDB(con, true);
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
	 * rewrite the settings to the database. Then close it.
	 * @return true if it seems to have worked OK, false otherwise.  
	 */
	public boolean reWriteSettings() {
		databaseName = batchControl.checkDatabasePath(databaseName);
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

	/**
	 * Get the binary store settings. 
	 * @return binary store settings or null. 
	 */
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
	
	public PamArray getArray() {
		PamControlledUnitSettings arraySet = findSettings(ArrayManager.arrayManagerType, ArrayManager.arrayManagerType);
		if (arraySet == null) {
			return null;
		}
		Object set = arraySet.getSettings();
		if (set instanceof ArrayParameters) {
			ArrayParameters ap = (ArrayParameters) set;
			ArrayList<PamArray> arrays = ap.getArrayList();
			if (arrays.size() > 0) {
				return arrays.get(0);
			}
		}
		return null;
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
		databaseName = batchControl.checkDatabasePath(databaseName);
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

	/**
	 * Pull the full job info out of a viewer database. 
	 * @param databaseFile
	 * @return
	 */
	public static BatchJobInfo extractJobInfo(String databaseFile) {
		ViewerDatabase viewDB = new ViewerDatabase(null, databaseFile);
		String soundFolder = null;
		String binFolder = null;
		PamSettingsGroup allSettings = viewDB.getSettings();
		if (allSettings == null) {
			return null; // probably not a viewer datatbase. 
		}
		BinaryStoreSettings binSettings = viewDB.getBinarySettings();
		if (binSettings != null) {
			binFolder = binSettings.getStoreLocation();
		}
		FolderInputParameters daqSettings = viewDB.getDaqSettings();
		if (daqSettings != null) {
			soundFolder = daqSettings.getMostRecentFile();
		}
		PamArray arrayInfo = viewDB.getArray();

		// should probably worry if the binary store is empty ? Though it's possible that there
		// is only database data. So let it pass. 
		BatchJobInfo batchJobInfo = new BatchJobInfo(soundFolder, binFolder, databaseFile);
		batchJobInfo.arrayData = arrayInfo;
		return batchJobInfo;
	}

	/**
	 * in viewer mode, rewrite array data back to it's database. <p>
	 * Should only be called in viewer !
	 * @param batchJobInfo
	 * @return
	 */
	public static boolean rewriteArrayData(BatchControl batchControl, BatchJobInfo batchJobInfo) {
		String dbName = batchJobInfo.outputDatabaseName;
		dbName = batchControl.checkDatabasePath(dbName);
		File dbFile = new File(dbName);
		if (dbFile.exists() == false) {
			String err = String.format("The database %s does not exist", dbName);
			WarnOnce.showWarning("Error writing array data to database", err, WarnOnce.WARNING_MESSAGE);
			return false;
		}
		PamArray array = batchJobInfo.arrayData;
		if (array == null) {
			return false; // don't ever write a null array to one of the databases. 
		}
		ViewerDatabase viewDB = new ViewerDatabase(null, dbName);
		PamSettingsGroup allSettings = viewDB.getSettings();
		if (allSettings == null) {
			return false;
		}
		PamControlledUnitSettings arraySet = viewDB.findSettings(ArrayManager.arrayManagerType, ArrayManager.arrayManagerType);
		if (arraySet == null) {
			return false;
		}
		Object set = arraySet.getSettings();
		if (set instanceof ArrayParameters) {
			ArrayParameters ap = (ArrayParameters) set;
			ArrayList<PamArray> arrays = ap.getArrayList();
			if (arrays.size() > 0) {
				arrays.remove(0);
			}
			arrays.add(0, array);
			// don't actually need to replace the settings, since we've modified the object in place. 
			boolean ok = viewDB.reWriteSettings();
			return ok;
		}
		else {
			return false;
		}
	}
	

}
