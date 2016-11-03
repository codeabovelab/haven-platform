package com.codeabovelab.dm.cluman.ui;

import com.codeabovelab.dm.cluman.model.NodeInfo;
import com.codeabovelab.dm.cluman.model.NodeInfoImpl;
import com.codeabovelab.dm.cluman.model.NodeMetrics;
import com.codeabovelab.dm.cluman.ui.model.UISearchQuery;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.codeabovelab.dm.cluman.ui.model.UISearchQuery.SortOrder.DESC;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

public class FilterApiTest {

    private final List<NodeInfo> nodeStorage = new ArrayList<>();
    private final FilterApi filterApi = new FilterApi();

    @SuppressWarnings("unchecked")
    @Before
    public void prepare() {

        NodeInfo nodeInfo1 = NodeInfoImpl.builder()
                .address("address")
                .name("nodeInfo1")
                .labels(Collections.singletonMap("key", "value"))
                .health(NodeMetrics.builder()
                        .healthy(Boolean.FALSE)
                        .sysMemAvail(null)
                        .sysMemUsed(2000L)
                        .sysMemTotal(2000L)
                        .sysCpuLoad(1f)
                        .build()).build();

        NodeInfo nodeInfo2 = NodeInfoImpl.builder()
                .address("address")
                .name("nodeInfo2")
                .labels(Collections.singletonMap("key", "value"))
                .health(NodeMetrics.builder()
                        .healthy(TRUE)
                        .sysMemAvail(2000L)
                        .sysMemUsed(2000L)
                        .sysMemTotal(4000L)
                        .sysCpuLoad(0.5f)
                        .build()).build();

        NodeInfo nodeInfo3 = NodeInfoImpl.builder()
                .address("address")
                .name("nodeInfo3")
                .labels(Collections.singletonMap("key", "value"))
                .health(NodeMetrics.builder()
                        .healthy(TRUE)
                        .sysMemAvail(500L)
                        .sysMemUsed(500L)
                        .sysMemTotal(1000L)
                        .sysCpuLoad(0.3f)
                        .build()).build();
        nodeStorage.add(nodeInfo1);
        nodeStorage.add(nodeInfo2);
        nodeStorage.add(nodeInfo3);

    }

    @Test
    public void testSearchSysCpuLoad() throws Exception {

        UISearchQuery uiSearchQuery = new UISearchQuery("health.sysCpuLoad < 0.99", null, 2, 0);

        Collection<NodeInfo> nodeInfos = filterApi.listNodes(nodeStorage, uiSearchQuery);

        assertNotNull(nodeInfos);
        assertEquals(nodeInfos.size(), 2);
        nodeInfos.forEach(n -> assertTrue(n.getHealth().getSysCpuLoad() < 0.99));
    }

    @Test
    public void testSearchSysMemUsed() throws Exception {

        UISearchQuery uiSearchQuery2 = new UISearchQuery("health.sysMemUsed > 1000", null, 1, 0);
        Collection<NodeInfo> nodeInfos2 = filterApi.listNodes(nodeStorage, uiSearchQuery2);
        assertNotNull(nodeInfos2);
        assertEquals(nodeInfos2.size(), 1);
        nodeInfos2.forEach(n -> assertTrue(n.getHealth().getSysMemUsed() > 1000));
    }

    @Test
    public void testSearchHealthy() throws Exception {

        UISearchQuery uiSearchQuery3 = new UISearchQuery("health.sysCpuLoad < 0.99 && health.healthy == true", null, 10, 0);
        Collection<NodeInfo> nodeInfos3 = filterApi.listNodes(nodeStorage, uiSearchQuery3);
        assertNotNull(nodeInfos3);
        assertEquals(nodeInfos3.size(), 2);
        nodeInfos3.forEach(n -> assertTrue(n.getHealth().getHealthy() == TRUE));
    }
    @Test
    public void testSorting() throws Exception {

        UISearchQuery.SearchOrder order = new UISearchQuery.SearchOrder("health.sysMemTotal", DESC);

        UISearchQuery uiSearchQuery4 = new UISearchQuery(null, singletonList(order), 10, 0);

        Collection<NodeInfo> nodeInfos4 = filterApi.listNodes(nodeStorage, uiSearchQuery4);
        assertNotNull(nodeInfos4);
        assertEquals(nodeInfos4.size(), 3);

        Iterator<NodeInfo> iterator = nodeInfos4.iterator();
        assertEquals(iterator.next().getHealth().getSysMemTotal().longValue(), 4000L);
        assertEquals(iterator.next().getHealth().getSysMemTotal().longValue(), 2000L);
        assertEquals(iterator.next().getHealth().getSysMemTotal().longValue(), 1000L);

    }

    @Test
    public void testSearchSysMemAvailNulls() throws Exception {

        UISearchQuery.SearchOrder order = new UISearchQuery.SearchOrder("health.sysMemAvail", DESC);
        UISearchQuery uiSearchQuery4 = new UISearchQuery(null, singletonList(order), 10, 0);

        Collection<NodeInfo> nodeInfos4 = filterApi.listNodes(nodeStorage, uiSearchQuery4);
        assertNotNull(nodeInfos4);
        assertEquals(nodeInfos4.size(), 3);
    }

}