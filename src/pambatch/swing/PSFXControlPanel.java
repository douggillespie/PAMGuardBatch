package pambatch.swing;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import PamController.PamController;
import PamUtils.PamFileChooser;
import PamUtils.PamFileFilter;
import PamView.PamColors.PamColor;
import PamView.PamObjectViewer;
import PamView.dialog.PamButton;
import PamView.dialog.PamCheckBox;
import PamView.dialog.PamDialog;
import PamView.dialog.PamGridBagContraints;
import PamView.dialog.PamLabel;
import PamView.dialog.PamTextField;
import PamView.help.PamHelp;
import PamView.panel.PamPanel;
import nidaqdev.networkdaq.Shell;
import pambatch.BatchControl;
import pambatch.config.BatchMode;
import pambatch.config.BatchParameters;
import pambatch.config.SettingsObserver;
import pambatch.config.SettingsObservers;
import pamguard.Pamguard;

public class PSFXControlPanel extends BatchPanel {

	private JTextField psfxName;

	private PamCheckBox useThisPSFX;

	private JButton browseButton, openButton, viewModel;

	private JTextField localExecutable; // going to launch from the installed exe, not from the jar file if possible.

	private BatchControl batchControl;

	private JCheckBox noGUI;

	private JButton helpButton;

	public PSFXControlPanel(BatchControl batchControl) {
		super(batchControl, new BorderLayout());
		this.batchControl = batchControl;
		this.setBorder(new TitledBorder("Configuration"));
		psfxName = new PamTextField(80);
		psfxName.setEditable(false);
		browseButton = new PamButton("Browse ...");
		openButton = new PamButton("Launch configuration ...");
		viewModel = new PamButton("View model");
		useThisPSFX = new PamCheckBox("Use this configuration");
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
		viewModel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				viewConfiguration();
			}
		});
		useThisPSFX.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setUseThisPsfx();
			}
		});
		setLayout(new BorderLayout());
		JPanel psfxPanel = new PamPanel();
		this.add(psfxPanel, BorderLayout.WEST);
		psfxPanel.setLayout(new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		psfxPanel.add(new PamLabel(" PSFX Configuration to run  "), c);
//		c.gridx++;
//		psfxPanel.add(useThisPSFX, c);
		c.gridx++;
		psfxPanel.add(browseButton, c);
		c.gridx++;
		psfxPanel.add(openButton, c);
		c.gridx++;
		psfxPanel.add(viewModel, c);
		c.gridx++;
		psfxPanel.add(new JLabel("Use the PAMGuard 'Start' button to start batch jobs"));
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 5;
		psfxPanel.add(psfxName, c);

		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 4;
		noGUI = new JCheckBox("Run processes without GUI (Headless)");
		noGUI.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setNoGUI();
			}
		});
		psfxPanel.add(noGUI, c);

		helpButton = new JButton("Help");
		helpButton.setToolTipText("Help pages for batch processing");
		helpButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				helpPressed();
			}
		});
