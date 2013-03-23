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
package piecework.form.model.view;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import piecework.form.model.Endpoint;
import piecework.form.model.builder.EndpointBuilder;

/**
 * @author James Renfro
 */
@XmlRootElement(name = EndpointView.Constants.ROOT_ELEMENT_NAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = EndpointView.Constants.TYPE_NAME)
public class EndpointView implements Endpoint {

	@XmlAttribute(name = EndpointView.Attributes.ID)
	@XmlID
	private final String id;
	
	@XmlAttribute(name = EndpointView.Attributes.URL)
	private final String url;
	
	@XmlAttribute(name = EndpointView.Attributes.METHOD)
	private final String method;
	
	@XmlAttribute(name = EndpointView.Attributes.MEDIA_TYPE)
	private final String mediaType;

	private EndpointView() {
		this(new EndpointView.Builder());
	}
	
	private EndpointView(EndpointView.Builder builder) {
		this.id = builder.getId();
		this.url = builder.getUrl();
		this.method = builder.getMethod();
		this.mediaType = builder.getMediaType();
	}
	
	public String getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

	public String getMethod() {
		return method;
	}

	public String getMediaType() {
		return mediaType;
	}

	static class Attributes {
		final static String ID = "id";
		final static String MEDIA_TYPE = "mediaType";
		final static String METHOD = "method";
		final static String URL = "url";
	}
	
	static class Constants {
		public static final String ROOT_ELEMENT_NAME = "endpointReference";
		public static final String TYPE_NAME = "EndpointReferenceType";
	}
	
	public final static class Builder extends EndpointBuilder<EndpointView> {

		public Builder() {
			super();
		}
		
		public Builder(Endpoint endpoint) {
			super(endpoint);
		}
		
		@Override
		public EndpointView build() {
			return new EndpointView(this);
		}
		
	}
}
