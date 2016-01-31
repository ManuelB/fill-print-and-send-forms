package de.incentergy.letter.sender.jsf;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

import com.ocpsoft.pretty.faces.annotation.URLMapping;

@Named
@RequestScoped
@URLMapping(id = "formFileName", pattern = "/#{index.formFileName}.pdf", viewId = "/index.jsf")
public class Index {

	private String formFileName;

	public String getFormFileName() {
		return formFileName;
	}

	public void setFormFileName(String formName) {
		this.formFileName = formName;
	}

}
