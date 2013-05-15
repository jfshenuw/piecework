/*
 * Copyright 2012 University of Washington
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
@XmlRootElement(name = Process.Constants.ROOT_ELEMENT_NAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = Process.Constants.TYPE_NAME)
@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collection = "process")
public class Process implements Serializable {

	private static final long serialVersionUID = -4257025274360046038L;

	@XmlAttribute
	@XmlID
	@Id
	private final String processDefinitionKey;
	
	@XmlElement
	private final String processLabel;
	
	@XmlElement
	private final String processSummary;
	
	@XmlElement
	private final String participantSummary;
	
	@XmlElement
	private final String engine;
	
	@XmlElement
	private final String engineProcessDefinitionKey;
	
	@XmlElement
	private final String uri;
	
	@XmlElementWrapper(name="screens")
	@XmlElementRef
	@DBRef
	private final List<Interaction> interactions;
	
	@XmlTransient
	@JsonIgnore
	private final boolean isDeleted;
	
	private Process() {
		this(new Process.Builder(), new ViewContext());
	}
			
	@SuppressWarnings("unchecked")
	private Process(Process.Builder builder, ViewContext context) {
		this.processDefinitionKey = builder.processDefinitionKey;
		this.processLabel = builder.processLabel;
		this.processSummary = builder.processSummary;
		this.participantSummary = builder.participantSummary;
		this.engine = builder.engine;
		this.engineProcessDefinitionKey = builder.engineProcessDefinitionKey;
		this.uri = context != null ? context.getApplicationUri(builder.processDefinitionKey) : null;
		this.interactions = (List<Interaction>) (builder.interactions != null ? Collections.unmodifiableList(builder.interactions) : Collections.emptyList());
		this.isDeleted = builder.isDeleted;
	}
	
	public String getProcessDefinitionKey() {
		return processDefinitionKey;
	}

	public String getProcessLabel() {
		return processLabel;
	}

	public String getProcessSummary() {
		return processSummary;
	}

	public String getParticipantSummary() {
		return participantSummary;
	}

	public String getEngine() {
		return engine;
	}

	public String getEngineProcessDefinitionKey() {
		return engineProcessDefinitionKey;
	}

	public String getUri() {
		return uri;
	}

	public List<Interaction> getInteractions() {
		return interactions;
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public final static class Builder {
		
		private String processDefinitionKey;		
		private String processLabel;		
		private String processSummary;		
		private String participantSummary;		
		private String engine;		
		private String engineProcessDefinitionKey;
		private List<Interaction> interactions;
		private boolean isDeleted;
		
		public Builder() {
			super();
		}
				
		public Builder(piecework.process.model.Process process, Sanitizer sanitizer) {
			this.processDefinitionKey = sanitizer.sanitize(process.processDefinitionKey);
			this.processLabel = sanitizer.sanitize(process.processLabel);
			this.processSummary = sanitizer.sanitize(process.processSummary);
			this.participantSummary = sanitizer.sanitize(process.participantSummary);
			this.engine = sanitizer.sanitize(process.engine);
			this.engineProcessDefinitionKey = sanitizer.sanitize(process.engineProcessDefinitionKey);
		
			if (process.interactions != null && !process.interactions.isEmpty()) {
				this.interactions = new ArrayList<Interaction>(process.interactions.size());
				for (Interaction interaction : process.interactions) {
					this.interactions.add(new Interaction.Builder(interaction, sanitizer).processDefinitionKey(processDefinitionKey).build());
				}
			}
		}
		
		public Process build() {
			return new Process(this, null);
		}
		
		public Process build(ViewContext context) {
			return new Process(this, context);
		}
		
		public Builder processDefinitionKey(String processDefinitionKey) {
			this.processDefinitionKey = processDefinitionKey;
			return this;
		}
		
		public Builder processLabel(String processLabel) {
			this.processLabel = processLabel;
			return this;
		}
		
		public Builder processSummary(String processSummary) {
			this.processSummary = processSummary;
			return this;
		}
		
		public Builder participantSummary(String participantSummary) {
			this.participantSummary = participantSummary;
			return this;
		}
		
		public Builder engine(String engine) {
			this.engine = engine;
			return this;
		}
		
		public Builder engineProcessDefinitionKey(String engineProcessDefinitionKey) {
			this.engineProcessDefinitionKey = engineProcessDefinitionKey;
			return this;
		}
		
		public Builder interaction(Interaction interaction) {
			if (this.interactions == null)
				this.interactions = new ArrayList<Interaction>();
			this.interactions.add(interaction);
			return this;
		}
		
		public Builder interactions(List<Interaction> interactions) {
			this.interactions = interactions;
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
	}
	
	public static class Constants {
		public static final String RESOURCE_LABEL = "Process";
		public static final String ROOT_ELEMENT_NAME = "process";
		public static final String TYPE_NAME = "ProcessType";
	}
}
