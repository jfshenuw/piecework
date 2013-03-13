/*
 * Copyright 2010 University of Washington
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
package piecework.authorization;

public enum PermissionType { 
	CREATE("Create"), EDIT("Edit"), SEARCH("Search"), VIEW("View"); 
	
	public static final String Create = "Create";
	public static final String Edit = "Edit";
	public static final String Search = "Search";
	public static final String View = "View";
	
	private String name;
	
	private PermissionType(String name) {
		this.name = name;
	}
	
	public static PermissionType resolvePermissionTypeFromName(String name) {
		return PermissionType.valueOf(name.toUpperCase());
	}
	
}