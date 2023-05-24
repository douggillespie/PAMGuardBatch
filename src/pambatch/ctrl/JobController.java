package pambatch.ctrl;

import java.util.ArrayList;

import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.BatchJobStatus;
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
		if (remoteAgent.isLocalMachine()) {
			jobController = new LocalJobController(batchControl, remoteAgent, batchDataUnit, jobMonitor);
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

	public String makeOneLinecommand(ArrayList<String> commands) {
		if (commands.size() < 1) {
			return "";
		}
		String command = commands.get(0);
		for (int i = 1; i < commands.size(); i++) {
			command += " \"" + commands.get(i) + "\"";
		}
		return command;
	}

	/**
	 * @return the remoteAgent
	 */
	public RemoteAgentDataUnit getRemoteAgent() {
		return remoteAgent;
	}

//	public abstract void killJob();
}
