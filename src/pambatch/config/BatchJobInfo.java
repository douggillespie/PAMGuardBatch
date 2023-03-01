package pambatch.config;

import java.io.Serializable;

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
	
	
	
	public BatchJobInfo() {
		super();
	}

	public BatchJobInfo(String soundFileFolder, String outputBinaryFolder, String outputDatabaseName) {
		super();
		this.soundFileFolder = soundFileFolder;
		this.outputBinaryFolder = outputBinaryFolder;
		this.outputDatabaseName = outputDatabaseName;
	}

	/**
	 * Job status
	 */
	public BatchJobStatus jobStatus = BatchJobStatus.NOTSTARTED;
	
	/**
	 * Percentage of job completed (probably %% of files rather than of actual data). 
	 */
	public double percentDone;

	@Override
	public BatchJobInfo clone() {
		try {
			return (BatchJobInfo) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}

}
