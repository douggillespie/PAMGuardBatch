package pambatch.swing;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamUtils.PamFileChooser;
import PamUtils.PamFileFilter;
import PamView.PamColors.PamColor;
import PamView.dialog.PamButton;
import PamView.dialog.PamGridBagContraints;
import PamView.dialog.PamLabel;
import PamView.dialog.PamTextField;
import PamView.panel.PamPanel;
import pambatch.BatchControl;

public class PSFXControlPanel extends BatchPanel {

	private JTextField psfxName;

	private JButton browseButton, openButton;

	private JTextField localExecutable; // going to launch from the installed exe, not from the jar file if possible. 

	//	private JTextField jreName;
	//	
	//	private JTextField maxMemory;
	//	
	//	private JTextField vmOptions;

	public PSFXControlPanel(BatchControl batchControl) {
		super(new BorderLayout());
		this.setBorder(new TitledBorder("Configuration"));
		psfxName = new PamTextField(80);
		browseButton = new PamButton("Browse ...");
		openButton = new PamButton("Launch configuration ...");
		browseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				browsePSFX();
			}
		});
		openButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				launchPSFX();
			}
		});
		
		JPanel topPanel = new BatchPanel(new BorderLayout());
		JPanel tlPanel = new BatchPanel(new BorderLayout());
		JPanel trPanel = new BatchPanel(new GridBagLayout());
		topPanel.add(BorderLayout.CENTER, tlPanel);
		topPanel.add(BorderLayout.EAST, trPanel);
		tlPanel.add(BorderLayout.WEST, new PamLabel(" PSFX Configuration to run  "));
		tlPanel.add(BorderLayout.CENTER, psfxName);
		GridBagConstraints c = new PamGridBagContraints();
		trPanel.add(browseButton, c);
		c.gridx++;
		trPanel.add(openButton, c);
		this.add(BorderLayout.NORTH, topPanel);
	}

	protected void browsePSFX() {
		PamFileChooser pamFileChooser = new PamFileChooser();
		pamFileChooser.setFileFilter(new PamFileFilter("PAMGuard Configuration Files", "psfx"));
		int ret = pamFileChooser.showOpenDialog(this);
		if (ret == PamFileChooser.APPROVE_OPTION) {
			File selFile = pamFileChooser.getSelectedFile();
			if (selFile != null) {
				psfxName.setText(selFile.getAbsolutePath());
			}
		}

	}

	protected void launchPSFX() {
		// TODO Auto-generated method stub

	}

}
