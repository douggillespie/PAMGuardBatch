package pambatch.tasks;

import PamguardMVC.PamDataUnit;
import offlineProcessing.OfflineTask;

public class OfflineTaskDataUnit extends PamDataUnit {

	private OfflineTask offlineTask;

	public OfflineTaskDataUnit(long timeMilliseconds, OfflineTask offlineTask) {
		super(timeMilliseconds);
		this.offlineTask = offlineTask;
	}

	/**
	 * @return the offlineTask
	 */
	public OfflineTask getOfflineTask() {
		return offlineTask;
	}

}
