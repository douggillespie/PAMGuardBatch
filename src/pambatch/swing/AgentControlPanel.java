package pambatch.swing;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import PamView.panel.PamPanel;
import pambatch.BatchControl;

public class AgentControlPanel extends BatchControlPanel {

	private BatchControl batchControl;

	private AgentTableView agentTableView;
	
	private AgentMulticastPanel agentMulticastPanel;
	
	private JPanel mainPanel;
	
	public AgentControlPanel(BatchControl batchControl) {
		super();
		this.batchControl = batchControl;
		
		mainPanel = new PamPanel(new BorderLayout());
		agentTableView = new AgentTableView(batchControl, batchControl.getRemoteAgentHandler().getRemoteAgentDataBlock());
		JComponent tableComponent;
		mainPanel.add(BorderLayout.CENTER, tableComponent = agentTableView.getComponent());
		mainPanel.setBorder(new TitledBorder("Processing machines"));
		agentMulticastPanel = new AgentMulticastPanel(batchControl);
		mainPanel.add(BorderLayout.NORTH, agentMulticastPanel.getMainPanel());
	}
	
	public JPanel getPanel() {
		return mainPanel;
	}

	
	
}
