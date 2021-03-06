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
package piecework.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * This provider maps StatusCodeError exceptions to 
 * responses that can be sent back to the client. 
 * 
 * @author James Renfro
 * @since 1.0.2.1
 * @date 8/18/2010
 */
public class StatusCodeErrorMapper implements ExceptionMapper<StatusCodeError> {

	private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(StatusCodeErrorMapper.class);
	
	/**
	 * @see javax.ws.rs.ext.ExceptionMapper#toResponse(java.lang.Throwable)
	 */
	public Response toResponse(StatusCodeError error) {
		if (LOG.isDebugEnabled())
			LOG.debug("Parsing a status code error", error);
		
		return ErrorResponseBuilder.buildErrorResponse(error);
	}
	
}
