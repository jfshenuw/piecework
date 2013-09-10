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
package piecework.engine.activiti.config;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

import org.springframework.ldap.core.support.LdapContextSource;
import piecework.config.PropertiesConfiguration;
import piecework.engine.activiti.ActivitiEngineProxy;
import piecework.engine.ProcessEngineProxy;
import piecework.engine.activiti.ActivitiEngineProxyHelper;
import piecework.engine.activiti.CustomBpmnProcessParseHandler;
import piecework.engine.activiti.CustomBpmnUserTaskParseHandler;
import piecework.engine.activiti.identity.LdapIdentityManager;
import piecework.engine.activiti.identity.LdapIdentitySessionFactory;
import piecework.engine.test.ExampleFactory;
import piecework.identity.IdentityHelper;
import piecework.identity.IdentityService;
import piecework.ldap.CustomLdapUserDetailsMapper;
import piecework.ldap.LdapSettings;
import piecework.model.*;
import piecework.model.Process;
import piecework.persistence.ProcessInstanceRepository;
import piecework.persistence.ProcessRepository;

/**
 * @author James Renfro
 */
@Configuration
@Profile("test")
@PropertySource("classpath:META-INF/test.properties")
@Import({PropertiesConfiguration.class, EngineConfiguration.class})
public class TestConfiguration {

	@Bean
	public ProcessEngineProxy activitiEngineProxy() {
		return new ActivitiEngineProxy();
	}

    @Bean
    public ActivitiEngineProxyHelper activitiEngineProxyHelper() {
        return new ActivitiEngineProxyHelper();
    }

    @Bean
    public CustomBpmnProcessParseHandler customBpmnProcessParseHandler() {
        return new CustomBpmnProcessParseHandler();
    }

    @Bean
    public CustomBpmnUserTaskParseHandler customBpmnUserTaskParseHandler() {
        return new CustomBpmnUserTaskParseHandler();
    }

    @Bean
    public LdapIdentitySessionFactory ldapIdentitySessionFactory() {
        return Mockito.mock(LdapIdentitySessionFactory.class);
    }

    @Bean
    public LdapIdentityManager ldapIdentityManager() {
        return Mockito.mock(LdapIdentityManager.class);
    }

    @Bean
    public LdapContextSource personLdapContextSource() {
        return Mockito.mock(LdapContextSource.class);
    }

    @Bean
    public LdapSettings ldapSettings() {
        return Mockito.mock(LdapSettings.class);
    }

    @Bean
    public CustomLdapUserDetailsMapper userDetailsMapper() {
        return Mockito.mock(CustomLdapUserDetailsMapper.class);
    }

    @Bean
    public IdentityHelper identityHelper() {
        return Mockito.mock(IdentityHelper.class);
    }

    @Bean
    public ProcessRepository processRepository() {
        ProcessRepository repository = Mockito.mock(ProcessRepository.class);
        Process exampleProcess = ExampleFactory.exampleProcess();
        Mockito.when(repository.findOne(exampleProcess.getProcessDefinitionKey())).thenReturn(exampleProcess);
        return repository;
    }

    @Bean
    public ProcessInstanceRepository processInstanceRepository() {
        return Mockito.mock(ProcessInstanceRepository.class);
    }

    @Bean
    public IdentityService userDetailsService() {
        return Mockito.mock(IdentityService.class);
    }
	
}
