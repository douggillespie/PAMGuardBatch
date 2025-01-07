package pambatch.config;

import java.io.Serializable;

import Array.PamArray;
import pambatch.BatchJobStatus;

/**
 * Info for a single set of data to be hit with with batch process
 * @author dg50
 *
 */
public class BatchJobInfo implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Source folder for sound files
	 */
	public String soundFileFolder;
	
	/**
	 * Output folder for binary data
	 */
	public String outputBinaryFolder;
	
	/**
	 * Output database
	 */
	public String outputDatabaseName;
	
	/*
	 * A start command has been sent (time of)
	 */
	public long startSent = 0;

	/**
	 * Job status
	 */
	public BatchJobStatus jobStatus = BatchJobStatus.NOTSTARTED;
	
	/**
	 * Percentage of job completed (probably %% of files rather than of actual data). 
	 */
	public double percentDone;

	private int jobId2;
	
	public PamArray arrayData;
	
	
	public BatchJobInfo() {
		super();
	}

	public BatchJobInfo(String soundFileFolder, String outputBinaryFolder, String outputDatabaseName) {
		super();
		this.soundFileFolder = soundFileFolder;
		this.outputBinaryFolder = outputBinaryFolder;
		this.outputDatabaseName = outputDatabaseName;
	}


	@Override
	public BatchJobInfo clone() {
		try {
			return (BatchJobInfo) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Set the jobid2 (jobid1 is the database index of the accompanying data unit)
	 * @param jobId2
	 */
	public void setJobId2(int jobId2) {
		this.jobId2 = jobId2;
	}

	/**
	 * @return the jobId2
	 */
	public int getJobId2() {
		return jobId2;
	}

}
