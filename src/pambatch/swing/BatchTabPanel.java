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
import PamView.panel.SplitPanePositioner;
import pambatch.BatchControl;
import pambatch.config.BatchParameters;

public class BatchTabPanel implements PamTabPanel {

	private BatchControl batchControl;
	
	private PamPanel mainPanel;
		
	private BatchJobsPanel jobsPanel;

	private PSFXControlPanel psfxPanel;
	
	private JobControlPanel jobControlPanel;
	
	private JPanel splitNorth, splitSouthN, splitSouthS;

	private AgentControlPanel agentControlPanel;
	
	private TaskTablePanel taskTablePanel;

	private JSplitPane splitPane;

	private JSplitPane southSplitPane;

	public BatchTabPanel(BatchControl batchControl) {
		this.batchControl = batchControl;
		mainPanel = new PamPanel(new BorderLayout());
		psfxPanel = new PSFXControlPanel(batchControl);
		taskTablePanel = new TaskTablePanel(batchControl, null);
//		batchControlPanel = new BatchControlPanel(batchControl);
		jobControlPanel = new JobControlPanel(batchControl);
		PamPanel northPanel = new PamPanel(new BorderLayout());
		northPanel.add(BorderLayout.WEST, jobControlPanel);
		northPanel.add(BorderLayout.CENTER, psfxPanel);
		mainPanel.add(BorderLayout.NORTH, northPanel);
		JPanel southPanel = new PamPanel(new BorderLayout());
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		southSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		
		splitPane.add(splitNorth= new PamPanel(new BorderLayout()));
		splitPane.add(southSplitPane);
		southSplitPane.add(splitSouthN= new PamPanel(new BorderLayout()));
		southSplitPane.add(splitSouthS= new PamPanel(new BorderLayout()));
//		splitPane.add(taskTablePanel.getPanel());
		setJobsPanel(new TableJobsPanel(batchControl, mainPanel));
		mainPanel.add(BorderLayout.CENTER, splitPane);
		
		agentControlPanel = new AgentControlPanel(batchControl);
		splitNorth.add(BorderLayout.CENTER, agentControlPanel.getPanel());
		splitSouthS.add(BorderLayout.CENTER, taskTablePanel.getPanel());

//		SwingUtilities.invokeLater(new Runnable() {
//			@Override
//			public void run() {
//				splitPane.setDividerLocation(0.3);
////				southSplitPane.setDividerLocation(0.6);
//			}
//		});
		new SplitPanePositioner("Batch Split Pane 1", splitPane, 0.3);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new SplitPanePositioner("Batch Split Pane 2", southSplitPane, 0.5);
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
		boolean first = jobsPanel == null;
		if (this.jobsPanel != null) {
			splitSouthN.remove(this.jobsPanel.getPanel());
		}
		this.jobsPanel = jobsPanel;
		splitSouthN.add(BorderLayout.CENTER, jobsPanel.getPanel());
//		if (first) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
			southSplitPane.setDividerLocation(0.55);
			}
		});
//		}
	}

	public void setParams(BatchParameters batchParameters) {
		jobControlPanel.setParams(batchParameters);
		psfxPanel.setParams(batchParameters);
		
	}

}
