package pambatch.swing;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamController.PamController;
import PamView.dialog.PamGridBagContraints;
import PamView.panel.PamAlignmentPanel;
import PamView.panel.PamPanel;
import pambatch.BatchControl;
import pambatch.ctrl.BatchState;
import pambatch.ctrl.BatchStateObserver;
import pambatch.remote.NetInterfaceFinder;

public class AgentMulticastPanel extends BatchControlPanel implements BatchStateObserver {

	private JPanel mainPanel;
	
	private BatchControl batchControl;
	
	private JComboBox<String> interfaceList;
	
	private JTextField ipAddress;

	private boolean initComplete;

	public AgentMulticastPanel(BatchControl batchControl) {
		super();
		this.batchControl = batchControl;
		mainPanel = new PamAlignmentPanel(BorderLayout.WEST);
//		mainPanel.setBorder(new TitledBorder("Remote computer communications"));
		mainPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		mainPanel.add(new JLabel("Network Interface for remote control "), c);
		interfaceList = new JComboBox<String>();
		c.gridx++;
		mainPanel.add(interfaceList, c);
		c.gridx++;
		mainPanel.add(new JLabel("  address "), c);
		c.gridx++;
		mainPanel.add(ipAddress = new JTextField(12));
		ipAddress.setEditable(false);
		
		
		interfaceList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectInterface();
			}
		});
		if (PamController.getInstance().isInitializationComplete()) {
			setParams();
		}
		batchControl.addStateObserver(this);
	}
	
	protected void selectInterface() {
		List<NetworkInterface> netInfs = NetInterfaceFinder.getIPV4Interfaces();
		int selind = interfaceList.getSelectedIndex();
		if (selind >= 0 && selind < netInfs.size()) {
			batchControl.getBatchParameters().setNetworkInterfaceName(netInfs.get(selind).getName());
			batchControl.updateObservers(BatchState.NEWSETTING);
			sayIpAddr(netInfs.get(selind));
		}
	}
	
	private void sayIpAddr(NetworkInterface netInf) {
		if (netInf == null) {
			ipAddress.setText("");
		}
		Inet4Address ipv4 = NetInterfaceFinder.getIPV4Address(netInf);
		ipAddress.setText(ipv4.getHostAddress());
	}

	private void setParams() {
		interfaceList.removeAllItems();
		List<NetworkInterface> netInfs = NetInterfaceFinder.getIPV4Interfaces();
		int selInd = -1;
		String selName = batchControl.getBatchParameters().getNetworkInterfaceName();
		for (int i = 0; i < netInfs.size(); i++) {
			NetworkInterface netInf = netInfs.get(i);
			interfaceList.addItem(netInf.getDisplayName());
			if (netInf.getName().equals(selName)) {
				selInd = i;
			}
		}
		if (selInd >= 0) {
			interfaceList.setSelectedIndex(selInd);
		}
	}
	
	public JPanel getMainPanel() {
		return mainPanel;
	}

	@Override
	public void update(BatchState batchState, Object data) {
		switch (batchState) {
		case INITIALISATIONCOMPLETE:
			setParams();
			initComplete = true;
		}
	}
}
