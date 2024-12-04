package pambatch.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
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
		mainPanel.setBorder(new TitledBorder("Processor"));
		agentMulticastPanel = new AgentMulticastPanel(batchControl);
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				mainPanel.setPreferredSize(new Dimension(0,getPreferredHeight()));
			}
		});
//		agentTableView.getComponent().setPreferredSize(new Dimension(0, 60));
		/**
		 *  Ditching multiple machines for now, so 
		 * there will only ever be the one machine, which is localhost. 
		 * This should leave the table in place, but it will only ever have one entry. 
		 */
		//		mainPanel.add(BorderLayout.NORTH, agentMulticastPanel.getMainPanel());
	}

	public JPanel getPanel() {
		return mainPanel;
	}
	
	public int getPreferredHeight() {
		int th = agentTableView.getPreferredHeight(1);
		Insets insets = mainPanel.getInsets();
		if (insets != null) {
			th += insets.bottom+insets.top;
		}
		th +=2;
//		Border border = mainPanel.getBorder();
//		if (border != null) {
//			Insets bi = border.getBorderInsets(mainPanel);
//			if (bi != null) {
//				th += bi.top + bi.bottom;
//			}
//		}
		
		return th;
	}



}
