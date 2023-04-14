package pambatch.remote;

import java.net.InetAddress;
import java.net.UnknownHostException;

import PamguardMVC.PamDataUnit;

public class RemoteAgentDataUnit extends PamDataUnit {

	private String computerName;
	private String osName;
	private String osArch;
	private int nProcessors;
	private String remoteIP;
	private boolean localMachine;
	private int runningCount;

	public RemoteAgentDataUnit(long timeMilliseconds, boolean localMachine, String remoteIP, String computerName, String osName, String osArch, int nProcessors) {
		super(timeMilliseconds);
		this.localMachine = localMachine;
		this.remoteIP = remoteIP;
		this.computerName = computerName;
		this.osName = osName;
		this.osArch = osArch;
		this.nProcessors = nProcessors;

//		String thisComputer = "This PC";
//		try {
//			// this gets the name of the computer, not an ip address, something like PC22586 for my laptop. 
//			thisComputer = InetAddress.getLocalHost().getHostName();
//			localMachine = thisComputer.equals(computerName);
//		} catch (UnknownHostException e) {
//		}
	}

	/**
	 * @return the computerName
	 */
	public String getComputerName() {
		return computerName;
	}

	/**
	 * @return the osName
	 */
	public String getOsName() {
		return osName;
	}

	/**
	 * @return the osArch
	 */
	public String getOsArch() {
		return osArch;
	}

	/**
	 * @return the nProcessors
	 */
	public int getnProcessors() {
		return nProcessors;
	}

	/**
	 * @return the remoteIP
	 */
	public String getRemoteIP() {
		return remoteIP;
	}

	/**
	 * @return the localMachine
	 */
	public boolean isLocalMachine() {
		return localMachine;
	}

	/**
	 * @return the runningCount
	 */
	public int getRunningCount() {
		return runningCount;
	}

	/**
	 * @param runningCount the runningCount to set
	 */
	public void setRunningCount(int runningCount) {
		this.runningCount = runningCount;
	}



}
