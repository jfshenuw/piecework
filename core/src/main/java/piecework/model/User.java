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
package piecework.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;

import org.springframework.security.core.userdetails.UserDetails;
import piecework.identity.InternalUserDetails;
import piecework.security.Sanitizer;
import piecework.common.ViewContext;
import piecework.util.ManyMap;

/**
 * @author James Renfro
 */
@XmlRootElement(name = User.Constants.ROOT_ELEMENT_NAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = User.Constants.TYPE_NAME)
@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements Serializable {

    private static final long serialVersionUID = -4312076944171057691L;

	@XmlAttribute
    @XmlID
    @Id
    private final String userId;

    @XmlAttribute
    private final String visibleId;

    @XmlElement
    private final String displayName;

    @XmlElement
    private final String emailAddress;

    @XmlElement
    private final String phoneNumber;

    @XmlElement
    private final String uri;

    @XmlTransient
    private final ManyMap<String, String> attributes;

    private User() {
        this(new User.Builder(), new ViewContext());
    }

    private User(User.Builder builder, ViewContext context) {
        this.userId = builder.userId;
        this.visibleId = builder.visibleId;
        this.displayName = builder.displayName;
        this.emailAddress = builder.emailAddress;
        this.phoneNumber = builder.phoneNumber;
        this.attributes = builder.attributes;
        this.uri = context != null ? context.getApplicationUri(builder.userId) : null;
    }

    public String getUserId() {
		return userId;
	}

	public String getVisibleId() {
		return visibleId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

    @JsonIgnore
    public ManyMap<String, String> getAttributes() {
        return attributes;
    }

    public String getUri() {
		return uri;
	}

	public final static class Builder {

        private String userId;
        private String visibleId;
        private String displayName;
        private String emailAddress;
        private String phoneNumber;
        private ManyMap<String, String> attributes;

        public Builder() {
            super();
        }

        public Builder(User user, Sanitizer sanitizer) {
            this.userId = sanitizer.sanitize(user.userId);
            this.visibleId = sanitizer.sanitize(user.visibleId);
            this.displayName = sanitizer.sanitize(user.displayName);
            this.emailAddress = sanitizer.sanitize(user.emailAddress);
            this.phoneNumber = sanitizer.sanitize(user.phoneNumber);
            this.attributes = new ManyMap<String, String>();
            if (user.attributes != null) {
                for (Map.Entry<String, List<String>> entry : user.attributes.entrySet()) {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();
                    if (key == null || values == null)
                        continue;
                    for (String value : values) {
                        this.attributes.putOne(key, value);
                    }
                }
            }
        }

        public Builder(UserDetails details) {
            if (details instanceof InternalUserDetails) {
                InternalUserDetails internalUserDetails = InternalUserDetails.class.cast(details);
                this.userId = internalUserDetails.getInternalId();
                this.visibleId = internalUserDetails.getExternalId();
                this.displayName = internalUserDetails.getDisplayName();
                this.emailAddress = internalUserDetails.getEmailAddress();
            } else {
                this.userId = details.getUsername();
                this.displayName = details.getUsername();
            }
        }

        public User build() {
            return new User(this, null);
        }

        public User build(ViewContext context) {
            return new User(this, context);
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder visibleId(String visibleId) {
            this.visibleId = visibleId;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder emailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public Builder attribute(String name, String value) {
            if (this.attributes == null)
                this.attributes = new ManyMap<String, String>();
            if (name != null && value != null)
                this.attributes.putOne(name, value);
            return this;
        }
    }

    public static class Constants {
        public static final String RESOURCE_LABEL = "User";
        public static final String ROOT_ELEMENT_NAME = "user";
        public static final String TYPE_NAME = "UserType";
    }

}
