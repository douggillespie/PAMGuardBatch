package pambatch.swing;

import java.awt.Window;
import java.io.File;

import Acquisition.FolderInputParameters;
import binaryFileStorage.BinaryStoreSettings;
import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.config.ViewerDatabase;

/**
 * Dialog for creating an offline job. This will be a bit different to 
 * the JobDialog, which works OK for raw processing since this will have
 * to select a database, then may be able to automatically extract the 
 * binary location and the wav location from within that. Perhaps one day even
 * abstract the rawinput location so that it can work with different inputs
 * such as sonar data. 
 * @author dg50
 *
 */
public class OfflineJobDialog extends JobDialog {

	private static OfflineJobDialog singleInstance;
	
	private static int[] jobOrder = {JobDialog.DATABASE, JobDialog.BINARY, JobDialog.SOURCES};
	
	public OfflineJobDialog(Window parentFrame, BatchControl batchControl) {
		super(parentFrame, batchControl);
		// TODO Auto-generated constructor stub
	}

	public static boolean showDialog(Window parentFrame, BatchControl batchControl, BatchDataUnit batchDataUnit) {
		//		if (singleInstance == null) {
		singleInstance = new OfflineJobDialog(parentFrame, batchControl);
		//		}
		singleInstance.setParams(batchDataUnit);
		singleInstance.setVisible(true);
		return singleInstance.isOk;
	}
	@Override
	public int[] getSelectionOrder() {
		return jobOrder;
	}

	@Override
	protected void selectionChanged(int iSet) {
		if (iSet == JobDialog.DATABASE) {
			// extract binary and wav information from the database
			// if we can. 
			String dbName = getJobSet(JobDialog.DATABASE).getText();
			JobSet binSet = getJobSet(JobDialog.BINARY);
			JobSet sourceSet = getJobSet(JobDialog.SOURCES);
			if (dbName == null) {
				return;
			}
			File dbFile = new File(dbName);
			if (dbFile.exists() == false) {
				binSet.setText(null);
				return;
			}
			ViewerDatabase viewDB = new ViewerDatabase(batchControl, dbName);
			BinaryStoreSettings binSettings = viewDB.getBinarySettings();
			if (binSettings != null) {
				binSet.setText(binSettings.getStoreLocation());
			}
			FolderInputParameters daqSet = viewDB.getDaqSettings();
			if (daqSet != null) {
				sourceSet.setText(daqSet.getMostRecentFile());
			}
		}
		
	}

	

}
