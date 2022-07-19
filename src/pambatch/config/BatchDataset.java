package pambatch.config;

import java.io.Serializable;

/**
 * Info for a set of data to be hit with with batch process
 * @author dg50
 *
 */
public class BatchDataset implements Serializable, Cloneable{

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

}
