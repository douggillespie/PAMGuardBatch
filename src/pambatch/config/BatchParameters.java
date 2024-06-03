package pambatch.config;

import java.io.Serializable;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import PamController.PamControlledUnit;
import PamController.fileprocessing.ReprocessManager;
import PamController.fileprocessing.ReprocessStoreChoice;
import offlineProcessing.OfflineTask;
import pambatch.BatchControl;
import pambatch.remote.NetInterfaceFinder;
import pambatch.tasks.TaskSelection;

public class BatchParameters  implements Serializable, Cloneable{

	public static final long serialVersionUID = 1L;
	
	public static final int defaultMulticastPort = 12346;
	
	public static final String defaultMulticastAddr = "230.1.1.1";
	
	private BatchMode batchMode = BatchMode.NORMAL;
	
	public boolean useThisPSFX = false;
	/**
	 * Master psfx file
	 */
	private String masterPSFX;
	
//	/**
//	 * Max number of concurrent jobs that can run (per machine)
//	 */
//	private int maxConcurrentJobs = 3;
	
	/**
	 * Port ID for multicast comms with many jobs
	 */
	private int multicastPort = defaultMulticastPort;
	
	/**
	 * Address for multicast comms with many jobs
	 */
	private String multicastAddress = defaultMulticastAddr;
	
	private boolean noGUI;
	
	private HashMap<String, MachineParameters> machineParameters = new HashMap<>();
	
	private String networkInterfaceName;
	
	private HashMap<String, TaskSelection> offlineTasks = new HashMap<>();
	
//	/**
//	 * List of datasets to hit with this process. 
//	 */
//	private ArrayList<BatchJobInfo> batchDataSets = new ArrayList();
//	
//	/**
//	 * List of commands / offline tasks to run on the datasets. 
//	 */
//	private ArrayList<BatchCommand> batchCommands = new ArrayList();
	
	private ReprocessStoreChoice reprocessChoice = ReprocessStoreChoice.CONTINUECURRENTFILE;

	public BatchParameters() {
		
	}

	/**
	 * @return the useThisPSFX
	 */
	public boolean isUseThisPSFX() {
		return useThisPSFX;
	}

	/**
	 * @param useThisPSFX the useThisPSFX to set
	 */
	public void setUseThisPSFX(boolean useThisPSFX) {
		this.useThisPSFX = useThisPSFX;
	}

	/**
	 * @return the masterPSFX
	 */
	public String getMasterPSFX() {
		return masterPSFX;
	}

	/**
	 * @param masterPSFX the masterPSFX to set
	 */
	public void setMasterPSFX(String masterPSFX) {
		this.masterPSFX = masterPSFX;
	}

//	/**
//	 * @return the maxConcurrentJobs
//	 */
//	public int getMaxConcurrentJobs() {
//		return maxConcurrentJobs;
//	}
//
//	/**
//	 * @param maxConcurrentJobs the maxConcurrentJobs to set
//	 */
//	public void setMaxConcurrentJobs(int maxConcurrentJobs) {
//		this.maxConcurrentJobs = maxConcurrentJobs;
//	}

	/**
	 * @return the multicastPort
	 */
	public int getMulticastPort() {
		if (multicastPort == 0) {
			multicastPort = defaultMulticastPort;
		}
		return multicastPort;
	}

	/**
	 * @param multicastPort the multicastPort to set
	 */
	public void setMulticastPort(int multicastPort) {
		this.multicastPort = multicastPort;
	}

	/**
	 * @return the multicastAddress
	 */
	public String getMulticastAddress() {
		if (multicastAddress == null) {
			multicastAddress = defaultMulticastAddr;
		}
		return multicastAddress;
	}

	/**
	 * @param multicastAddress the multicastAddress to set
	 */
	public void setMulticastAddress(String multicastAddress) {
		this.multicastAddress = multicastAddress;
	}

	/**
	 * @return the reprocessChoice
	 */
	public ReprocessStoreChoice getReprocessChoice() {
		if (reprocessChoice == null) {
			reprocessChoice = ReprocessStoreChoice.CONTINUECURRENTFILE;
		}
		return reprocessChoice;
	}

	/**
	 * @param reprocessChoice the reprocessChoice to set
	 */
	public void setReprocessChoice(ReprocessStoreChoice reprocessChoice) {
		this.reprocessChoice = reprocessChoice;
	}

	/**
	 * @return the noGUI
	 */
	public boolean isNoGUI() {
		return noGUI;
	}

	/**
	 * @param noGUI the noGUI to set
	 */
	public void setNoGUI(boolean noGUI) {
		this.noGUI = noGUI;
	}
	
	/**
	 * Set parameters for a specified machine. 
	 * @param machineName
	 * @param machineParameters
	 */
	public void setMachineParameters(String machineName, MachineParameters machineParameters) {
		if (this.machineParameters == null) {
			this.machineParameters = new HashMap<>();
		}
		this.machineParameters.put(machineName, machineParameters);
	}
	
	/**
	 * Get parameters for a specified machine. Create if needed. 
	 * @param machineName
	 * @return
	 */
	public MachineParameters getMachineParameters(String machineName) {
		if (machineName == null) {
			return null;
		}
		if (this.machineParameters == null) {
			this.machineParameters = new HashMap<>();
		}
		MachineParameters params = this.machineParameters.get(machineName);
		if (params == null) {
			params = new MachineParameters(machineName);
			if (machineName.equals(BatchControl.getLocalMachineName())) {
				params.setEnabled(true);
			}
			setMachineParameters(machineName, params);
		}
		return params;
	}

	/**
	 * @return the networkInterfaceName
	 */
	public String getNetworkInterfaceName() {
		if (networkInterfaceName == null) {
			// default to first in list which will probably be loopback. 
			List<NetworkInterface> infNames = NetInterfaceFinder.getIPV4Interfaces();
			if (infNames.size() > 0) {
				networkInterfaceName = infNames.get(0).getName();
			}
		}
		return networkInterfaceName;
	}

	/**
	 * @param networkInterfaceName the networkInterfaceName to set
	 */
	public void setNetworkInterfaceName(String networkInterfaceName) {
		this.networkInterfaceName = networkInterfaceName;
	}

	/**
	 * @return the batchMode
	 */
	public BatchMode getBatchMode() {
		if (batchMode == null) {
			batchMode = BatchMode.NORMAL;
		}
		return batchMode;
	}

	/**
	 * @param batchMode the batchMode to set
	 */
	public void setBatchMode(BatchMode batchMode) {
		this.batchMode = batchMode;
	}
	
	/**
	 * Set task selection parameters. 
	 * @param offlineTask
	 * @param taskSelection
	 */
	public void setTaskSelection(OfflineTask offlineTask, TaskSelection taskSelection) {
		if (offlineTasks == null) {
			offlineTasks = new HashMap<String, TaskSelection>();
		}
		offlineTasks.put(offlineTask.getLongName(), taskSelection);
	}
	
	/**
	 * Get offline task selection information. Will return a default empty 
	 * class if one is not already stored in the hashmap
	 * @param offlineTask 
	 * @return task parameters
	 */
	public TaskSelection getTaskSelection(OfflineTask offlineTask) {
		if (offlineTasks == null) {
			offlineTasks = new HashMap<String, TaskSelection>();
		}
		TaskSelection sel = offlineTasks.get(offlineTask.getLongName());
		if (sel == null) {
			sel = new TaskSelection(false);
			setTaskSelection(offlineTask, sel);
		}
		return sel;
	}
	
}