//		c.gridy++;
		c.gridx += 3;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		psfxPanel.add(helpButton, c);

		setToolTips();
	}
	protected void helpPressed() {
		String hp = "docs.batchoverview";
		PamHelp.getInstance().displayContextSensitiveHelp(hp);
	}

	@Override
	public void setParams(BatchParameters batchParams) {
		psfxName.setText(batchParams.getMasterPSFX());
		this.useThisPSFX.setSelected(batchParams.useThisPSFX);
		noGUI.setSelected(batchParams.isNoGUI());
		enableControls();
		setToolTips();
	}

	private void enableControls() {
//		boolean isUseThis = useThisPSFX.isSelected();
//		browseButton.setEnabled(!isUseThis);
//		psfxName.setEnabled(isUseThis);
		psfxName.setEditable(false);
		browseButton.setEnabled(true);
		openButton.setEnabled(hasPSFX());
		viewModel.setEnabled(hasPSFX());
	}

	private void setToolTips() {

		BatchMode batchMode = batchControl.getBatchParameters().getBatchMode();
		if (batchMode == BatchMode.NORMAL) {
			browseButton.setToolTipText("Select PSFX file for data processing");
		} else {
			browseButton.setToolTipText("Select extracted PSFX file, or extract settings from task database");
		}
		openButton.setToolTipText("Show and edit configuration in PAMGuard");
		viewModel.setToolTipText("Show data model view");
	}

	private void setNoGUI() {
		BatchParameters batchParams = batchControl.getBatchParameters();
		if (batchParams != null) {
			batchParams.setNoGUI(noGUI.isSelected());
		}
	}

	protected void setUseThisPsfx() {
		enableControls();
		BatchParameters batchParams = batchControl.getBatchParameters();
		if (batchParams != null) {
			batchParams.useThisPSFX = useThisPSFX.isSelected();
		}
	}

	private boolean hasPSFX() {
		BatchParameters batchParams = batchControl.getBatchParameters();
		if (batchParams == null) {
			return false;
		}
		if (batchParams.getMasterPSFX() == null) {
			return false;
		}
		return true;
	}

	protected void browsePSFX() {
		BatchParameters batchParams = batchControl.getBatchParameters();
		PamFileChooser pamFileChooser = new PamFileChooser();
		pamFileChooser.setFileFilter(new PamFileFilter("PAMGuard Configuration Files", "psfx"));
		int ret = pamFileChooser.showOpenDialog(this);
		if (ret == PamFileChooser.APPROVE_OPTION) {
			File selFile = pamFileChooser.getSelectedFile();
			if (selFile != null) {
				psfxName.setText(selFile.getAbsolutePath());
				if (batchParams != null) {
					batchParams.setMasterPSFX(selFile.getAbsolutePath());
					getBatchControl().settingsChange(SettingsObservers.CHANGE_CONFIG);
				}
			}
		}

	}

	protected void launchPSFX() {
		/**
		 * Having serious trub launching clone within eclipse, so just use installed
		 * version.
		 */
		String psfx = psfxName.getText();
		File psfxFile = new File(psfx);
		if (psfxFile.exists() == false) {
			PamDialog.showWarning(batchControl.getGuiFrame(), "Error", "PSFX file does not exist");
			return;
		}
		batchControl.launchPamguard(psfx, false);

		// can we get the PAMGuard jar that this is running ?
		// String jar = "C:/Users/dg50/source/repos/PAMGuardDG/target/classes/";
		// String java = "C:\\Program Files\\Java\\jdk-16.0.2\\bin\\java.exe";
		// String cmd = String.format("\"%s\" -cp \"%s\" pamguard.Pamguard", java, jar);
		// Process proc = null;
		// try {
		// proc = Runtime.getRuntime().exec(cmd);
		//// proc.getOutputStream()
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// System.out.println(cmd);

//		final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
//		File currentJar = null;
//		try {
//			currentJar = new File(Pamguard.class.getProtectionDomain().getCodeSource().getLocation().toURI());
//		} catch (URISyntaxException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//
//		/* is it a jar file? */
////		if(!currentJar.getName().endsWith(".jar"))
////			return;
//
//		/* Build command: java -jar application.jar */
//		final ArrayList<String> command = new ArrayList<String>();
//		command.add(javaBin);
//		command.add("-cp");
//		command.add(currentJar.getPath());
//		command.add("pamguard.Pamguard");
//
//		final ProcessBuilder builder = new ProcessBuilder(command);
//		try {
//			builder.start();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		// System.exit(0);
//		// https://learn.microsoft.com/en-us/troubleshoot/windows-client/shell-experience/command-line-string-limitation
//	      StringBuilder cmd = new StringBuilder();
//	      cmd.append("\"");
//	        cmd.append(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
//		      cmd.append("\" ");
//		      List<String> inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
////	        for (String jvmArg : inputArgs) {
////	            cmd.append(jvmArg + " ");
////	        }
//	        String xBean = ManagementFactory.getRuntimeMXBean().getClassPath();
//	        cmd.append("-cp ").append(xBean).append(" class ");
//	        cmd.append(Pamguard.class.getName()).append(" ");
////	        for (String arg : args) {
////	            cmd.append(arg).append(" ");
////	        }
//	        System.out.println(cmd);
//	        System.out.printf("Command line is %d characters long\n", cmd.length());
////	        try {
////				Runtime.getRuntime().exec(cmd.toString());
////			} catch (IOException e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
////			}
//			final ProcessBuilder builder = new ProcessBuilder(cmd.toString());
//			try {
//				builder.start();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
	}

	/**
	 * View the configuratoin in the model viewer.
	 */
	protected void viewConfiguration() {
		PamObjectViewer.Show(PamController.getMainFrame(),
				batchControl.getExternalConfiguration().getExtConfiguration());
	}

}
