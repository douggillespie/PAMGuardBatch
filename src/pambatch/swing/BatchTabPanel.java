package pambatch.swing;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JToolBar;

import PamView.PamTabPanel;
import PamView.panel.PamPanel;
import pambatch.BatchControl;
import pambatch.config.BatchParameters;

public class BatchTabPanel implements PamTabPanel {

	private BatchControl batchControl;
	
	private PamPanel mainPanel;
		
	private BatchJobsPanel jobsPanel;

	private PSFXControlPanel psfxPanel;
	
	private JobControlPanel jobControlPanel;

	public BatchTabPanel(BatchControl batchControl) {
		this.batchControl = batchControl;
		mainPanel = new PamPanel(new BorderLayout());
		psfxPanel = new PSFXControlPanel(batchControl);
//		batchControlPanel = new BatchControlPanel(batchControl);
		jobControlPanel = new JobControlPanel(batchControl);
		PamPanel northPanel = new PamPanel(new BorderLayout());
		northPanel.add(BorderLayout.WEST, jobControlPanel);
		northPanel.add(BorderLayout.CENTER, psfxPanel);
		mainPanel.add(BorderLayout.NORTH, northPanel);
		setJobsPanel(new TableJobsPanel(batchControl, mainPanel));
	}

	@Override
	public JMenu createMenu(Frame parentFrame) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JComponent getPanel() {
		return mainPanel;
	}

	@Override
	public JToolBar getToolBar() {
		return null;
	}

	/**
	 * @return the jobsPanel
	 */
	public BatchJobsPanel getJobsPanel() {
		return jobsPanel;
	}

	/**
	 * @param jobsPanel the jobsPanel to set
	 */
	public void setJobsPanel(BatchJobsPanel jobsPanel) {
		if (this.jobsPanel != null) {
			mainPanel.remove(this.jobsPanel.getPanel());
		}
		this.jobsPanel = jobsPanel;
		mainPanel.add(BorderLayout.CENTER, jobsPanel.getPanel());
	}

	public void setParams(BatchParameters batchParameters) {
		jobControlPanel.setParams(batchParameters);
		psfxPanel.setParams(batchParameters);
		
	}

}
