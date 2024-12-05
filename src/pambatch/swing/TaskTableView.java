package pambatch.swing;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;

import PamController.PamControlledUnit;
import PamView.component.DataBlockTableView;
import PamView.dialog.SettingsButton;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import offlineProcessing.OfflineTask;
import pambatch.BatchControl;
import pambatch.tasks.OfflineTaskDataUnit;
import pambatch.tasks.TaskSelection;

public class TaskTableView extends DataBlockTableView<OfflineTaskDataUnit> {

	private static final String[] columnNames = {"Module", "Name", "Input", "Output", "Select", "Configure"};
	private BatchControl batchControl;

	public TaskTableView(BatchControl batchControl, PamDataBlock<OfflineTaskDataUnit> pamDataBlock, String displayName) {
		super(pamDataBlock, displayName);
		this.batchControl = batchControl;
		getTable().addMouseListener(new MouseAction());
		showViewerScrollControls(false);
	}

	@Override
	public String[] getColumnNames() {
		return columnNames;
	}
	
	private class MouseAction extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getButton() == MouseEvent.BUTTON1) {
				checkButtons(e);
			}
		}
		
	}

	@Override
	public Object getColumnData(OfflineTaskDataUnit dataUnit, int column) {
		OfflineTask task = dataUnit.getOfflineTask();
		if (task == null) {
			return null;
		}
		PamControlledUnit pamModule = task.getTaskControlledUnit();
		TaskSelection taskSelection = batchControl.getBatchParameters().getTaskSelection(task);
		switch (column) {
		case 0:
			String type = pamModule.getUnitType();
			String name = pamModule.getUnitName();
			if (name.equals(type) ) {
				return name;
			}
			else {
				return type + " : " + name;
			}
		case 1:
			return task.getName();
		case 2:
			PamDataBlock primaryBlock = task.getDataBlock();
			if (primaryBlock == null) {
				return "no input data block";
			}
			else {
				return primaryBlock.getDataName();
			}
		case 3:
			return task.getAffectedBlocksList();
		case 4:
			return taskSelection.selected;
		case 5:
			if (task.hasSettings()) {
				return "Configure";
			}
		}
		
		return null;
	}

	public void checkButtons(MouseEvent e) {
		checkRowSelection(e);
		OfflineTaskDataUnit dataUnit = getDataUnit(getTable().rowAtPoint(e.getPoint()));
		if (dataUnit == null) {
			return;
		}
		OfflineTask task = dataUnit.getOfflineTask();
		int selColumn = getTable().columnAtPoint(e.getPoint());
		if (dataUnit == null || selColumn < 0) {
			return;
		}
		switch (selColumn) {
		case 4:
			TaskSelection taskSelection = batchControl.getBatchParameters().getTaskSelection(task);
			taskSelection.selected = !taskSelection.selected; // flip the value
			fireTableDataChanged();
			break;
		case 5:
			System.out.println("Open settings for task " + dataUnit.getOfflineTask().getName());
			if (dataUnit.getOfflineTask().hasSettings()) {
				dataUnit.getOfflineTask().callSettings();
			}
			break;
		}
		
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch(columnIndex) {
		case 4:
			return Boolean.class;
//		case 5:
//			return JButton.class;
		}
		return null;
	}

	@Override
	public void popupMenuAction(MouseEvent e, OfflineTaskDataUnit dataUnit, String colName) {
		// TODO Auto-generated method stub
		super.popupMenuAction(e, dataUnit, colName);
	}

}
