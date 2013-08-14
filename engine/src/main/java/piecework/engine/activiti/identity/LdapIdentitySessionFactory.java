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
package piecework.engine.activiti.identity;

import org.activiti.engine.impl.interceptor.Session;
import org.activiti.engine.impl.interceptor.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author James Renfro
 */
@Service("ldapIdentitySessionFactory")
public class LdapIdentitySessionFactory implements SessionFactory {

    @Autowired
    LdapIdentityManager ldapIdentityManager;

    @Override
    public Class<?> getSessionType() {
        return LdapIdentityManager.class;
    }

    @Override
    public Session openSession() {
        return ldapIdentityManager;
    }

}