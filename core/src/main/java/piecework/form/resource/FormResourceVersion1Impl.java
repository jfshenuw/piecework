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
package piecework.form.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import piecework.common.view.ViewContext;
import piecework.exception.StatusCodeError;
import piecework.form.FormRepository;
import piecework.form.FormResourceVersion1;
import piecework.form.FormService;
import piecework.form.model.Form;
import piecework.form.model.record.FormRecord;
import piecework.form.model.view.FormView;

/**
 * @author James Renfro
 */
@Service
public class FormResourceVersion1Impl implements FormResourceVersion1 {

	@Autowired 
	private FormService service;
	
	@Autowired 
	private FormRepository repository;
	
	@Value("${statement}") 
	private String statement;
	
	public FormView read(String processDefinitionKey) throws StatusCodeError {
		Form form = service.getForm(processDefinitionKey, null);
		if (form == null) {
			FormRecord.Builder formRecord = new FormRecord.Builder();
			formRecord.id(processDefinitionKey);
			formRecord.name("This is a quick test");
			service.storeForm(processDefinitionKey, null, formRecord.build(), null);
		}
		
//		SecurityContext context = SecurityContextHolder.getContext();
//		Collection<? extends GrantedAuthority> authorities = context.getAuthentication().getAuthorities();
//		
//		if (authorities != null && !authorities.isEmpty()) {
//			GrantedAuthority authority = authorities.iterator().next();
//			
//			if (authority instanceof ResourceAuthority) {
//				ResourceAuthority resourceAuthority = ResourceAuthority.class.cast(authority);
//				form.setName(resourceAuthority.toString());
//			}
//		}
		
		return new FormView.Builder(form).build(context());
	}
	
	private static ViewContext context() {
		return new ViewContext("", "", "v1", "form");
	}
}
