package pambatch.swing;

import java.awt.Frame;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JToolBar;

import PamView.PamTabPanel;
import pambatch.BatchControl;

public class BatchTabPanel implements PamTabPanel {

	private BatchControl batchControl;
	
	private BatchControlPanel batchControlPanel;

	public BatchTabPanel(BatchControl batchControl) {
		this.batchControl = batchControl;
		batchControlPanel = new BatchControlPanel(batchControl);
	}

	@Override
	public JMenu createMenu(Frame parentFrame) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JComponent getPanel() {
		// TODO Auto-generated method stub
		return batchControlPanel.getMainPanel();
	}

	@Override
	public JToolBar getToolBar() {
		return null;
	}

}
