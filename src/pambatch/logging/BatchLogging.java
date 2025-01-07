package pambatch.logging;

import java.sql.Types;

import Array.ArrayManager;
import Array.PamArray;
import PamController.PamControlledUnitSettings;
import PamView.PamControlledGUISwing;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import generalDatabase.EmptyTableDefinition;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLLogging;
import generalDatabase.SQLTypes;
import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.BatchJobStatus;
import pambatch.config.BatchJobInfo;

public class BatchLogging extends SQLLogging {

	private BatchControl batchControl;
	
	private PamTableItem  updateTime, sourceFolder, binaryFolder, databaseName, array, percent, status;
	
	private PamTableDefinition tableDefinition;

	public BatchLogging(BatchControl batchControl, PamDataBlock pamDataBlock) {
		super(pamDataBlock);
		this.batchControl = batchControl;
		
		tableDefinition = new PamTableDefinition(batchControl.getUnitName());
		tableDefinition.addTableItem(updateTime = new PamTableItem("Updated", Types.TIMESTAMP));
		tableDefinition.addTableItem(sourceFolder = new PamTableItem("Source", Types.VARCHAR));
		tableDefinition.addTableItem(binaryFolder = new PamTableItem("Binary", Types.VARCHAR));
		tableDefinition.addTableItem(databaseName = new PamTableItem("Database", Types.VARCHAR));
		tableDefinition.addTableItem(array = new PamTableItem("ArrayData", Types.BLOB));
		tableDefinition.addTableItem(percent = new PamTableItem("Percent", Types.REAL));
		tableDefinition.addTableItem(status = new PamTableItem("Status", Types.CHAR, 50));
		
		tableDefinition.setUseCheatIndexing(false);
		
		setTableDefinition(tableDefinition);
	}

	@Override
	public void setTableData(SQLTypes sqlTypes, PamDataUnit pamDataUnit) {
		BatchDataUnit batchDataUnit = (BatchDataUnit) pamDataUnit;
		updateTime.setValue(sqlTypes.getTimeStamp(pamDataUnit.getLastUpdateTime()));
		BatchJobInfo jobInfo = batchDataUnit.getBatchJobInfo();
		if (jobInfo == null) {
			sourceFolder.setValue(null);
			binaryFolder.setValue(null);
			databaseName.setValue(null);
			status.setValue(null);
			array.setValue(null);
		}
		else {
			sourceFolder.setValue(jobInfo.soundFileFolder);
			binaryFolder.setValue(jobInfo.outputBinaryFolder);
			databaseName.setValue(jobInfo.outputDatabaseName);
			percent.setValue((float) jobInfo.percentDone);
			if (jobInfo.jobStatus == null) {
				status.setValue(null);
			}
			else {
				status.setValue(jobInfo.jobStatus.toString());
			}
			PamArray arrayData = jobInfo.arrayData;
			if (arrayData == null) {
				array.setValue(null);
			}
			else { // serialise the data into a byte array and store. 
				PamControlledUnitSettings set = new PamControlledUnitSettings(ArrayManager.arrayManagerType, ArrayManager.arrayManagerType, 
						ArrayManager.class.getName(), arrayData.serialVersionUID, arrayData);
				array.setValue(set.getNamedSerialisedByteArray());
			}
		}
	}

	@Override
	protected PamDataUnit createDataUnit(SQLTypes sqlTypes, long timeMilliseconds, int databaseIndex) {
		BatchJobInfo jobInfo = new BatchJobInfo();
		jobInfo.soundFileFolder = sourceFolder.getDeblankedStringValue();
		jobInfo.outputBinaryFolder = binaryFolder.getDeblankedStringValue();
		jobInfo.outputDatabaseName = databaseName.getDeblankedStringValue();
		double percentDone = percent.getFloatValue();
		jobInfo.percentDone = percentDone;
		String statusStr = status.getDeblankedStringValue();
		jobInfo.jobStatus = BatchJobStatus.getValue(statusStr);
		Object serArray = array.getValue();
		if (serArray instanceof byte[]) {
			Object settings = PamControlledUnitSettings.createFromNamedByteArray((byte[]) serArray);
			if (settings instanceof PamControlledUnitSettings) {
				Object arrayData = ((PamControlledUnitSettings) settings).getSettings();
				if (arrayData instanceof PamArray) {
					jobInfo.arrayData = (PamArray) arrayData;
				}
			}
		}
		
		
		BatchDataUnit dataUnit = new BatchDataUnit(timeMilliseconds, jobInfo);
		
		return dataUnit;
	}

}
