package pambatch.swing;

import java.awt.Window;
import java.io.File;

import PamUtils.SelectFolder;
import PamView.dialog.PamDialog;

@Deprecated
public class JobFolderDialog extends PamDialog {

	private boolean mustExist;
	
	private File selectedFolder;
	
	private SelectFolder selectFolder;

	private JobFolderDialog(Window parentFrame, String title, boolean mustExist) {
		super(parentFrame, title, false);
		this.mustExist = mustExist;
		selectFolder = new SelectFolder("Select folder", 50, false);
		setDialogComponent(selectFolder.getFolderPanel());
	}
	
	public File showDialog(Window parentFrame, String title, File currentFolder, boolean mustExist) {
		JobFolderDialog folderDialog = new JobFolderDialog(parentFrame, title, mustExist);
		folderDialog.setFolder(currentFolder);
		folderDialog.setVisible(true);
		return folderDialog.selectedFolder;
	}

	private void setFolder(File currentFolder) {
		selectedFolder = currentFolder;
//		if (currentFolder.)
//		selectFolder.setFolderName(getName());
	}

	@Override
	public boolean getParams() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void cancelButtonPressed() {
		selectedFolder = null;
	}

	@Override
	public void restoreDefaultSettings() {

	}

}
