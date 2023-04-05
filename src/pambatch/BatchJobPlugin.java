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
		// TODO Auto-generated method stub
		return null;
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
		return "2.9";
	}

	@Override
	public String getContactEmail() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return "1.0";
	}

	@Override
	public String getPamVerDevelopedOn() {
		// TODO Auto-generated method stub
		return "2.9";
	}

	@Override
	public String getPamVerTestedOn() {
		// TODO Auto-generated method stub
		return "2.9";
	}

	@Override
	public String getAboutText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getClassName() {
		return BatchControl.class.getName();
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMenuGroup() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getToolTip() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PamDependency getDependency() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getMinNumber() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxNumber() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getNInstances() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isItHidden() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int allowedModes() {
		// TODO Auto-generated method stub
		return 0;
	}

}
