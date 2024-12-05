package pambatch;

import PamModel.PamDependency;
import PamModel.PamPluginInterface;

public class BatchJobPlugin implements PamPluginInterface {

	private String jarFile;

	@Override
	public String getDefaultName() {
		return "Batch Processing";
	}

	@Override
	public String getHelpSetName() {
		return "pambatch/help/BatchHelp.hs";
	}

	@Override
	public void setJarFile(String jarFile) {
		this.jarFile = jarFile;
	}

	@Override
	public String getJarFile() {
		return jarFile;
	}

	@Override
	public String getDeveloperName() {
		return "Doug Gillespie";
	}

	@Override
	public String getContactEmail() {
		return "support@pamguard.org";
	}

	@Override
	public String getVersion() {
		return "1.6";
	}

	@Override
	public String getPamVerDevelopedOn() {
		return "2.02.15";
	}

	@Override
	public String getPamVerTestedOn() {
		return "2.02.15";
	}

	@Override
	public String getAboutText() {
		return "Batch processing";
	}

	@Override
	public String getClassName() {
		return BatchControl.class.getName();
	}

	@Override
	public String getDescription() {
		return "Batch processing";
	}

	@Override
	public String getMenuGroup() {
		return "Utilities";
	}

	@Override
	public String getToolTip() {
		return "Control of multiple PAMGuard processes with a common configuration (batch prcessing)";
	}

	@Override
	public PamDependency getDependency() {
		return null;
	}

	@Override
	public int getMinNumber() {
		return 0;
	}

	@Override
	public int getMaxNumber() {
		return 1;
	}

	@Override
	public int getNInstances() {
		return 0;
	}

	@Override
	public boolean isItHidden() {
		return false;
	}

	@Override
	public int allowedModes() {
		return PamPluginInterface.ALLMODES;
	}

}
