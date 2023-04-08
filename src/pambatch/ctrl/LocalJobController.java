package pambatch.ctrl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.BatchJobStatus;
import pambatch.ProcessProgress;

public class LocalJobController extends JobController {

	public LocalJobController(BatchControl batchControl, BatchDataUnit batchDataUnit, JobMonitor jobMonitor) {
		super(batchControl, batchDataUnit, jobMonitor);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean launchJob(ArrayList<String> pamguardOptions) {
		ArrayList<String> totalCommand = new ArrayList<>();
		totalCommand.add(getBatchControl().findStartExecutable());
		totalCommand.addAll(pamguardOptions);
		
		String singleLine = getOneLineCommand(totalCommand);

		Process jobProcess = null;
		try {
			jobProcess = Runtime.getRuntime().exec(singleLine);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return false;
		}
		
		
//		jobProcess.

//		final ProcessBuilder builder = new ProcessBuilder(totalCommand);
//		try {
//			jobProcess = builder.start();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return false;
//		}
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
	
	private String getOneLineCommand(ArrayList<String> totalCommand) {
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

}
