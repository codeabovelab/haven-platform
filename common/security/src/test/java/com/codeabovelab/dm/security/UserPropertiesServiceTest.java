package com.codeabovelab.dm.security;

import com.codeabovelab.dm.security.acl.AclConfiguration;
import com.codeabovelab.dm.security.entity.UserAuthDetails;
import com.codeabovelab.dm.security.repository.UserRepository;
import com.codeabovelab.dm.security.sampleobject.SampleObjectsConfiguration;
import com.codeabovelab.dm.security.user.UserPropertiesService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional(isolation = Isolation.REPEATABLE_READ)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringApplicationConfiguration(classes = {UserPropertiesServiceTest.TestConfiguration.class})
public class UserPropertiesServiceTest {


    @Configuration
    @EnableAutoConfiguration
    @EnableTransactionManagement
    @Import({AclConfiguration.class, SampleObjectsConfiguration.class})
    @ComponentScan(basePackageClasses = {UserAuthDetails.class, UserRepository.class})
    public static class TestConfiguration {
    }

    private final static String username = "tester";

    @Autowired
    UserPropertiesService service;
    @Autowired
    UserRepository userRepository;

    @Before
    public void prepare() {
        UserAuthDetails user = new UserAuthDetails();
        user.setUsername(username);
        user.setEmail("te@st.ts");
        user.setPassword("62626262");
        user.setTitle("test user");
        UserAuthDetails saved = userRepository.save(user);

    }

    @After
    public void after() {
        userRepository.delete(userRepository.findByUsername(username));

    }

    @Test
    public void testProperties() {

        Map<String, String> map = new HashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        map.put("three", "3");
        service.update(username, new HashMap<>(map));
        {
            Map<String, String> res = service.get(username);
            assertEquals(map, res);
        }

        // update three
        map.put("three", "99");
        {
            HashMap<String, String> copy = new HashMap<>(map);
            // remove two
            copy.put("two", null);
            map.remove("two");
            // we do not touch one
            copy.remove("one");
            service.update(username, copy);
        }
        {
            Map<String, String> res = service.get(username);
            assertEquals(map, res);

            //clear all
            for(String key: res.keySet()) {
                res.put(key, null);
            }
            service.update(username, res);
            Map<String, String> mustBeEmpty = service.get(username);
            assertEquals(0, mustBeEmpty.size());
        }

    }

    @Test
    public void testUserRelation() {

        {
            // put some props back for test user remove
            HashMap<String, String> map = new HashMap<>();
            map.put("two", "two");
            service.update(username, map);
        }

        final UserAuthDetails one = userRepository.findByUsername(username);
        assertNotNull(one);
        assertTrue(service.get(username).size() > 0);
        // for below line we need commit or use single transaction, that is impossible in this test
        //assertNotNull(one.getProperties());
        userRepository.delete(one.getId());
        userRepository.flush();
    }

}
//*/