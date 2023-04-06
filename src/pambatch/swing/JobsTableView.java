package pambatch.swing;

import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import PamView.component.DataBlockTableView;
import PamguardMVC.PamDataBlock;
import pambatch.BatchControl;
import pambatch.BatchDataUnit;
import pambatch.config.BatchJobInfo;

public class JobsTableView extends DataBlockTableView<BatchDataUnit>{
	

	private String[]  colNames = {"Id", "Source", "Binary", "Database", "Status"};
	
	private String[] tips = {"Job Id: the same as the index in the jobs database table",
			"Source of input recordings to process", "Output file destination for binary data",
			"Output database", "Job Status"};

	private BatchControl batchControl;

	public JobsTableView(BatchControl batchControl, PamDataBlock<BatchDataUnit> pamDataBlock, String displayName) {
		super(pamDataBlock, displayName);
		this.batchControl = batchControl;
		
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
		
		}
		return null;
	}

}
