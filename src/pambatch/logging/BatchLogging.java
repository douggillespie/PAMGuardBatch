package pambatch.logging;

import java.sql.Types;

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
	
	private PamTableItem  updateTime, sourceFolder, binaryFolder, databaseName, percent, status;
	
	private PamTableDefinition tableDefinition;

	public BatchLogging(BatchControl batchControl, PamDataBlock pamDataBlock) {
		super(pamDataBlock);
		this.batchControl = batchControl;
		
		tableDefinition = new PamTableDefinition(batchControl.getUnitName());
		tableDefinition.addTableItem(updateTime = new PamTableItem("Updated", Types.TIMESTAMP));
		tableDefinition.addTableItem(sourceFolder = new PamTableItem("Source", Types.VARCHAR));
		tableDefinition.addTableItem(binaryFolder = new PamTableItem("Binary", Types.VARCHAR));
		tableDefinition.addTableItem(databaseName = new PamTableItem("Database", Types.VARCHAR));
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
		}
		else {
			sourceFolder.setValue(jobInfo.soundFileFolder);
			binaryFolder.setValue(jobInfo.outputBinaryFolder);
			databaseName.setValue(jobInfo.outputDatabaseName);
			if (jobInfo.jobStatus == null) {
				status.setValue(null);
			}
			else {
				status.setValue(jobInfo.jobStatus.toString());
			}
		}
	}

	@Override
	protected PamDataUnit createDataUnit(SQLTypes sqlTypes, long timeMilliseconds, int databaseIndex) {
		BatchJobInfo jobInfo = new BatchJobInfo();
		jobInfo.soundFileFolder = sourceFolder.getDeblankedStringValue();
		jobInfo.outputBinaryFolder = binaryFolder.getDeblankedStringValue();
		jobInfo.outputDatabaseName = databaseName.getDeblankedStringValue();
		String statusStr = status.getDeblankedStringValue();
		jobInfo.jobStatus = BatchJobStatus.getValue(statusStr);
		BatchDataUnit dataUnit = new BatchDataUnit(timeMilliseconds, jobInfo);
		
		return dataUnit;
	}

}
