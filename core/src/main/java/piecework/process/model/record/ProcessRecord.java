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
package piecework.process.model.record;

import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import piecework.Sanitizer;
import piecework.common.view.ViewContext;


/**
 * @author James Renfro
 */
@Document(collection = "process")
public class ProcessRecord implements piecework.process.model.Process {

	private static final long serialVersionUID = 1L;

	@Id
	private String id;
	
	private String processDefinitionKey;
	
	private String processLabel;
	
	private String processSummary;
	
	private String participantSummary;
	
	private String engine;
	
	private String engineProcessDefinitionKey;
	
	private boolean isDeleted;
	
	private String startRequestFormIdentifier;
	
	private String startResponseFormIdentifier;
	
	private Map<String, String> taskRequestFormIdentifiers;
	
	private Map<String, String> taskResponseFormIdentifiers;
	
	private ProcessRecord() {
		
	}
	
	private ProcessRecord(ProcessRecord.Builder builder) {
		this.id = builder.getProcessDefinitionKey();
		this.processLabel = builder.getProcessLabel();
		this.processSummary = builder.getProcessSummary();
		this.participantSummary = builder.getParticipantSummary();
		this.processDefinitionKey = builder.getProcessDefinitionKey();
		this.engine = builder.getEngine();
		this.engineProcessDefinitionKey = builder.getEngineProcessDefinitionKey();
		this.isDeleted = builder.isDeleted();
		this.startRequestFormIdentifier = builder.getStartRequestFormIdentifier();
		this.startResponseFormIdentifier = builder.getStartResponseFormIdentifier();
		this.taskRequestFormIdentifiers = builder.getTaskRequestFormIdentifiers();
		this.taskResponseFormIdentifiers = builder.getTaskResponseFormIdentifiers();
	}
	
	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	@Override
	public String getProcessDefinitionKey() {
		return processDefinitionKey;
	}

	@Override
	public String getEngine() {
		return engine;
	}

	@Override
	public String getEngineProcessDefinitionKey() {
		return engineProcessDefinitionKey;
	}

	public boolean isDeleted() {
		return isDeleted;
	}

	public void setDeleted(boolean isDeleted) {
		this.isDeleted = isDeleted;
	}

	public void setProcessDefinitionKey(String processDefinitionKey) {
		this.processDefinitionKey = processDefinitionKey;
	}

	public void setEngine(String engine) {
		this.engine = engine;
	}

	public void setEngineProcessDefinitionKey(String engineProcessDefinitionKey) {
		this.engineProcessDefinitionKey = engineProcessDefinitionKey;
	}

	public String getStartRequestFormIdentifier() {
		return startRequestFormIdentifier;
	}

	public void setStartRequestFormIdentifier(String startRequestFormIdentifier) {
		this.startRequestFormIdentifier = startRequestFormIdentifier;
	}

	public String getStartResponseFormIdentifier() {
		return startResponseFormIdentifier;
	}

	public void setStartResponseFormIdentifier(String startResponseFormIdentifier) {
		this.startResponseFormIdentifier = startResponseFormIdentifier;
	}

	public Map<String, String> getTaskRequestFormIdentifiers() {
		return taskRequestFormIdentifiers;
	}

	public void setTaskRequestFormIdentifiers(
			Map<String, String> taskRequestFormIdentifiers) {
		this.taskRequestFormIdentifiers = taskRequestFormIdentifiers;
	}

	public Map<String, String> getTaskResponseFormIdentifiers() {
		return taskResponseFormIdentifiers;
	}

	public void setTaskResponseFormIdentifiers(
			Map<String, String> taskResponseFormIdentifiers) {
		this.taskResponseFormIdentifiers = taskResponseFormIdentifiers;
	}
	
	public final static class Builder extends piecework.process.model.builder.ProcessBuilder<ProcessRecord> {
		
		private boolean isDeleted;
		
		public Builder() {
			super();
		}
		
		public Builder(piecework.process.model.Process process, Sanitizer sanitizer) {
			super(process, sanitizer);
		}
		
		public Builder delete() {
			this.isDeleted = true;
			return this;
		}
		
		public Builder undelete() {
			this.isDeleted = false;
			return this;
		}
		
		public ProcessRecord build() {
			return new ProcessRecord(this);
		}

		@Override
		public ProcessRecord build(ViewContext context) {
			return new ProcessRecord(this);
		}

		public boolean isDeleted() {
			return isDeleted;
		}
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

}
