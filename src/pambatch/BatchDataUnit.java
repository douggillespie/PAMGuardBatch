package pambatch;

import PamguardMVC.PamDataUnit;
import pambatch.config.BatchJobInfo;

/**
 * Batch job data will be held in data units which will make management easier and 
 * io to the database. When each is created it will immediately generate a database 
 * record and get the database Id which will be unique and used to identify it for 
 * perpetuity. Low rate, so can mess a bit with making sure we correctly read index, etc.  
 * @author dg50
 *
 */
public class BatchDataUnit extends PamDataUnit {

	private BatchJobInfo batchJobInfo;
	
	private BatchDataUnit conflictingJob;
	
	public BatchDataUnit(long timeMilliseconds, BatchJobInfo batchJobInfo) {
		super(timeMilliseconds);
		this.setBatchJobInfo(batchJobInfo);
	}

	/**
	 * @return the batchJobInfo
	 */
	public BatchJobInfo getBatchJobInfo() {
		return batchJobInfo;
	}

	/**
	 * @param batchJobInfo the batchJobInfo to set
	 */
	public void setBatchJobInfo(BatchJobInfo batchJobInfo) {
		this.batchJobInfo = batchJobInfo;
	}

	/**
	 * @return the conflictingJob
	 */
	public BatchDataUnit getConflictingJob() {
		return conflictingJob;
	}

	/**
	 * @param conflictingJob the conflictingJob to set
	 */
	public void setConflictingJob(BatchDataUnit conflictingJob) {
		this.conflictingJob = conflictingJob;
	}


}
