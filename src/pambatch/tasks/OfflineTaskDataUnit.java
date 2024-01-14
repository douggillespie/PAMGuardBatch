package pambatch.tasks;

import PamguardMVC.PamDataUnit;
import offlineProcessing.OfflineTask;

public class OfflineTaskDataUnit extends PamDataUnit {

	private OfflineTask offlineTask;
	
	private boolean selected;

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

	/**
	 * @return the selected
	 */
	public boolean isSelected() {
		return selected;
	}

	/**
	 * @param selected the selected to set
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}


}
