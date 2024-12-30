package pambatch.ctrl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import PamController.command.StopCommand;
import PamView.dialog.warn.WarnOnce;
import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.BatchJobStatus;
import pambatch.ProcessProgress;
import pambatch.comms.BatchMulticastController;
import pambatch.config.BatchJobInfo;
import pambatch.remote.RemoteAgentDataUnit;

public class LocalJobController extends JobController {

	private Process jobProcess;

	public LocalJobController(BatchControl batchControl, RemoteAgentDataUnit remoteAgent, BatchDataUnit batchDataUnit, JobMonitor jobMonitor) {
		super(batchControl, remoteAgent, batchDataUnit, jobMonitor);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean launchJob(ArrayList<String> pamguardOptions) {
		ArrayList<String> totalCommand = new ArrayList<>();
		totalCommand.addAll(getBatchControl().findStartExecutable());
		totalCommand.addAll(pamguardOptions);
		
		String singleLine = getOneLineCommand(totalCommand);

		jobProcess = null;
		try {
			jobProcess = Runtime.getRuntime().exec(singleLine); // should change to a command array. 
		} catch (IOException e1) {
			e1.printStackTrace();
			return false;
		}
		
		
		/*
		 *  seems to have launched, so set up monitor threads to read the 
		 *  output and error streams of the process. 
		 */
		
		InputStreamMonitor errMon = new InputStreamMonitor("Error", jobProcess.getErrorStream());
		Thread t = new Thread(errMon);
		t.start();

		InputStreamMonitor ipMon = new InputStreamMonitor("Input", jobProcess.getInputStream());
		t = new Thread(ipMon);
		t.start();

		/*
		 * Don't do this, because if PAMGuard takes a very long time to start, it may be blocking and
		 * won't respond to status requests. The checks within the batch controller may therefore think
		 * it's crashed and restart it, creating chaos. The status will be set to running when 
		 * it responds to a status request.  
		 */
//		getBatchDataUnit().getBatchJobInfo().jobStatus = BatchJobStatus.RUNNING;
//		updateJobStatus();

		return true;
	}
	

	private class InputStreamMonitor implements Runnable {

		private String streamType;
		
		private InputStream inputStream;
		
		public InputStreamMonitor(String streamType, InputStream inputStream) {
			super();
			this.streamType = streamType;
			this.inputStream = inputStream;
		}

		@Override
		public void run() {
			InputStreamReader isr = new InputStreamReader(inputStream);
			BufferedReader bufferedReader = new BufferedReader(isr);
			String line;
			try {
				while ((line = bufferedReader.readLine()) != null) {
//					publish(new ProcessProgress(batchDataUnit, line));
					System.out.printf("Job %d: %s: %s\n", getBatchDataUnit().getDatabaseIndex(), streamType, line);
				}
				/*
				 *  will exit to here when the process ends without throwing exception so shouldn't 
				 *  really use this to set complete. Need to do that using a more sophisticated 
				 *  process monitor. 
				 */
//				getBatchDataUnit().getBatchJobInfo().jobStatus = BatchJobStatus.COMPLETE;
			}
			catch (IOException e) {
//				getBatchDataUnit().getBatchJobInfo().jobStatus = BatchJobStatus.COMPLETE;
			}
			updateJobStatus();
			
		}
		
	}

//	@Override
//	public void killJob() {
//		if (jobProcess == null) {
//			return;
//		}
//		String msg = String.format("Are you sure you want to kill job id %d on %s?", getBatchDataUnit().getDatabaseIndex(),
//				this.getRemoteAgent().getComputerName());
//		int ans = WarnOnce.showWarning("Stop batch job", msg, WarnOnce.OK_CANCEL_OPTION);
//		if (ans == WarnOnce.CANCEL_OPTION) {
//			return;
//		}
//		// launch a separate thread to kill the job. It may not work, but 
//		// worth a try.
//		Thread t = new Thread(new Runnable() {
//			
//			@Override
//			public void run() {
//				killJobThread();
//			}
//		}, "Killing batch job");
//	}
//
//	protected void killJobThread() {
//		Process locProc = jobProcess;
//		if (locProc == null) {
//			return;
//		}
//		long t1 = System.currentTimeMillis();
//		// ask nicely for it to stop, then if that doesn't work in a few
//		// seconds, kill it. 
//		BatchMulticastController mcController = getBatchControl().getMulticastController();
//		BatchJobInfo jobInfo = getBatchDataUnit().getBatchJobInfo();
//		int id1 = getBatchDataUnit().getDatabaseIndex();
//		mcController.targetCommand(id1, jobInfo.getJobId2(), "stop");
////		while (System.currentTimeMillis() - t1 < 4000) {
////			mcController.targetCommand(id1, jobInfo.getJobId2(), "status");
////		}
//	}

}
