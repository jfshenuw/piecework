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
package piecework.form.view;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import piecework.form.model.Constraint;

/**
 * @author James Renfro
 */
@XmlRootElement(name = ConstraintView.Constants.ROOT_ELEMENT_NAME)
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = ConstraintView.Constants.TYPE_NAME)
public class ConstraintView implements Constraint {

	@XmlElement(name = ConstraintView.Elements.CONSTRAINT_TYPE_CODE)
	private final String constraintTypeCode;
	
	@XmlElement(name = ConstraintView.Lists.REFERENCED_PROPERTY_NAMES)
	private final List<String> referencedPropertyNames;
	
	@XmlElement(name = ConstraintView.Elements.OPERATOR)
	private final String operator;
	
	@XmlElement(name = ConstraintView.Elements.VALUE)
	private final String value;
	
	@XmlElement(name = ConstraintView.Elements.EFFECT)
	private final String effect;
	
	private ConstraintView() {
		this(new ConstraintView.Builder());
	}
	
	private ConstraintView(ConstraintView.Builder builder) {
		this.constraintTypeCode = builder.constraintTypeCode;
		this.referencedPropertyNames = builder.referencedPropertyNames;
		this.operator = builder.operator;
		this.value = builder.value;
		this.effect = builder.effect;
	}
	
	@Override
	public String getConstraintTypeCode() {
		return constraintTypeCode;
	}

	@Override
	public List<String> getReferencedPropertyNames() {
		return referencedPropertyNames;
	}

	@Override
	public String getOperator() {
		return operator;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	public String getEffect() {
		return effect;
	}

	static class Constants {
		public static final String ROOT_ELEMENT_NAME = "fieldConstraint";
		public static final String TYPE_NAME = "FieldConstraintType";
	}
	
	static class Elements {
		final static String CONSTRAINT_TYPE_CODE = "constraintTypeCode";
		final static String EFFECT = "effect";
		final static String OPERATOR = "operator";
		final static String VALUE = "value";
	}
	
	static class Lists {
		final static String REFERENCED_PROPERTY_NAMES = "referencedPropertyNames";
	}
	
	/*
	 * Fluent builder class, as per Joshua Bloch's Effective Java
	 */
	public final static class Builder {
		private String constraintTypeCode;
		private List<String> referencedPropertyNames;
		private String operator;
		private String value;
		private String effect;
		
		public Builder() {
			
		}
		
		public Builder(Constraint constraint) {
			this.constraintTypeCode = constraint.getConstraintTypeCode();
			this.referencedPropertyNames = constraint.getReferencedPropertyNames();
			this.operator = constraint.getOperator();
			this.value = constraint.getValue();
			this.effect = constraint.getEffect();
		}
		
		public ConstraintView build() {
			return new ConstraintView(this);
		}
		
		public ConstraintView.Builder constraintTypeCode(String constraintTypeCode) {
			this.constraintTypeCode = constraintTypeCode;
			return this;
		}
		
		public ConstraintView.Builder referencedPropertyName(String referencedPropertyName) {
			if (this.referencedPropertyNames == null)
				this.referencedPropertyNames = new ArrayList<String>();
			this.referencedPropertyNames.add(referencedPropertyName);
			return this;
		}
		
		public ConstraintView.Builder referencedPropertyNames(List<String> referencedPropertyNames) {
			if (this.referencedPropertyNames == null)
				this.referencedPropertyNames = new ArrayList<String>();
			this.referencedPropertyNames.addAll(referencedPropertyNames);
			return this;
		}
		
		public ConstraintView.Builder operator(String operator) {
			this.operator = operator;
			return this;
		}
		
		public ConstraintView.Builder value(String value) {
			this.value = value;
			return this;
		}
		
		public ConstraintView.Builder effect(String effect) {
			this.effect = effect;
			return this;
		}
	}
}
