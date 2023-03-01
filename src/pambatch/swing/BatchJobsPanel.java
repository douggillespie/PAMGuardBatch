package pambatch.swing;

import javax.swing.JComponent;

import pambatch.config.BatchParameters;

public interface BatchJobsPanel {

	public JComponent getPanel();
	
	public void updateJobsList();

	
}
