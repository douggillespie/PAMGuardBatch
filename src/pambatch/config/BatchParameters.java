package pambatch.config;

import java.io.Serializable;
import java.util.ArrayList;

public class BatchParameters  implements Serializable, Cloneable{

	public static final long serialVersionUID = 1L;
	
	/**
	 * Master psfx file
	 */
	private String masterPSFX;
	
	/**
	 * Max number of concurrent jobs that can run (per machine)
	 */
	private int maxConcurrentJobs = 3;
	
	/**
	 * List of datasets to hit with this process. 
	 */
	private ArrayList<BatchDataset> batchDataSets = new ArrayList();
	
	/**
	 * List of commands / offline tasks to run on the datasets. 
	 */
	private ArrayList<BatchCommand> batchCommands = new ArrayList();

	public BatchParameters() {
		
	}

}
