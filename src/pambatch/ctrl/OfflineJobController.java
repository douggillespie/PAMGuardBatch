package pambatch.ctrl;

import java.util.ArrayList;

import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.remote.RemoteAgentDataUnit;

/**
 * Job controller for offline tasks. Will require quite a different set of command line parameters
 * to the job control for normal mode. 
 * @author dg50
 *
 */
public class OfflineJobController extends JobController {

	public OfflineJobController(BatchControl batchControl, RemoteAgentDataUnit remoteAgent, BatchDataUnit batchDataUnit,
			JobMonitor jobMonitor) {
		super(batchControl, remoteAgent, batchDataUnit, jobMonitor);
	}

	@Override
	public boolean launchJob(ArrayList<String> pamguardOptions) {

		ArrayList<String> totalCommand = new ArrayList<>();
		totalCommand.add(getBatchControl().findStartExecutable());
		totalCommand.addAll(pamguardOptions);
		
		String singleLine = getOneLineCommand(totalCommand);
		
		String commandsOnly = getOneLineCommand(pamguardOptions);
		return false;
	}

}
