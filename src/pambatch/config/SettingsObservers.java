package pambatch.config;

import java.util.ArrayList;

/**
 * Manage everything that's wanting notified when settings
 * change. 
 * @author dg50
 *
 */
public class SettingsObservers {

	private ArrayList<SettingsObserver> observers = new ArrayList<>();
	
	public static final int CHANGE_RUNMODE = 0;
	public static final int CHANGE_CONFIG = 1;
	
	public void addObserver(SettingsObserver observer) {
		if (observers.contains(observer)) {
			return;
		}
		observers.add(observer);
	}
	
	public void removeObserver(SettingsObserver observer) {
		observers.remove(observer);
	}
	
	public void notifyObservers(int changeType) {
		for (SettingsObserver obs : observers) {
			obs.settingsUpdate(changeType);
		}
	}
	
}
