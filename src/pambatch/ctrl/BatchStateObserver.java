package pambatch.ctrl;

public interface BatchStateObserver {

	/**
	 * Something has changed worth telling other classes in the batch system about. 
	 * @param batchState state change
	 * @param data data appropriate for the state change (often null)
	 */
	public void update(BatchState batchState, Object data);
	
}
