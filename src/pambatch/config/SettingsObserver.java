package pambatch.config;

/**
 * Used by most components of the back processor to get 
 * notifications when things change. 
 * @author dg50
 *
 */
public interface SettingsObserver {
	
	/**
	 * Called when there has been a significant configuration change, such as a new psfx or a mode change. 
	 */
	public void settingsUpdate(int changeType);
	
}
