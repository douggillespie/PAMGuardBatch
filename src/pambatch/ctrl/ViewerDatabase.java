package pambatch.ctrl;

import java.io.File;
import java.util.ArrayList;

import Acquisition.AcquisitionControl;
import Acquisition.AcquisitionParameters;
import Acquisition.FolderInputParameters;
import Acquisition.FolderInputSystem;
import PamController.PSFXReadWriter;
import PamController.PamControlledUnitSettings;
import PamController.PamController;
import PamController.PamSettingsGroup;
import binaryFileStorage.BinaryStore;
import binaryFileStorage.BinaryStoreSettings;
import generalDatabase.DBControlSettings;
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
		dbSettings = extractSettings(databaseName);
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
	
	

	
}
