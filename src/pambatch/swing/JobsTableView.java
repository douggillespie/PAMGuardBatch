package pambatch.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.Timer;

import PamUtils.PamCalendar;
import PamView.component.DataBlockTableView;
import PamguardMVC.PamDataBlock;
import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.config.BatchJobInfo;

public class JobsTableView extends DataBlockTableView<BatchDataUnit>{
	

	private String[]  colNames = {"Id", "Source", "Binary", "Database", "Status", "Updated"};
	
	private String[] tips = {"Job Id: the same as the index in the jobs database table",
			"Source of input recordings to process", "Output file destination for binary data",
			"Output database", "Job Status", "Last update time"};

	private BatchControl batchControl;

	public JobsTableView(BatchControl batchControl, PamDataBlock<BatchDataUnit> pamDataBlock, String displayName) {
		super(pamDataBlock, displayName);
		this.batchControl = batchControl;
		Timer updateTimer = new Timer(1000, new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				updateTable();
			}
		});
		updateTimer.start();
	}

	protected void updateTable() {
		this.fireTableDataChanged();
	}

	@Override
	public String[] getColumnNames() {
		return colNames;
	}

	@Override
	public String getToolTipText(BatchDataUnit dataUnit, int columnIndex) {
		return tips[columnIndex];
	}

	@Override
	public void popupMenuAction(MouseEvent e, BatchDataUnit dataUnit, String colName) {
		JPopupMenu popMenu = batchControl.getSwingPopupMenu(dataUnit);
		if (popMenu == null) {
			return;
		}
		popMenu.show(e.getComponent(), e.getX(), e.getY());
	}

	@Override
	public Object getColumnData(BatchDataUnit dataUnit, int column) {
		if (dataUnit == null) {
			return null;
		}
		BatchJobInfo jobInfo = dataUnit.getBatchJobInfo();
		switch (column) {
		case 0:
			return dataUnit.getDatabaseIndex();
		case 1:
			if (jobInfo != null) {
				return jobInfo.soundFileFolder;
			}
			break;
		case 2:
			if (jobInfo != null) {
				return jobInfo.outputBinaryFolder;
			}
			break;
		case 3:
			if (jobInfo != null) {
				return jobInfo.outputDatabaseName;
			}
			break;
		case 4:
			BatchDataUnit conflict = dataUnit.getConflictingJob();
			if (conflict != null) {
				return "Conflict with job id " + conflict.getDatabaseIndex();
			}
			if (jobInfo != null) {
				if (jobInfo.jobStatus == null) {
					return "Status unknown";
				}
				String str = String.format("%s: %3.1f%% done", jobInfo.jobStatus.toString(), jobInfo.percentDone);
				return str;
			}
			break;
		case 5: // last update time
			long interval = System.currentTimeMillis()-dataUnit.getLastChangeTime();
			if (interval < 10000) {
				return "Just now";
			}
			if (interval < 60000) {
				return String.format("%d seconds ago", interval / 1000);
			}
			else {
				return PamCalendar.formatDateTime(dataUnit.getLastChangeTime(), true);
			}
		}
		return null;
	}

}
