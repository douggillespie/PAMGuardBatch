package pambatch.swing;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import PamView.panel.PamPanel;
import pambatch.BatchControl;
import pambatch.config.BatchMode;
import pambatch.config.ExternalConfiguration;
import pambatch.config.SettingsObserver;
import pambatch.config.SettingsObservers;

public class TaskTablePanel implements BatchJobsPanel, SettingsObserver {

	private JPanel mainPanel;

	private BatchControl batchControl;
	
	private JComponent parent;
	
	private TaskTableView taskTableView;

	private ExternalConfiguration externalConfiguration;

	public TaskTablePanel(BatchControl batchControl, JComponent parent) {
		super();
		this.batchControl = batchControl;
		this.parent = parent;
		
		externalConfiguration = batchControl.getExternalConfiguration();
		
		taskTableView = new TaskTableView(batchControl, externalConfiguration.getTaskDataBlock(), "Offline Tasks");
		
		mainPanel = new PamPanel(new BorderLayout());
		mainPanel.setBorder(new TitledBorder("Offline tasks"));
		mainPanel.add(BorderLayout.CENTER, taskTableView.getComponent());
		
		batchControl.getSettingsObservers().addObserver(this);
	}

	@Override
	public JComponent getPanel() {
		// TODO Auto-generated method stub
		return mainPanel;
	}

	@Override
	public void updateJobsList() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void settingsUpdate(int changeType) {
		if (changeType == SettingsObservers.CHANGE_RUNMODE) {
			mainPanel.setVisible(batchControl.getBatchParameters().getBatchMode() == BatchMode.VIEWER);
		}
	}

}
