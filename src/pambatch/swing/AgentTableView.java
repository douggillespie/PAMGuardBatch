package pambatch.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

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
		showViewerScrollControls(false);
	}

	@Override
	public void popupMenuAction(MouseEvent e, RemoteAgentDataUnit dataUnit, String colName) {
		if (dataUnit == null) {
			return;
		}
		MachineParameters machineParams = batchControl.getBatchParameters().getMachineParameters(dataUnit.getComputerName());
		boolean enabled = machineParams.isEnabled();
		int maxJobs = machineParams.maxJobs;
		JPopupMenu popMenu = new JPopupMenu();
		JCheckBoxMenuItem enab = new JCheckBoxMenuItem("Enable " + dataUnit.getComputerName(), enabled);
		enab.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				machineParams.setEnabled(enab.isSelected());
				fireTableDataChanged();
			}
		});
		popMenu.add(enab);
		if (enabled) {
			JMenuItem nJobs = new JMenuItem("Reduce max jobs to " + (maxJobs-1));
			nJobs.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					machineParams.maxJobs--;
					fireTableDataChanged();
				}
			});
			nJobs.setEnabled(maxJobs > 1);
			popMenu.add(nJobs);
			
			nJobs = new JMenuItem("Increase max jobs to " + (maxJobs+1));
			nJobs.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					machineParams.maxJobs++;
					fireTableDataChanged();
				}
			});
			popMenu.add(nJobs);
		}
		
		
		popMenu.show(e.getComponent(), e.getX(), e.getY());
	}
	
	@Override
	public String[] getColumnNames() {
		return columnNames;
	}

	@Override
	public Object getColumnData(RemoteAgentDataUnit dataUnit, int column) {
		boolean isLocal = dataUnit.isLocalMachine();
		switch (column) {
		case 0:
			String name = dataUnit.getComputerName();
			if (isLocal) {
				return name + " (this PC)";
			}
			else {
				return name;
			}
		case 1:
			return dataUnit.getRemoteIP();
		case 2:
			return dataUnit.getnProcessors();
		case 3:
			return getMachineParameters(dataUnit).isEnabled();
		case 4:
			return getMachineParameters(dataUnit).maxJobs;
		case 5:
			return dataUnit.getRunningCount();
		case 6:
			long t = dataUnit.getLastChangeTime();
			return PamCalendar.formatDateTime(t, true);
		}
		return null;
	}

	private MachineParameters getMachineParameters(RemoteAgentDataUnit agentDataUnit) {
		return batchControl.getBatchParameters().getMachineParameters(agentDataUnit.getComputerName());
	}



}
