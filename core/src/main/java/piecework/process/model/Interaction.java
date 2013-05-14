/*
 * Copyright 2013 University of Washington
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package piecework.process.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import piecework.Sanitizer;
import piecework.common.view.ViewContext;

/**
 * @author James Renfro
 */
@XmlRootElement(name = Interaction.Constants.ROOT_ELEMENT_NAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = Interaction.Constants.TYPE_NAME)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "interaction")
public class Interaction implements Serializable {

	private static final long serialVersionUID = 851573721362656883L;

	@XmlAttribute
	@XmlID
	@Id
	private final String id;
	
	@XmlAttribute
	private final String uri;
	
	@XmlElement
	private final String label;
	
	@XmlElementWrapper(name="screens")
	@XmlElementRef
	@DBRef
	private final List<Screen> screens;
	
	@XmlTransient
	@JsonIgnore
	private final boolean isDeleted;
	
	private Interaction() {
		this(new Interaction.Builder(), new ViewContext());
	}

	@SuppressWarnings("unchecked")
	private Interaction(Interaction.Builder builder, ViewContext context) {
		this.id = builder.id;
		this.label = builder.label;
		this.uri = context != null ? context.getApplicationUri(builder.processDefinitionKey, builder.id) : null;
		this.screens = (List<Screen>) (builder.screens != null ? Collections.unmodifiableList(builder.screens) : Collections.emptyList());
		this.isDeleted = builder.isDeleted;
	}
	
	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public List<Screen> getScreens() {
		return screens;
	}

	public String getUri() {
		return uri;
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public final static class Builder {

		private String processDefinitionKey;
		private String id;
		private String label;
		private List<Screen> screens;
		private boolean isDeleted;
		
		public Builder() {
			super();
		}

		public Builder(Interaction interaction, Sanitizer sanitizer) {
			this.id = sanitizer.sanitize(interaction.id);
			this.label = sanitizer.sanitize(interaction.label);
			
			if (interaction.screens != null && !interaction.screens.isEmpty()) {
				this.screens = new ArrayList<Screen>(interaction.screens.size());
				for (Screen screen : interaction.screens) {
					this.screens.add(new Screen.Builder(screen, sanitizer).build());
				}
			}
		}

		public Interaction build() {
			return new Interaction(this, null);
		}

		public Interaction build(ViewContext context) {
			return new Interaction(this, context);
		}
		
		public Builder processDefinitionKey(String processDefinitionKey) {
			this.processDefinitionKey = processDefinitionKey;
			return this;
		}
		
		public Builder id(String id) {
			this.id = id;
			return this;
		}
		
		public Builder label(String label) {
			this.label = label;
			return this;
		}
		
		public Builder screen(Screen screen) {
			if (this.screens == null)
				this.screens = new ArrayList<Screen>();
			this.screens.add(screen);
			return this;
		}
		
		public Builder screens(List<Screen> screens) {
			this.screens = screens;
			return this;
		}
		
		public Builder delete() {
			this.isDeleted = true;
			return this;
		}
		
		public Builder undelete() {
			this.isDeleted = false;
			return this;
		}

		public List<Screen> getScreens() {
			return screens;
		}

		public String getId() {
			return id;
		}
	}
	
	public static class Constants {
		public static final String RESOURCE_LABEL = "Interaction";
		public static final String ROOT_ELEMENT_NAME = "interaction";
		public static final String TYPE_NAME = "InteractionType";
	}
}
