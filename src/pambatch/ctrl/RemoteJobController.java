package pambatch.ctrl;

import java.util.ArrayList;

import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.remote.RemoteAgentDataUnit;

/**
 * Job controller for a remote PC. Communication is via PamDog on the remote computer. 
 * @author dg50
 *
 */
public class RemoteJobController extends JobController {

	public RemoteJobController(BatchControl batchControl, RemoteAgentDataUnit remoteAgent, BatchDataUnit batchDataUnit, JobMonitor jobMonitor) {
		super(batchControl, remoteAgent, batchDataUnit, jobMonitor);
	}

	@Override
	public boolean launchJob(ArrayList<String> pamguardOptions) {
		/*
		 *  need to first check that the remote computer can see the data folders before asking it to do 
		 *  anything. If it can't, then return false.  
		 */
		
		return false;
	}

//	@Override
//	public void killJob() {
//		// TODO Auto-generated method stub
//		
//	}

}
