package pambatch.swing;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

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

	private static final String[] columnNames = {"Module", "Name", "Input", "Output", "Select", "Settings"};
	// remove configur eoption for now since it's too many settings versions. Use launch option for PAMGuard instead. 
//	private static final String[] columnNames = {"Module", "Name", "Input", "Output", "Select"};
	private BatchControl batchControl;
	private TableCellRenderer tableRenderer;
	
	private HashMap<PamDataUnit, JButton> rowButtons = new HashMap();
	private HashMap<PamDataUnit, JCheckBox> rowCheckBoxs = new HashMap();

	public TaskTableView(BatchControl batchControl, PamDataBlock<OfflineTaskDataUnit> pamDataBlock, String displayName) {
		super(pamDataBlock, displayName);
		this.batchControl = batchControl;
		getTable().addMouseListener(new MouseAction());
		 
		tableRenderer = getTable().getDefaultRenderer(JButton.class);
		getTable().setDefaultRenderer(JButton.class, new JTableButtonRenderer(tableRenderer));
		getTable().setDefaultRenderer(JCheckBox.class, new JTableButtonRenderer(tableRenderer));
	      
		
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
		if (pamModule == null) {
			System.out.println("Task with no module " + task.getLongName());
			pamModule = task.getTaskControlledUnit();
			return null;
		}
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
//			return taskSelection.selected;// && task.canRun();
			JCheckBox cb = getRowCheckBox(dataUnit);
			cb.setSelected(taskSelection.selected);
			return cb;
		case 5:
			if (task.hasSettings()) {
//				return "Configure";
				return getRowButton(dataUnit);
			}
		}
		
		return null;
	}
	
	class JTableButtonRenderer extends DefaultTableCellRenderer {
		private TableCellRenderer defaultRenderer;
		public JTableButtonRenderer(TableCellRenderer renderer) {
			defaultRenderer = renderer;
		}
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			if (column == 4) {
				int a = 6+6;
			}
			int align = row == 4 ? SwingConstants.CENTER : SwingConstants.LEFT;
			this.setHorizontalAlignment(align);
			this.setHorizontalTextPosition(SwingConstants.CENTER);
//			if (defaultRenderer instanceof DefaultTableCellRenderer) {
//				DefaultTableCellRenderer dr = (DefaultTableCellRenderer) defaultRenderer;
//				dr.setHorizontalAlignment(align);
//			}
			if(value instanceof Component) {
//				align = SwingConstants.CENTER;
//				this.setHorizontalAlignment(align);
//				if (defaultRenderer instanceof DefaultTableCellRenderer) {
//					DefaultTableCellRenderer dr = (DefaultTableCellRenderer) defaultRenderer;
//					dr.setHorizontalAlignment(align);
//				}
				return (Component)value;
			}
			return defaultRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
	}

	@Override
	public int getDataIndexForRow(int tableRow) {
		return tableRow;
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
			JCheckBox cb = getRowCheckBox(dataUnit);
			if (cb.isEnabled()) {
				TaskSelection taskSelection = batchControl.getBatchParameters().getTaskSelection(task);
				taskSelection.selected = !taskSelection.selected; // flip the value
				fireTableDataChanged();
			}
			break;
		case 5:
//			System.out.println("Open settings for task " + dataUnit.getOfflineTask().getName());
			JButton button = getRowButton(dataUnit);
			if (button == null || button.isShowing()) {
				return;
			}
			boolean changed = batchControl.taskSettings(e, dataUnit);
			if (changed) {
				enableButtons();
			}
//			if (dataUnit.getOfflineTask().hasSettings()) {
//				dataUnit.getOfflineTask().callSettings();
//			}
			break;
		}
		
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		switch(columnIndex) {
		case 4:
			return JCheckBox.class;
		case 5:
			return JButton.class;
		}
		return null;
	}

	@Override
	public void popupMenuAction(MouseEvent e, OfflineTaskDataUnit dataUnit, String colName) {
		// TODO Auto-generated method stub
		super.popupMenuAction(e, dataUnit, colName);
	}
	private JCheckBox getRowCheckBox(OfflineTaskDataUnit dataUnit) {
		JCheckBox cb = rowCheckBoxs.get(dataUnit);
		if (cb == null) {
			cb = new JCheckBox();
			rowCheckBoxs.put(dataUnit, cb);
		}
		return cb;
	}
	private JButton getRowButton(OfflineTaskDataUnit dataUnit) {
		// overkill. Don't actually need a button per row and the buttons don't 
		// need an action listener since the response if off the clcik on the table row, not the button itself, 
		// so use a single button and it's purely cosmetic. 
		JButton button = rowButtons.get(dataUnit);
		if (button == null) {
			button = new JButton("Configure Task");
			rowButtons.put(dataUnit, button);
			button.addActionListener(new ButtonAction(dataUnit));
		}
		return button;
	}

	private class ButtonAction implements ActionListener {

		private OfflineTaskDataUnit dataUnit;

		public ButtonAction(OfflineTaskDataUnit dataUnit) {
			this.dataUnit = dataUnit;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
//			System.out.println("Action for data unit " + dataUnit.getSummaryString());
		}
		
	}
	
	/**
	 * Try to enable the checkboxes based on state of settings for each task
	 */
	public void enableButtons() {
		int rows = getRowCount();
		for (int i = 0; i < rows; i++) {
			OfflineTaskDataUnit du = getDataUnit(i);
			enableTaskButton(du, i);
		}
	}

	private void enableTaskButton(OfflineTaskDataUnit du, int rowIndex) {
		OfflineTask task = du.getOfflineTask();
		boolean en = task.canRun();
		JCheckBox cb = getRowCheckBox(du);
		cb.setEnabled(en);
		if (en == false) {
			TaskSelection taskSelection = batchControl.getBatchParameters().getTaskSelection(task);
			taskSelection.selected = false;
			cb.setSelected(false);
		}
	}

	@Override
	public String getToolTipText(OfflineTaskDataUnit dataUnit, int columnIndex) {
		// see if there is information about the task, such as a error code from the task. 
		OfflineTask task = dataUnit.getOfflineTask();
		JButton button = getRowButton(dataUnit);
		/**
		 * Need to check to see if it's a Tethys task. in which case it can't really 
		 * run unless every job has a unique array identifier. This is probably better
		 * done in the process though as a prestart check since it may take a bit longer ? 
		 */
		boolean can = task.canRun();
		if (button != null) {
			// don't disable the button since we need to press it to make it so it CAN run. 
			// we should really be disabling the checkbox - but that doesn't actually exist, so we can't. 
			button.setEnabled(true);
		}
		if (can == false) {
			String err = task.whyNot();
			if (err != null) {
				return "Error! ! " + err;
			}
			else {
				return "Task cannot run (reason unknown)";
			}
		}
		// otherwise make up some standard text about each task. 
//		String str = String.format("%s %s ", task.getParentControlledUnit().getUnitName(), task.getName());
//		return str;
		return task.getLongName();
//		return super.getToolTipText(dataUnit, columnIndex);
	}

	@Override
	public void fireTableDataChanged() {
		super.fireTableDataChanged();
		enableButtons();
	}

}
