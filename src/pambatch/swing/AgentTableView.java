package pambatch.swing;

import PamUtils.PamCalendar;
import PamView.component.DataBlockTableView;
import PamguardMVC.PamDataBlock;
import pambatch.BatchControl;
import pambatch.config.MachineParameters;
import pambatch.remote.RemoteAgentDataUnit;

public class AgentTableView extends DataBlockTableView<RemoteAgentDataUnit> {
	
	String[] columnNames = {"Computer name", "Address", "Cores", "Enabled", "Max jobs", "Active jobs", "Last update"};
	private BatchControl batchControl;

	public AgentTableView(BatchControl batchControl, PamDataBlock<RemoteAgentDataUnit> pamDataBlock) {
		super(pamDataBlock, "Processing machines");
		this.batchControl = batchControl;
	}

	@Override
	public String[] getColumnNames() {
		return columnNames;
	}

	@Override
	public Object getColumnData(RemoteAgentDataUnit dataUnit, int column) {
		switch (column) {
		case 0:
			return dataUnit.getComputerName();
		case 1:
			return dataUnit.getRemoteIP();
		case 2:
			return dataUnit.getnProcessors();
		case 3:
			return getMachineParameters(dataUnit).isEnabled();
		case 4:
			return getMachineParameters(dataUnit).maxJobs;
		case 6:
			return PamCalendar.formatDateTime(dataUnit.getLastChangeTime(), true);
		}
		return null;
	}

	private MachineParameters getMachineParameters(RemoteAgentDataUnit agentDataUnit) {
		return batchControl.getBatchParameters().getMachineParameters(agentDataUnit.getComputerName());
	}


}
