package pambatch.swing;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

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
	
	private JPanel splitNorth, splitSouth;

	private AgentControlPanel agentControlPanel;

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
		JPanel southPanel = new PamPanel(new BorderLayout());
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPane.add(splitNorth= new PamPanel(new BorderLayout()));
		splitPane.add(splitSouth= new PamPanel(new BorderLayout()));
		setJobsPanel(new TableJobsPanel(batchControl, mainPanel));
		mainPanel.add(BorderLayout.CENTER, splitPane);
		
		agentControlPanel = new AgentControlPanel(batchControl);
		splitNorth.add(BorderLayout.CENTER, agentControlPanel.getPanel());

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				splitPane.setDividerLocation(0.3);
			}
		});
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
			splitSouth.remove(this.jobsPanel.getPanel());
		}
		this.jobsPanel = jobsPanel;
		splitSouth.add(BorderLayout.CENTER, jobsPanel.getPanel());
	}

	public void setParams(BatchParameters batchParameters) {
		jobControlPanel.setParams(batchParameters);
		psfxPanel.setParams(batchParameters);
		
	}

}
