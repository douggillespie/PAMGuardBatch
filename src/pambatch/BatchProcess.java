package pambatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import PamguardMVC.PamProcess;
import pambatch.ctrl.JobController;
import pambatch.ctrl.JobMonitor;
import pambatch.logging.BatchLogging;

public class BatchProcess extends PamProcess implements JobMonitor {

	private BatchControl batchControl;
	
	private BatchDataBlock batchDataBlock;
	
	private BatchLogging batchLogging;

	private volatile boolean keepProcessing;

	public BatchProcess(BatchControl batchControl) {
		super(batchControl, null);
		this.batchControl = batchControl;
		batchDataBlock = new BatchDataBlock(this);
		batchLogging = new BatchLogging(batchControl, batchDataBlock);
		batchDataBlock.SetLogging(batchLogging);
		addOutputDataBlock(batchDataBlock);
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
			while (countJobState(BatchJobStatus.COMPLETE) < totalJobs && keepProcessing) {
				int nRunning = countRunningJobs();
				if (nRunning >= batchControl.getBatchParameters().getMaxConcurrentJobs()) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
//						e.printStackTrace();
					}
					continue;
				}
				BatchDataUnit nextJob = findNextNotStarted();
				if (nextJob != null) {
					JobController jobControl = JobController.getJobController(batchControl, nextJob, BatchProcess.this);
					ArrayList<String> commands = batchControl.getBatchJobLaunchParams(nextJob.getBatchJobInfo());
					if (jobControl.launchJob(commands)) {
						nextJob.getBatchJobInfo().jobStatus = BatchJobStatus.RUNNING;
					}
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
//			for (BatchDataUnit batchDataUnit : batchJobs) {
//				ArrayList<String> commands = batchControl.getBatchJobLaunchCommand(batchDataUnit.getBatchJobInfo());
//				
//				if (commands != null) {
//					String command = makeOneLinecommand(commands);
//					System.out.println(command);
////					final ProcessBuilder builder = new ProcessBuilder(commands);
////					try {
////						builder.start();
////					} catch (IOException e) {
////						// TODO Auto-generated catch block
////						e.printStackTrace();
////					}
//					try {
//						Process proc = Runtime.getRuntime().exec(makeCommandArray(commands));
//						createProcessMonitor(batchDataUnit, proc);
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//				}
//				break;
//			}
		}
		
	}
	
	/**
	 * 
	 * @return number of running jobs. 
	 */
	private int countRunningJobs() {
		return countJobState(BatchJobStatus.RUNNING);
	}
	
	/**
	 * Find the next job that hasn't started yet.
	 * @return next job to run. 
	 */
	private BatchDataUnit findNextNotStarted() {
		List<BatchDataUnit> jobList = batchDataBlock.copyDataList();
		int n = 0;
		for (BatchDataUnit aJob : jobList) {
			if (aJob.getBatchJobInfo().jobStatus == BatchJobStatus.NOTSTARTED) {
				return aJob;
			}
		}
		return null;
	}
	
	/**
	 * Count the number of jobs in a given state.
	 * @param jobState
	 * @return
	 */
	private int countJobState(BatchJobStatus jobState) {
		List<BatchDataUnit> jobList = batchDataBlock.copyDataList();
		int n = 0;
		for (BatchDataUnit aJob : jobList) {
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
		// TODO Auto-generated method stub
		batchDataBlock.notifyObservers();
	}

}
