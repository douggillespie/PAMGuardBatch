package pambatch.remote;

import java.util.List;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;

public class RemoteAgentDataBlock extends PamDataBlock<RemoteAgentDataUnit> {

	private RemoteAgentHandler remoteAgentHandler;

	public RemoteAgentDataBlock(RemoteAgentHandler remoteAgentHandler, PamProcess parentProcess) {
		super(RemoteAgentDataUnit.class, "Remote Agents", parentProcess, 0);
		this.remoteAgentHandler = remoteAgentHandler;
		// TODO Auto-generated constructor stub
	}
	
	public RemoteAgentDataUnit findByRemoteIp(String remoteIP) {
		List<RemoteAgentDataUnit> dataCopy = copyDataList();
		for (RemoteAgentDataUnit rad : dataCopy) {
			if (remoteIP.equals(rad.getRemoteIP())) {
				return rad;
			}
		}
		return null;
	}
	
	public RemoteAgentDataUnit findByComputerName(String computerName) {
		List<RemoteAgentDataUnit> dataCopy = copyDataList();
		for (RemoteAgentDataUnit rad : dataCopy) {
			if (computerName.equals(rad.getComputerName())) {
				return rad;
			}
		}
		return null;
	}
	
	@Override
	public void clearAll() {
//		super.clearAll();
	}

	@Override
	protected int removeOldUnitsT(long currentTimeMS) {
		return 0;
	}

	@Override
	protected int removeOldUnitsS(long mastrClockSample) {
		return 0;
	}

}
