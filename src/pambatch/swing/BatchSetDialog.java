package pambatch.swing;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import PamUtils.PamFileChooser;
import PamView.dialog.PamDialog;


public abstract class BatchSetDialog extends PamDialog{

	private static PamFileChooser sharedChooser;
	
	protected JobSet[] jobSets = new JobSet[3];

	protected static final int SOURCES = 0;
	protected static final int BINARY = 1;
	protected static final int DATABASE = 2;

	protected static String[] sectionNames = {"Source folder", "Binary folder", "Database folder"};
	protected static String[] tipName = {"Source folder or URI for soundn files to process",
			"Binary folder for output data", "Output database (file end will be added at run time)"};
	
	public BatchSetDialog(Window parentFrame, String title, boolean hasDefault) {
		super(parentFrame, title, hasDefault);
	}


	protected void addJobSet(int i, JPanel mainPanel, GridBagConstraints c) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(BorderLayout.WEST, new JLabel(sectionNames[i]));
		JButton selButton;
		topPanel.add(BorderLayout.EAST, selButton = new JButton("Select"));
		selButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectButton(i);
			}
		});
		panel.add(topPanel);
		JTextField mainField = new JTextField(60);
		mainField.setToolTipText(tipName[i]);
		panel.add(mainField);
		mainPanel.add(panel, c);
		jobSets[i] = new JobSet(mainField, selButton);
	}
	
	protected abstract void selectButton(int i);

	protected class JobSet {

		protected JTextField mainField;

		protected JButton selectbutton;

		protected JobSet(JTextField mainField, JButton selButton) {
			this.mainField = mainField;
			this.selectbutton = selButton;
		}

		protected String getText() {
			return mainField.getText();
		}

		protected void setText(String text) {
			mainField.setText(text);
		}

	}
	
	/**
	 * File choosers seem to take ages to create, so use a shared one. 
	 * @return
	 */
	PamFileChooser getSharedChooser() {
		if (sharedChooser == null) {
			sharedChooser = new PamFileChooser();
		}
		return sharedChooser;
	}

}
