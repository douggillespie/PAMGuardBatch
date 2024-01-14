package pambatch.config;

/**
 * Batch processing mode, which can be normal mode(processes raw data) or viewer (runs a selection of offline tasks)
 * @author dg50
 *
 */
public enum BatchMode {
	NORMAL, VIEWER;

	@Override
	public String toString() {
		switch(this) {
		case NORMAL:
			return "Raw data processing (normal mode)";
		case VIEWER:
			return "Offline tasks (viewer mode)";
		default:
			break;
		}
		return null;
	}
	
	
}
