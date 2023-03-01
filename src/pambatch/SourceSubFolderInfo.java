package pambatch;

import java.io.File;

/**
 * Inofrmation about sub folders within a master folder. 
 */
public class SourceSubFolderInfo {

	private int nSourceFiles;
	
	public SourceSubFolderInfo(File sourceSubFolder, int nSourceFiles) {
		super();
		this.sourceSubFolder = sourceSubFolder;
		this.nSourceFiles = nSourceFiles;
	}

	/**
	 * @return the nSourceFiles
	 */
	public int getnSourceFiles() {
		return nSourceFiles;
	}

	/**
	 * @param nSourceFiles the nSourceFiles to set
	 */
	public void setnSourceFiles(int nSourceFiles) {
		this.nSourceFiles = nSourceFiles;
	}

	/**
	 * @return the sourceSubFolder
	 */
	public File getSourceSubFolder() {
		return sourceSubFolder;
	}

	/**
	 * @param sourceSubFolder the sourceSubFolder to set
	 */
	public void setSourceSubFolder(File sourceSubFolder) {
		this.sourceSubFolder = sourceSubFolder;
	}

	private File sourceSubFolder;
}
