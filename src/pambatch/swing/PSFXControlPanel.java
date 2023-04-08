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

import PamUtils.PamFileChooser;
import PamUtils.PamFileFilter;
import PamView.PamColors.PamColor;
import PamView.dialog.PamButton;
import PamView.dialog.PamCheckBox;
import PamView.dialog.PamDialog;
import PamView.dialog.PamGridBagContraints;
import PamView.dialog.PamLabel;
import PamView.dialog.PamTextField;
import PamView.panel.PamPanel;
import nidaqdev.networkdaq.Shell;
import pambatch.BatchControl;
import pambatch.config.BatchParameters;
import pamguard.Pamguard;

public class PSFXControlPanel extends BatchPanel {

	private JTextField psfxName;

	private PamCheckBox useThisPSFX;

	private JButton browseButton, openButton;

	private JTextField localExecutable; // going to launch from the installed exe, not from the jar file if possible. 

	private BatchControl batchControl;

	private BatchParameters batchParams;
	
	private JCheckBox noGUI;

	//	private JTextField jreName;
	//	
	//	private JTextField maxMemory;
	//	
	//	private JTextField vmOptions;

	public PSFXControlPanel(BatchControl batchControl) {
		super(new BorderLayout());
		this.batchControl = batchControl;
		this.setBorder(new TitledBorder("Configuration"));
		psfxName = new PamTextField(80);
		psfxName.setEditable(false);
		browseButton = new PamButton("Browse ...");
		openButton = new PamButton("Launch configuration ...");
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
		useThisPSFX.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setUseThisPsfx();
			}
		});
		setLayout(new BorderLayout());
		JPanel psfxPanel = new PamPanel();
		this.add(psfxPanel, BorderLayout.WEST);
		psfxPanel.setLayout( new GridBagLayout());
		GridBagConstraints c = new PamGridBagContraints();
		psfxPanel.add(new PamLabel(" PSFX Configuration to run  "), c);
//		c.gridx++;
//		psfxPanel.add(useThisPSFX, c);
		c.gridx++;
		psfxPanel.add(browseButton, c);
		c.gridx++;
		psfxPanel.add(openButton, c);
		c.gridx++;
		psfxPanel.add(new JLabel("Use the PAMGuard 'Start' button to start batch jobs"));
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 4;
		psfxPanel.add(psfxName, c);

		c.gridx = 0;
		c.gridy++;
		noGUI = new JCheckBox("Run processes without GUI (Headless)");
		noGUI.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setNoGUI();
			}
		});
		psfxPanel.add(noGUI, c);
		//		JPanel topPanel = new BatchPanel(new BorderLayout());
		//		JPanel tlPanel = new BatchPanel(new BorderLayout());
		//		JPanel trPanel = new BatchPanel(new GridBagLayout());
		//		topPanel.add(BorderLayout.CENTER, tlPanel);
		//		topPanel.add(BorderLayout.EAST, trPanel);
		//		tlPanel.add(BorderLayout.WEST, new PamLabel(" PSFX Configuration to run  "));
		//		tlPanel.add(BorderLayout.CENTER, psfxName);
		//		
		//		
		//		trPanel.add(browseButton, c);
		//		c.gridx++;
		//		trPanel.add(openButton, c);
		//		this.add(BorderLayout.NORTH, topPanel);
	}
	
	@Override
	public void setParams(BatchParameters batchParams) {
		this.batchParams = batchParams;
		psfxName.setText(batchParams.getMasterPSFX());
		this.useThisPSFX.setSelected(batchParams.useThisPSFX);
		
		enableControls();
	}
	
	private void enableControls() {
		boolean isUseThis = useThisPSFX.isSelected();
		browseButton.setEnabled(!isUseThis);
		psfxName.setEnabled(isUseThis);
	}

	private void setNoGUI() {
		if (batchParams != null) {
			batchParams.setNoGUI(noGUI.isSelected());
		}
	}

	protected void setUseThisPsfx() {
		enableControls();
		if (batchParams != null) {
			batchParams.useThisPSFX = useThisPSFX.isSelected();
		}
	}

	protected void browsePSFX() {
		PamFileChooser pamFileChooser = new PamFileChooser();
		pamFileChooser.setFileFilter(new PamFileFilter("PAMGuard Configuration Files", "psfx"));
		int ret = pamFileChooser.showOpenDialog(this);
		if (ret == PamFileChooser.APPROVE_OPTION) {
			File selFile = pamFileChooser.getSelectedFile();
			if (selFile != null) {
				psfxName.setText(selFile.getAbsolutePath());
				if (batchParams != null) {
					batchParams.setMasterPSFX(selFile.getAbsolutePath());
				}
			}
		}

	}

	protected void launchPSFX() {
		/**
		 * Having serious trub launching clone within eclipse, so just use installed version. 
		 */
		String psfx = psfxName.getText();
		File psfxFile = new File(psfx);
		if (psfxFile.exists() == false) {
			PamDialog.showWarning(batchControl.getGuiFrame(), "Error", "PSFX file does not exist");
			return;
		}
		batchControl.launchPamguard(psfx, false);
		
		// can we get the PAMGuard jar that this is running ? 
		//		String jar = "C:/Users/dg50/source/repos/PAMGuardDG/target/classes/";
		//		String java = "C:\\Program Files\\Java\\jdk-16.0.2\\bin\\java.exe";
		//		String cmd = String.format("\"%s\" -cp \"%s\" pamguard.Pamguard", java, jar);
		//		Process proc = null;
		//		try {
		//			 proc = Runtime.getRuntime().exec(cmd);
		////			proc.getOutputStream()
		//		} catch (IOException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}
		//		System.out.println(cmd);

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
		//		  System.exit(0);
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

}
