package pambatch.config;

import java.io.Serializable;
import java.util.ArrayList;

public class BatchParameters  implements Serializable, Cloneable{

	public static final long serialVersionUID = 1L;
	
	
	public boolean useThisPSFX = false;
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
	private ArrayList<BatchJobInfo> batchDataSets = new ArrayList();
	
	/**
	 * List of commands / offline tasks to run on the datasets. 
	 */
	private ArrayList<BatchCommand> batchCommands = new ArrayList();

	public BatchParameters() {
		
	}

	/**
	 * @return the useThisPSFX
	 */
	public boolean isUseThisPSFX() {
		return useThisPSFX;
	}

	/**
	 * @param useThisPSFX the useThisPSFX to set
	 */
	public void setUseThisPSFX(boolean useThisPSFX) {
		this.useThisPSFX = useThisPSFX;
	}

	/**
	 * @return the masterPSFX
	 */
	public String getMasterPSFX() {
		return masterPSFX;
	}

	/**
	 * @param masterPSFX the masterPSFX to set
	 */
	public void setMasterPSFX(String masterPSFX) {
		this.masterPSFX = masterPSFX;
	}

	/**
	 * @return the maxConcurrentJobs
	 */
	public int getMaxConcurrentJobs() {
		return maxConcurrentJobs;
	}

	/**
	 * @param maxConcurrentJobs the maxConcurrentJobs to set
	 */
	public void setMaxConcurrentJobs(int maxConcurrentJobs) {
		this.maxConcurrentJobs = maxConcurrentJobs;
	}

}
