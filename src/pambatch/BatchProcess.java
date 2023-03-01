package pambatch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import PamguardMVC.PamProcess;
import pambatch.logging.BatchLogging;

public class BatchProcess extends PamProcess {

	private BatchControl batchControl;
	
	private BatchDataBlock batchDataBlock;
	
	private BatchLogging batchLogging;

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
			for (BatchDataUnit batchDataUnit : batchJobs) {
				ArrayList<String> commands = batchControl.getBatchJobLaunchCommand(batchDataUnit.getBatchJobInfo());
				
				if (commands != null) {
					String command = makeOneLinecommand(commands);
					System.out.println(command);
//					final ProcessBuilder builder = new ProcessBuilder(commands);
//					try {
//						builder.start();
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					try {
						Process proc = Runtime.getRuntime().exec(makeCommandArray(commands));
						createProcessMonitor(batchDataUnit, proc);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
//				break;
			}
		}
		
	}
	
	private String[] makeCommandArray(ArrayList<String> commands) {
		String[] command = new String[commands.size()];
		for (int i = 0; i < commands.size(); i++) {
			command[i] = commands.get(i);
		}
		return command;
	}
	
	public void createProcessMonitor(BatchDataUnit batchDataUnit, Process proc) {
		ProcessMonitor pm = new ProcessMonitor(batchDataUnit, proc, proc.getInputStream());
		pm.execute();
		ProcessMonitor pme = new ProcessMonitor(batchDataUnit, proc, proc.getErrorStream());
		pme.execute();
	}
	
	private class ProcessMonitor extends SwingWorker<Integer, ProcessProgress> {

		private BatchDataUnit batchDataUnit;
		
		private Process batchProcess;

		private InputStream inputStream;

		private BufferedReader bufferedReader;
		
		public ProcessMonitor(BatchDataUnit batchDataUnit, Process batchProcess, InputStream inputStream) {
			super();
			this.batchDataUnit = batchDataUnit;
			this.batchProcess = batchProcess;
			this.inputStream = inputStream;
			InputStreamReader isr = new InputStreamReader(inputStream);
			bufferedReader = new BufferedReader(isr);
		}

		@Override
		protected Integer doInBackground() throws Exception {
			String line;
//			publish(new LogCaptureMessage("Enter log capture thread"));
			try {
				while ((line = bufferedReader.readLine()) != null) {
					publish(new ProcessProgress(batchDataUnit, line));
				}
			}
			catch (IOException e) {
				publish(new ProcessProgress(batchDataUnit, e.getMessage()));
			}
			return null;
		}

		@Override
		protected void process(List<ProcessProgress> chunks) {
			for (ProcessProgress pp : chunks) {
				System.out.println(pp.getString());
			}
		}
		
	}
	
	

	private String makeOneLinecommand(ArrayList<String> commands) {
		if (commands.size() < 1) {
			return "";
		}
		String command = commands.get(0);
		for (int i = 1; i < commands.size(); i++) {
			command += " \"" + commands.get(i) + "\"";
		}
		return command;
	}

	@Override
	public void pamStop() {
		// TODO Auto-generated method stub

	}

}
