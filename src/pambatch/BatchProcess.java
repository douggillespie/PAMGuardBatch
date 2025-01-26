package pambatch;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;
import javax.swing.Timer;

import PamController.PamController;
import PamController.command.BatchStatusCommand;
import PamView.dialog.warn.WarnOnce;
import PamguardMVC.PamProcess;
import pambatch.comms.BatchMulticastController;
import pambatch.config.BatchJobInfo;
import pambatch.config.BatchMode;
import pambatch.config.MachineParameters;
import pambatch.ctrl.JobController;
import pambatch.ctrl.JobMonitor;
import pambatch.logging.BatchLogging;
import pambatch.remote.RemoteAgentDataUnit;
import warnings.PamWarning;
import warnings.WarningSystem;

public class BatchProcess extends PamProcess implements JobMonitor {

	private BatchControl batchControl;

	private BatchDataBlock batchDataBlock;

	private BatchLogging batchLogging;

	private volatile boolean keepProcessing;

	private Timer jobCheckTimer;

	/**
	 * Interval between checks.
	 */
	private int CHECKTIMEINTERVAL = 5000;

	public BatchProcess(BatchControl batchControl) {
		super(batchControl, null);
		this.batchControl = batchControl;
		batchDataBlock = new BatchDataBlock(this);
		batchLogging = new BatchLogging(batchControl, batchDataBlock);
		batchDataBlock.SetLogging(batchLogging);
		addOutputDataBlock(batchDataBlock);
		jobCheckTimer = new Timer(CHECKTIMEINTERVAL, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				checkAllJobStatus();
			}
		});
		jobCheckTimer.start();
	}

	protected void checkAllJobStatus() {
		// send out a message on multicast and see what comes back. 
		BatchMulticastController mcController = batchControl.getMulticastController();
		if (mcController == null) {
			return;
		}
		checkForCrashes();
		// response will be asynchronous, so don't wait for anything. 
//		System.out.println("Sending Batch job multicast command: " + BatchStatusCommand.commandId);
		mcController.sendCommand(BatchStatusCommand.commandId);
	}

	/**
	 * Check all running jobs for crashes. 
	 * A job will be deemed to have crashed if it's not had an update
	 * to it's data unit for 3x the check interval, i.e. it's failed to
	 * respond to 2 or 3 consecutive multicast status requests  
	 */
	private void checkForCrashes() {
		long now = System.currentTimeMillis();
		ArrayList<BatchDataUnit> jobUnits = batchDataBlock.getDataCopy();
		for (BatchDataUnit aJob : jobUnits) {
			BatchJobInfo jobInfo = aJob.getBatchJobInfo();
			long updateInterval = now - aJob.getLastChangeTime();
			if (jobInfo.jobStatus == BatchJobStatus.RUNNING && updateInterval > 3*CHECKTIMEINTERVAL) {
				// job has stopped responding, so set it's status as unknown. 
				jobInfo.jobStatus = BatchJobStatus.UNKNOWN;
				updateJobStatus(aJob);
			}
		}
	}

	/**
	 * @return the batchDataBlock
	 */
	public BatchDataBlock getBatchDataBlock() {
		return batchDataBlock;
	}

	/**
	 * @return the batchLogging
	 */
	public BatchLogging getBatchLogging() {
		return batchLogging;
	}

	@Override
	public void pamStart() {
		/**
		 * Start a thread to handle all the batch jobs. this may need
		 * to launch other threads to capture output, etc. 
		 */
		BatchWorker batchWorker = new BatchWorker();
		batchWorker.execute();
		keepProcessing = true;
	}

	private class BatchWorker extends SwingWorker<Integer, BatchDataUnit> {

		@Override
		protected Integer doInBackground() throws Exception {
			try {
				runBatchJobs();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			return 0;
		}

		private void runBatchJobs() {
			ArrayList<BatchDataUnit> batchJobs = batchDataBlock.getDataCopy();
			int totalJobs = batchJobs.size();
			ArrayList<RemoteAgentDataUnit> machines = batchControl.getRemoteAgentHandler().getRemoteAgentDataBlock().getDataCopy();
			while (countJobState(BatchJobStatus.COMPLETE, null) < totalJobs && keepProcessing) {
				for (RemoteAgentDataUnit aPC : machines) {
					checkMachineJobs(aPC, batchJobs);
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					//						e.printStackTrace();
				}
			}
		}
		
		/**
		 * Check jobs running on a machine and start another if it's not busy enough 
		 * @param remoteAgent
		 * @param batchJobs
		 * @return true if a new job was started, false otherwise. 
		 */
		private boolean checkMachineJobs(RemoteAgentDataUnit remoteAgent, ArrayList<BatchDataUnit> batchJobs) {
			MachineParameters machineParams = batchControl.getBatchParameters().getMachineParameters(remoteAgent.getComputerName());
			// get number running AND starting, otherwise it starts the whole lot in one go.
			int nRunning = countActiveJobs(remoteAgent);
//			checkAgentStatus(remoteAgent);
//			if (nRunning != remoteAgent.getRunningCount()) {
//			System.out.println("Set running count to " + nRunning);
//			remoteAgent.setRunningCount(nRunning);
//			
//			}
			
			if (machineParams.isEnabled() == false) {
				return false;
			}
			if (nRunning >= machineParams.maxJobs) {////batchControl.getBatchParameters().getMaxConcurrentJobs()) {
				return false;
			}
			BatchDataUnit nextJob = findNextNotStarted();
			boolean newLaunch = false;
			if (nextJob != null) {
				JobController jobControl = JobController.getJobController(batchControl, remoteAgent, nextJob, BatchProcess.this);
				if (jobControl == null) {
					return false;
				}
				ArrayList<String> commands = batchControl.getBatchJobLaunchParams(nextJob);
				if (jobControl.launchJob(commands)) {
					nextJob.getBatchJobInfo().jobStatus = BatchJobStatus.STARTING;
					updateJobStatus(nextJob);
					newLaunch = true;
					remoteAgent.setRunningCount(nRunning+1);
				}
				
			}

			return newLaunch;
		}

		@Override
		protected void done() {
			PamController.getInstance().pamEnded();
			PamController.getInstance().pamStop();
			String msg = "Batch processing is complete. Nothing more to do";
			WarnOnce.showWarning("Batch Procesing", msg, WarnOnce.WARNING_MESSAGE);
		}

	}

	/**
	 * 
	 * @return number of running jobs. 
	 */
	private int countRunningJobs(RemoteAgentDataUnit remoteAgent) {
		return countJobState(BatchJobStatus.RUNNING, remoteAgent);
	}
	
	/**
	 * Get the number of active jobs - that's the number starting and running. 
	 * @param remoteAgent
	 * @return
	 */
	private int countActiveJobs(RemoteAgentDataUnit remoteAgent) {
		return countJobState(BatchJobStatus.RUNNING, remoteAgent) + countJobState(BatchJobStatus.STARTING, remoteAgent);
	}

	/**
	 * Find the next job that hasn't started yet.
	 * @return next job to run. 
	 */
	private BatchDataUnit findNextNotStarted() {
		List<BatchDataUnit> jobList = batchDataBlock.copyDataList();
		int n = 0;
		for (BatchDataUnit aJob : jobList) {
			BatchJobStatus jobStatus = aJob.getBatchJobInfo().jobStatus;
			if (jobStatus == BatchJobStatus.NOTSTARTED || jobStatus == BatchJobStatus.UNKNOWN || jobStatus == null) {
				return aJob;
			}
		}
		return null;
	}

	/**
	 * Count the number of jobs in a given state.
	 * @param jobState job state to count
	 * @param remoteAgent  specific PC for job count. Null will return a count of all jobw. 
	 * @return
	 */
	private int countJobState(BatchJobStatus jobState, RemoteAgentDataUnit remoteAgent) {
		List<BatchDataUnit> jobList = batchDataBlock.copyDataList();
		int n = 0;
		for (BatchDataUnit aJob : jobList) {
			if (remoteAgent != null && aJob.getJobController() != null && aJob.getJobController().getRemoteAgent() != remoteAgent) {
				continue;
			}
			if (aJob.getBatchJobInfo().jobStatus == jobState) {
				n++;
			}
		}
		return n;
	}

	private String[] makeCommandArray(ArrayList<String> commands) {
		String[] command = new String[commands.size()];
		for (int i = 0; i < commands.size(); i++) {
			command[i] = commands.get(i);
		}
		return command;
	}

	//	public void createProcessMonitor(BatchDataUnit batchDataUnit, Process proc) {
	//		ProcessMonitor pm = new ProcessMonitor(batchDataUnit, proc, proc.getInputStream());
	//		pm.execute();
	//		ProcessMonitor pme = new ProcessMonitor(batchDataUnit, proc, proc.getErrorStream());
	//		pme.execute();
	//	}
	//	
	//	private class ProcessMonitor extends SwingWorker<Integer, ProcessProgress> {
	//
	//		private BatchDataUnit batchDataUnit;
	//		
	//		private Process batchProcess;
	//
	//		private InputStream inputStream;
	//
	//		private BufferedReader bufferedReader;
	//		
	//		public ProcessMonitor(BatchDataUnit batchDataUnit, Process batchProcess, InputStream inputStream) {
	//			super();
	//			this.batchDataUnit = batchDataUnit;
	//			this.batchProcess = batchProcess;
	//			this.inputStream = inputStream;
	//			InputStreamReader isr = new InputStreamReader(inputStream);
	//			bufferedReader = new BufferedReader(isr);
	//		}
	//
	//		@Override
	//		protected Integer doInBackground() throws Exception {
	//			String line;
	////			publish(new LogCaptureMessage("Enter log capture thread"));
	//			try {
	//				while ((line = bufferedReader.readLine()) != null) {
	//					publish(new ProcessProgress(batchDataUnit, line));
	//				}
	//			}
	//			catch (IOException e) {
	//				publish(new ProcessProgress(batchDataUnit, e.getMessage()));
	//			}
	//			return null;
	//		}
	//
	//		@Override
	//		protected void process(List<ProcessProgress> chunks) {
	//			for (ProcessProgress pp : chunks) {
	//				System.out.println(pp.getString());
	//			}
	//		}
	//		
	//	}
	//	


	@Override
	public void pamStop() {
		keepProcessing = false;
	}

	@Override
	public void updateJobStatus(BatchDataUnit batchDataUnit) {
		// check the agent too. 
		JobController ctrler = batchDataUnit.getJobController();
		if (ctrler == null) {
			return;
		}
		checkAgentStatus(ctrler.getRemoteAgent());
		
		batchDataBlock.updatePamData(batchDataUnit, System.currentTimeMillis());
	}

	public void checkAgentStatus(RemoteAgentDataUnit remoteAgent) {
		if (remoteAgent == null) {
			return;
		}
		int nOld = remoteAgent.getRunningCount();
		int nNew = countActiveJobs(remoteAgent);
		if (nOld != nNew) {
//			System.out.printf("Set running count from %d to %d\n", nOld, nNew);
			remoteAgent.setRunningCount(nNew);
			// fires table update in GUI
			batchControl.getRemoteAgentHandler().getRemoteAgentDataBlock().updatePamData(remoteAgent, System.currentTimeMillis());
		}
	}

	@Override
	public boolean prepareProcessOK() {
		String errorMsg;
		if (batchControl.getBatchParameters().getBatchMode() == BatchMode.VIEWER) {
			errorMsg = batchControl.viewerStartChecks();
		}
		else {
			errorMsg = batchControl.normalStartChecks();
		}
		if (errorMsg == null) {
			return true;
		}
		else {
			WarnOnce.showWarning("Batch Processing Cannot Start", "Reason: " + errorMsg, WarnOnce.WARNING_MESSAGE);
			return false;
		}
	}


}
