package com.codeabovelab.dm.cluman.cluster.docker.management;

import com.codeabovelab.dm.cluman.ds.container.ContainerStarterHelper;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ContainerStarterHelperTest {

    @Test
    public void testCalculatePreferredNode() throws Exception {
        List<String> existsNodes = Arrays.asList("node1","node2","node3","node4");
        Map<String, Integer> appCountPerNode = new HashMap<String, Integer>(){{
            put("node1", 5); put("node2", 2); put("node3", 4);
        }};
        List<String> result = new ArrayList<>();
        ContainerStarterHelper.calculateConstraints(existsNodes, null, appCountPerNode, 10, result);
        assertNotNull(result);
        assertTrue(result.contains("constraint:node==~node4"));
        assertFalse(result.contains("!="));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNodesNotFounds() throws Exception {
        List<String> existsNodes = Arrays.asList("node1","node2","node3");
        Map<String, Integer> appCountPerNode = new HashMap<String, Integer>(){{
            put("node1", 5); put("node2", 2); put("node3", 4);
        }};
        List<String> result = new ArrayList<>();
        ContainerStarterHelper.calculateConstraints(existsNodes, null, appCountPerNode, 1, result);
    }

    @Test
    public void testCalculateConstraints() throws Exception {
        List<String> existsNodes = Arrays.asList("node1","node2","node3","node4");
        Map<String, Integer> appCountPerNode = new HashMap<String, Integer>(){{
            put("node1", 5); put("node2", 2); put("node3", 4);
        }};
        List<String> result = new ArrayList<>();
        ContainerStarterHelper.calculateConstraints(existsNodes, null, appCountPerNode, 3, result);
        assertNotNull(result);
        assertTrue(result.contains("constraint:node!=/\\\\Qnode3\\\\E|\\\\Qnode1\\\\E/"));
        assertTrue(result.contains("constraint:node==~node4"));

    }
}