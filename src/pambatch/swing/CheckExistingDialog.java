package pambatch.swing;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;

import PamView.dialog.PamDialog;
import PamView.dialog.PamGridBagContraints;
import pambatch.BatchControl;

/**
 * dialog to check for existing jobs. This is called / shown at start up 
 * and blocks the GUI for a short while, while the system sends out multicast messages
 * to find existing PAMGuard jobs. This could occurr if the main controller exited but 
 * while some jobs were still running. Can this really happen ? Not really on one machine
 * but might become important when running on multiple machines. 
 * @author dg50
 *
 */
public class CheckExistingDialog extends PamDialog {
	
	private JProgressBar progressBar;
	
	private static final int secondScale = 10;
	
	private Timer timer;

	private long timerStartTime;

	private int waitSeconds;

	private CheckExistingDialog(Window parentFrame, int timeSeconds) {
		super(parentFrame, "Check for existing jobs", false);
		this.waitSeconds = timeSeconds;
		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setBorder(new TitledBorder("Startup Checks"));
		GridBagConstraints c = new PamGridBagContraints();
		mainPanel.add(new JLabel("Sending out messages to check for existing jobs"), c);
		c.gridy++;
		progressBar = new JProgressBar(0, timeSeconds*secondScale);
		mainPanel.add(progressBar, c);
		
		getOkButton().setVisible(false);
		getCancelButton().setVisible(false);
		setDialogComponent(mainPanel);
		
		timer = new Timer(100, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				timerAction();
			}
		});
	}
	
	protected void timerAction() {
		long elapsed = System.currentTimeMillis() - timerStartTime;
		progressBar.setValue((int) (elapsed*secondScale/1000));
		if (elapsed/1000 >= waitSeconds) {
			closeLater();
		}
	}

	public static void showDialog(Window parentFrame, BatchControl batchContro, int waitSeconds) {
		CheckExistingDialog dialog = new CheckExistingDialog(parentFrame, waitSeconds);
		dialog.setVisible(true);
	}

	@Override
	public boolean getParams() {
		return true;
	}

	@Override
	public void cancelButtonPressed() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void restoreDefaultSettings() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			timer.start();
			timerStartTime = System.currentTimeMillis();
		}
		else {
			timer.stop();
		}
		super.setVisible(visible);
	}

}
