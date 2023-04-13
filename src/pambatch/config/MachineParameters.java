package pambatch.config;

import java.io.Serializable;

/**
 * Parameters for a single processing machine. Held as a hashMap within
 * the main BatchParameters class.
 * @author dg50
 *
 */
public class MachineParameters implements Serializable, Cloneable {

	private static final long serialVersionUID = 1L;
	
	private String machineName;
	
	public int maxJobs = 2;

	private boolean enabled;

	public MachineParameters(String machineName) {
		this.machineName = machineName;
	}

	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}


}
