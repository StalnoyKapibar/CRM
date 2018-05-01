package com.ewp.crm.models;

import javax.persistence.*;

@Entity
@Table
public class MessageTemplate {

	@Id
	@GeneratedValue
	private Long id;

	@Column(unique = true)
	private String name;

	@Lob
	private String templateText;

	public MessageTemplate(String name, String templateText) {
		this.name = name;
		this.templateText = templateText;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTemplateText() {
		return templateText;
	}

	public void setTemplateText(String templateText) {
		this.templateText = templateText;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		MessageTemplate that = (MessageTemplate) o;

		if (id != null ? !id.equals(that.id) : that.id != null) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		return templateText != null ? templateText.equals(that.templateText) : that.templateText == null;
	}

	@Override
	public int hashCode() {
		int result = id != null ? id.hashCode() : 0;
		result = 31 * result + (name != null ? name.hashCode() : 0);
		result = 31 * result + (templateText != null ? templateText.hashCode() : 0);
		return result;
	}
}
