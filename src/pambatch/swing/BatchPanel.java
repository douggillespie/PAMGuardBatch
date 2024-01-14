package pambatch.swing;

import java.awt.LayoutManager;

import PamView.panel.PamPanel;
import pambatch.BatchControl;
import pambatch.config.BatchParameters;
import pambatch.config.SettingsObserver;

public abstract class BatchPanel extends PamPanel implements SettingsObserver {

	private static final long serialVersionUID = 1L;
	
	private BatchControl batchControl;

	public BatchPanel(BatchControl batchControl) {
		super();
		this.batchControl = batchControl;
		batchControl.getSettingsObservers().addObserver(this);
	}

	public BatchPanel(BatchControl batchControl, LayoutManager layout) {
		super(layout);
		this.batchControl = batchControl;
		batchControl.getSettingsObservers().addObserver(this);
	}
	
	/**
	 * Called when there has been a significant configuration change, such as a new psfx or a mode change. 
	 */
	@Override
	public void settingsUpdate(int changeType) {
		setParams(batchControl.getBatchParameters());
	}

	public abstract void setParams(BatchParameters batchParams);

	/**
	 * @return the batchControl
	 */
	public BatchControl getBatchControl() {
		return batchControl;
	}
}
