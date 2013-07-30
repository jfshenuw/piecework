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
package piecework.ldap;

import org.apache.cxf.configuration.jsse.SSLUtils;
import org.apache.cxf.configuration.security.FiltersType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.ldap.SizeLimitExceededException;
import org.springframework.ldap.authentication.DefaultValuesAuthenticationSourceDecorator;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.ContextMapperCallbackHandler;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DistinguishedName;
import org.springframework.ldap.core.support.AbstractContextMapper;
import org.springframework.ldap.core.support.DirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.ExternalTlsDirContextAuthenticationStrategy;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.LikeFilter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.ldap.SpringSecurityLdapTemplate;
import org.springframework.security.ldap.authentication.SpringSecurityAuthenticationSource;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import piecework.config.EmbeddedLdapConfiguration;
import piecework.config.LdapConfiguration;
import piecework.identity.InternalUserDetails;
import piecework.test.config.UnitTestConfiguration;
import org.springframework.security.ldap.server.ApacheDSContainer;
import piecework.util.KeyManagerCabinet;

import javax.naming.directory.SearchControls;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Set;

/**
 * @author James Renfro
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes={LdapConfiguration.class, EmbeddedLdapConfiguration.class, UnitTestConfiguration.class})
@ActiveProfiles({"ldap", "embedded-ldap", "test"})
public class CustomLdapUserDetailsMapperTest {

    @Autowired
    ApacheDSContainer apacheDSContainer;

    @Autowired
    Environment environment;

    @Autowired
    UserDetailsService userDetailsService;

    @Autowired
    LdapContextSource personLdapContextSource;

    @Autowired
    LdapContextSource groupLdapContextSource;

    @Before
    public void setup() {
        apacheDSContainer.start();
    }

    @Test
    public void testInternalUserDetails() throws Exception {
        String testUser = environment.getProperty("authentication.testuser");

        UserDetails userDetails = userDetailsService.loadUserByUsername(testUser);

        Assert.assertTrue(userDetails instanceof InternalUserDetails);

        InternalUserDetails internalUserDetails = InternalUserDetails.class.cast(userDetails);
        Assert.assertEquals(testUser, internalUserDetails.getUsername());
    }


}