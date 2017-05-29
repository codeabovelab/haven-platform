package com.codeabovelab.dm.cluman.ds.nodes;

import com.codeabovelab.dm.agent.notifier.NotifierData;
import com.codeabovelab.dm.cluman.model.DiscoveryStorage;
import com.codeabovelab.dm.cluman.model.NodeInfo;
import com.codeabovelab.dm.cluman.model.NodeInfoImpl;
import com.codeabovelab.dm.cluman.model.NodesGroup;
import com.codeabovelab.dm.common.utils.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.MimeTypeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableMap.of;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * Test for swarm token discovery service implementation
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DiscoveryNodeControllerTest.TestConfiguration.class)
public class DiscoveryNodeControllerTest {
    private static final String CLUSTER_ID = "cluster_id";
    private static final String URL = "/discovery/nodes";
    private static final String SECRET = "secr3t";
    @Autowired
    private DiscoveryStorage discoveryStorage;
    @Autowired
    private NodeStorage nodeStorage;
    private MockMvc mvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void before() {
        mvc = standaloneSetup(new DiscoveryNodeController(nodeStorage, SECRET,
                "docker run --name havenAgent -d -e \"dm_agent_notifier_server={server}\" {secret} " +
                "--restart=unless-stopped -p 8771:8771 -v /run/docker.sock:/run/docker.sock codeabovelab/agent:latest"))
                .build();
    }

    @Test
    public void testAgent() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/discovery/agent/")
                .contentType(MimeTypeUtils.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
        .andExpect(content().string("docker run --name havenAgent -d" +
                " -e \"dm_agent_notifier_server=http://localhost:80\"" +
                " -e \"dm_agent_notifier_secret=secr3t\" " +
                "--restart=unless-stopped -p 8771:8771 -v /run/docker.sock:/run/docker.sock codeabovelab/agent:latest"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test() throws Exception {
        final String clusterId = CLUSTER_ID;
        NodesGroup cluster = discoveryStorage.getCluster(clusterId);

        {
            Collection<NodeInfo> nodes = cluster.getNodes();
            assertThat(nodes, empty());
        }

        final String hostPort = "node-one:1234";
        final String secondHostPort = "node-two:134";
        addNode(hostPort, true);
        addNode(secondHostPort, true);
        addNode("unauthorized:876", false);

        {
            Collection<NodeInfo> nodes = cluster.getNodes();
            assertThat(nodes, hasSize(2));
            System.out.println(nodes);
            assertThat(nodes, hasItems(hasProperty("address", is(hostPort)), hasProperty("address", is(secondHostPort))));
        }
    }

    @SuppressWarnings("deprecation")
    private void addNode(String hostPort, boolean auth) throws Exception {
        NotifierData data = new NotifierData();
        data.setName(StringUtils.before(hostPort, ':'));
        data.setAddress(hostPort);
        MockHttpServletRequestBuilder b = MockMvcRequestBuilders.post(getClusterUrl(data.getName()))
          .param("ttl", "234")
          .contentType(MimeTypeUtils.APPLICATION_JSON_VALUE)
          .content(objectMapper.writeValueAsString(data));
        if(auth) {
            b.header("X-Auth-Node", SECRET);
        }
        mvc.perform(b).andExpect(auth ? status().isOk() : status().isUnauthorized());
    }

    private String getClusterUrl(String clusterId) {
        return URL + "/" + clusterId;
    }

    @Configuration
    public static class TestConfiguration {
        final Map<String, NodeInfo> nodes = new HashMap<>();

        @SuppressWarnings("unchecked")
        @Bean
        NodeStorage nodeStorage() {
            NodeStorage ns = mock(NodeStorage.class);
            Mockito.doAnswer(invocation -> {
                final String name = invocation.getArgumentAt(0, String.class);
                final Consumer updater = invocation.getArgumentAt(2, Consumer.class);
                NodeInfo ni = nodes.get(name);
                NodeInfoImpl.Builder b = NodeInfoImpl.builder(ni);
                updater.accept(b);
                nodes.put(name, b.build());
                return null;
            }).when(ns).updateNode(anyString(), anyInt(), anyObject());
            return ns;
        }

        @Bean
        DiscoveryStorage discoveryStorage() {
            NodesGroup cluster = mock(NodesGroup.class);
            when(cluster.getNodes()).thenAnswer(invocation -> new ArrayList<>(nodes.values()));

            DiscoveryStorage storage = mock(DiscoveryStorage.class);

            when(storage.getCluster(CLUSTER_ID)).thenReturn(cluster);
            return storage;
        }
    }

}
