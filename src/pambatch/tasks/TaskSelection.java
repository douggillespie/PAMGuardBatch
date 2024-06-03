package pambatch.tasks;

import java.io.Serializable;

/**
 * Serializable parameters about specific tasks to store with BatchParameters
 * May never contain anything more than a bool ? 
 * @author dg50
 *
 */
public class TaskSelection implements Serializable {

	private static final long serialVersionUID = 1L;

	public boolean selected;

	public TaskSelection(boolean selected) {
		super();
		this.selected = selected;
	}
	
}
