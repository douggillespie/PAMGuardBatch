package pambatch.ctrl;

import java.util.ArrayList;

import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.BatchJobStatus;
import pambatch.config.BatchMode;
import pambatch.remote.RemoteAgentDataUnit;

/**
 * Controller for a <em>SINGLE</em> batch object. This will launch the job on 
 * the appropriate machine with the appropriate feedback and send updates
 * back into the system through calls to updateJobStatus. 
 * @author dg50
 *
 */
public abstract class JobController {
	
	private BatchControl batchControl;

	private BatchDataUnit batchDataUnit;
	
	private ArrayList<JobMonitor> jobMonitors = new ArrayList<>();

	private RemoteAgentDataUnit remoteAgent;

	public JobController(BatchControl batchControl, RemoteAgentDataUnit remoteAgent, BatchDataUnit batchDataUnit, JobMonitor jobMonitor) {
		super();
		this.batchControl = batchControl;
		this.remoteAgent = remoteAgent;
		this.batchDataUnit = batchDataUnit;
		if (jobMonitor != null) {
			jobMonitors.add(jobMonitor);
		}
	}
	
	/**
	 * Get the appropriate type of batch controller. This will eventually need
	 * more options when we work out how to use multiple machines, so for now
	 * just returns the default LocalJobControl class. 
	 * @param batchControl
	 * @param remoteAgent 
	 * @param batchDataUnit
	 * @return
	 */
	public static JobController getJobController(BatchControl batchControl, RemoteAgentDataUnit remoteAgent, BatchDataUnit batchDataUnit, JobMonitor jobMonitor) {
		JobController jobController = null;
		BatchMode mode = batchControl.getBatchParameters().getBatchMode();
		if (remoteAgent.isLocalMachine()) {
			if (mode == BatchMode.NORMAL) {
				jobController = new LocalJobController(batchControl, remoteAgent, batchDataUnit, jobMonitor);
			}
			else {
				jobController = new OfflineJobController(batchControl, remoteAgent, batchDataUnit, jobMonitor);
			}
			batchDataUnit.setJobController(jobController);
		}
		return jobController;
	}

	public abstract boolean launchJob(ArrayList<String> pamguardOptions);
	
	protected void updateJobStatus() {
		batchControl.getBatchProcess().getBatchDataBlock().updatePamData(batchDataUnit, System.currentTimeMillis());
		for (JobMonitor jobMon : jobMonitors) {
			jobMon.updateJobStatus(batchDataUnit);
		}
	}

	/**
	 * @return the batchControl
	 */
	public BatchControl getBatchControl() {
		return batchControl;
	}

	/**
	 * @return the batchDataUnit
	 */
	public BatchDataUnit getBatchDataUnit() {
		return batchDataUnit;
	}

	/**
	 * Turn the array list of commands into a single string with each
	 * parameter encapsulated in "" so that file paths with spaces don't break. 
	 * @param totalCommand
	 * @return
	 */
	public String getOneLineCommand(ArrayList<String> totalCommand) {
		if (totalCommand == null || totalCommand.size() == 0) {
			return null;
		}
		String oneLine = totalCommand.get(0);
		for (int i = 1; i < totalCommand.size(); i++) {
			if (oneLine.endsWith(" ") == false) {
				oneLine += " ";
			}
			String bit = totalCommand.get(i);
			bit = bit.trim();
			if (bit.contains(" ")) {
				bit = "\"" + bit + "\"";
			}
			oneLine += bit;
		}
		return oneLine;
	}
	
	/**
	 * @return the remoteAgent
	 */
	public RemoteAgentDataUnit getRemoteAgent() {
		return remoteAgent;
	}

//	public abstract void killJob();
}
