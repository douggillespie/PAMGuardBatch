package pambatch.ctrl;

import pambatch.BatchDataUnit;

public interface JobMonitor {

	public void updateJobStatus(BatchDataUnit batchDataUnit);
	
}
