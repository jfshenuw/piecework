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

/**
 * This is an enumeration that defines the set of trusted credentials that may be used for authorization. 
 *
 * @author James Renfro
 * @since 1.0.2.1
 * @added 8/30/2010
 */
public enum AuthorizationType {
	END_USER, SYSTEM;
}
