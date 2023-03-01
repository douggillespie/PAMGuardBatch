package pambatch.swing;

import java.awt.LayoutManager;

import PamView.panel.PamPanel;
import pambatch.config.BatchParameters;

public abstract class BatchPanel extends PamPanel {

	public BatchPanel() {
		super();
	}

	public BatchPanel(LayoutManager layout) {
		super(layout);
	}

	public abstract void setParams(BatchParameters batchParams);
}
