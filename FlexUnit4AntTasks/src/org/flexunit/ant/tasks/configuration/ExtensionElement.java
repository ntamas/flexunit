package org.flexunit.ant.tasks.configuration;

/**
 * Class for nested <code>&lt;extension&gt;</code> elements in a
 * <code>&lt;flexUnit&gt;</code> tag.
 * 
 * @author ntamas
 */
public class ExtensionElement {
	/**
	 * The name of the extension.
	 */
	private String name = "";
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
}
