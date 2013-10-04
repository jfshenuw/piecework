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
package piecework.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import piecework.common.ViewContext;
import piecework.enumeration.InteractionStatus;
import piecework.security.Sanitizer;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.*;

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
	@Transient
	private final String processDefinitionKey;
	
	@XmlAttribute
	private final String link;
	
	@XmlElement
	private final String label;

    @XmlElement
    private final String completionStatus;
	
	@XmlElementWrapper(name="screens")
	@XmlElementRef
	@DBRef
	private final List<Screen> screens;

    @XmlElementWrapper(name="taskDefinitionKeys")
    private final Set<String> taskDefinitionKeys;

    @XmlElementWrapper(name="candidates")
    @XmlElementRef
    private final List<Candidate> candidates;

    @XmlAttribute
    private final int ordinal;

	@XmlTransient
	@JsonIgnore
	private final boolean isDeleted;
	
	private Interaction() {
		this(new Interaction.Builder(), new ViewContext());
	}

	@SuppressWarnings("unchecked")
	private Interaction(Interaction.Builder builder, ViewContext context) {
		this.id = builder.id;
		this.processDefinitionKey = builder.processDefinitionKey;
		this.label = builder.label;
        this.completionStatus = builder.completionStatus;
		this.link = context != null ? context.getApplicationUri(builder.processDefinitionKey, builder.id) : null;
		this.screens = Collections.unmodifiableList(builder.screens);
        this.taskDefinitionKeys = (Set<String>) (builder.taskDefinitionKeys != null ? Collections.unmodifiableSet(builder.taskDefinitionKeys) : Collections.emptySet());
		this.candidates = (List<Candidate>) (builder.candidates != null ? Collections.unmodifiableList(builder.candidates) : Collections.emptyList());
        this.ordinal = builder.ordinal;
        this.isDeleted = builder.isDeleted;
	}
	
	public String getId() {
		return id;
	}

	public String getProcessDefinitionKey() {
		return processDefinitionKey;
	}

	public String getLabel() {
		return label;
	}

	public List<Screen> getScreens() {
		return screens;
	}

    public Set<String> getTaskDefinitionKeys() {
        return taskDefinitionKeys;
    }

    public String getCompletionStatus() {
        return completionStatus;
    }

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public String getLink() {
		return link;
	}

    public int getOrdinal() {
        return ordinal;
    }

    @JsonIgnore
	public boolean isDeleted() {
		return isDeleted;
	}

	public final static class Builder {

		private String processDefinitionKey;
		private String id;
		private String label;
		private List<Screen> screens;
        private Set<String> taskDefinitionKeys;
        private String completionStatus;
        private List<Candidate> candidates;
        private int ordinal;
		private boolean isDeleted;
		
		public Builder() {
			super();
            this.screens = new ArrayList<Screen>();
		}

		public Builder(Interaction interaction, Sanitizer sanitizer) {
			this.id = sanitizer.sanitize(interaction.id);
			this.label = sanitizer.sanitize(interaction.label);
			this.processDefinitionKey = sanitizer.sanitize(interaction.processDefinitionKey);
			this.completionStatus = sanitizer.sanitize(interaction.completionStatus);
			if (interaction.screens != null && !interaction.screens.isEmpty()) {
				this.screens = new ArrayList<Screen>(interaction.screens.size());
				for (Screen screen : interaction.screens) {
					this.screens.add(new Screen.Builder(screen, sanitizer).processDefinitionKey(processDefinitionKey).build());
				}
			}  else {
                this.screens = new ArrayList<Screen>();
            }
            if (interaction.taskDefinitionKeys != null && !interaction.taskDefinitionKeys.isEmpty()) {
                this.taskDefinitionKeys = new HashSet<String>(interaction.taskDefinitionKeys.size());
                for (String taskDefinitionKey : interaction.taskDefinitionKeys) {
                    this.taskDefinitionKeys.add(sanitizer.sanitize(taskDefinitionKey));
                }
            } else {
                this.taskDefinitionKeys = new HashSet<String>();
            }
            if (interaction.candidates != null && !interaction.candidates.isEmpty()) {
                this.candidates = new ArrayList<Candidate>(interaction.candidates.size());
                for (Candidate candidate : interaction.candidates) {
                    this.candidates.add(new Candidate.Builder(candidate, sanitizer).build());
                }
            } else {
                this.candidates = new ArrayList<Candidate>();
            }
            this.ordinal = interaction.ordinal;
            this.isDeleted = interaction.isDeleted;
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

        public Builder candidate(Candidate candidate) {
            if (this.candidates == null)
                this.candidates = new ArrayList<Candidate>();
            this.candidates.add(candidate);
            return this;
        }

        public Builder completionStatus(String completionStatus) {
            this.completionStatus = completionStatus;
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

        public Builder taskDefinitionKey(String taskDefinitionKey) {
            if (this.taskDefinitionKeys == null)
                this.taskDefinitionKeys = new HashSet<String>();
            this.taskDefinitionKeys.add(taskDefinitionKey);
            return this;
        }

        public Builder ordinal(int ordinal) {
            this.ordinal = ordinal;
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

        public Builder clearScreens() {
            this.screens = new ArrayList<Screen>();
            return this;
        }
	}
	
	public static class Constants {
		public static final String RESOURCE_LABEL = "Interaction";
		public static final String ROOT_ELEMENT_NAME = "interaction";
		public static final String TYPE_NAME = "InteractionType";
	}
}
