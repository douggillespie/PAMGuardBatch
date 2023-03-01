package pambatch;

public class ProcessProgress {

	private BatchDataUnit batchDataUnit;
	
	private String string;

	public ProcessProgress(BatchDataUnit batchDataUnit, String string) {
		super();
		this.batchDataUnit = batchDataUnit;
		this.string = string;
	}

	/**
	 * @return the batchDataUnit
	 */
	public BatchDataUnit getBatchDataUnit() {
		return batchDataUnit;
	}

	/**
	 * @return the string
	 */
	public String getString() {
		return string;
	}
	
	
}
