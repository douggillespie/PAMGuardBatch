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
import pambatch.config.BatchMode;
import pambatch.config.SettingsObserver;

public class JobsTableView extends DataBlockTableView<BatchDataUnit> implements SettingsObserver {
	

	private String[]  colNames = {"Id", "Source", "Binary", "Database", "Instrument / Array",  "Status", "Updated"};
	
	// changed column order for viewer mode. 
	private int[] viewerOrder = {0, 3, 2, 1, 4, 5, 6};
	private int[] normalOrder = {0, 1, 2, 3, 4, 5, 6};
	
	private String[] viewerColumns;
	
	private String[] tips = {"Job Id: the same as the index in the jobs database table",
			"Source of input recordings to process", "Output file destination for binary data",
			"Output database", "Job specific calibration / array data", "Job Status", "Last update time"};

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
		batchControl.getSettingsObservers().addObserver(this);
		showViewerScrollControls(false);
	}

	@Override
	public void settingsUpdate(int changeType) {
		updateTable();
		fireTableStructureChanged();
	}

	protected void updateTable() {
		this.fireTableDataChanged();
	}

	@Override
	public String getColumnName(int columnIndex) {
		columnIndex = modeColumn(columnIndex);
		return colNames[columnIndex];
	}

	private int modeColumn(int column) {
		BatchMode mode = batchControl.getBatchParameters().getBatchMode();
		if (mode == BatchMode.VIEWER) {
			if (column < viewerOrder.length) {
				return viewerOrder[column];
			}
		}
		return column;
	}
	
	@Override
	public String getToolTipText(BatchDataUnit dataUnit, int columnIndex) {
		columnIndex = modeColumn(columnIndex);
		return tips[columnIndex];
	}

	@Override
	public void popupMenuAction(MouseEvent e, BatchDataUnit dataUnit, String colName) {
		JPopupMenu popMenu = batchControl.getJobsPopupMenu(dataUnit);
		if (popMenu == null) {
			return;
		}
		popMenu.show(e.getComponent(), e.getX(), e.getY());
	}
	
	/**
	 * Override default behaviour which shows reversed order in 
	 * normal mode. We don't want that. 
	 */
	@Override
	public int getDataIndexForRow(int tableRow) {
		return tableRow;
	}

	@Override
	public Object getColumnData(BatchDataUnit dataUnit, int column) {
		if (dataUnit == null) {
			return null;
		}
		column = modeColumn(column);
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
			if (jobInfo.arrayData == null) {
				return  "Default";
			}
			else {
				String array = String.format("%s-%s", jobInfo.arrayData.getInstrumentType(), jobInfo.arrayData.getInstrumentId());
				return array;
			}
		case 5:
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
		case 6: // last update time
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

	@Override
	public String[] getColumnNames() {
		return colNames;
	}

}
