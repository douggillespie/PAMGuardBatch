package pambatch.ctrl;

import java.util.ArrayList;

import pambatch.BatchControl;
import pambatch.BatchDataUnit;

public class RemoteJobController extends JobController {

	public RemoteJobController(BatchControl batchControl, BatchDataUnit batchDataUnit, JobMonitor jobMonitor) {
		super(batchControl, batchDataUnit, jobMonitor);
	}

	@Override
	public boolean launchJob(ArrayList<String> pamguardOptions) {
		return false;
	}

}
