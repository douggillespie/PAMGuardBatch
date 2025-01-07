package pambatch;

public enum BatchJobStatus {
	
	NOTSTARTED, READY, RUNNING, COMPLETE, UNKNOWN, CANCELLED, STARTING;

	@Override
	public String toString() {
		switch (this) {
		case COMPLETE:
			return "Complete";
		case NOTSTARTED:
			return "Not Started";
		case READY:
			return "Ready";
		case RUNNING:
			return "Running";
		case CANCELLED:
			return "Cancelled";
		case STARTING:
			return "Starting";
		case UNKNOWN:
			return "Unknown";
		default:
			return super.toString();
		}
	}
	
	/**
	 * Overrides the valueof function to also work with the formatted string types .
	 * @param str
	 * @return
	 */
	public static BatchJobStatus getValue(String str) {
		if (str == null) {
			return null;
		}
		try {
			BatchJobStatus stVal = BatchJobStatus.valueOf(str);
			if (stVal != null) {
				return stVal;
			}
		}
		catch (Exception e) {
			
		}
		// try the formatted strings. 
		BatchJobStatus[] values = BatchJobStatus.values();
		for (int i = 0; i < values.length; i++) {
			if (values[i].toString().equals(str)) {
				return values[i];
			}
		}
		return BatchJobStatus.UNKNOWN;
	}
	
}
