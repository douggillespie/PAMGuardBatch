package pambatch.swing;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import PamView.PamTable;
import PamView.panel.PamPanel;
import PamView.tables.SwingTableColumnWidths;
import pambatch.BatchControl;

public class TableJobsPanel implements BatchJobsPanel {

	private JPanel mainPanel;

	private BatchControl batchControl;
	
	private JComponent parent;

//	private JobTableModel jobTableModel;
	private JobsTableView jobsTableView;

//	private PamTable jobsTable;
	
	public TableJobsPanel(BatchControl batchControl, JComponent parent) {
		super();
		this.batchControl = batchControl;
		this.parent = parent;
		
		mainPanel = new PamPanel(new BorderLayout());
		mainPanel.setBorder(new TitledBorder("Job detail table"));
		
		jobsTableView = new JobsTableView(batchControl, batchControl.getBatchProcess().getBatchDataBlock(), "Jobs");

		mainPanel.add(BorderLayout.CENTER, jobsTableView.getComponent());
	}

	
	@Override
	public JComponent getPanel() {
		return mainPanel;
	}

	@Override
	public void updateJobsList() {
		// TODO Auto-generated method stub

	}
	

}
