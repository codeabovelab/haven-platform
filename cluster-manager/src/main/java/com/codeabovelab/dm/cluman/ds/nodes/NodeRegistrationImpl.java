/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.cluman.ds.nodes;

import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.cluman.persistent.PersistentBusFactory;
import com.codeabovelab.dm.cluman.security.AccessContextFactory;
import com.codeabovelab.dm.cluman.security.SecuredType;
import com.codeabovelab.dm.common.mb.*;
import com.codeabovelab.dm.common.security.Action;
import org.springframework.security.acls.model.ObjectIdentity;

import java.util.*;
import java.util.function.Consumer;

/**
 */
class NodeRegistrationImpl implements NodeRegistration {

    /**
     * minimal ttl of node in seconds
     */
    private static final int MIN_TTL = 10;
    private final String name;
    private final Object lock = new Object();
    private volatile NodeInfoImpl cache;
    private final NodeInfoImpl.Builder builder;

    private volatile long endTime;
    private final MessageBus<NodeHealthEvent> healthBus;
    private volatile int ttl;
    private final NodeUpdateHandler nuh;
    private final ObjectIdentity oid;

    NodeRegistrationImpl(PersistentBusFactory pbf, NodeInfo nodeInfo, NodeUpdateHandler nuh) {
        String name = nodeInfo.getName();
        NodeUtils.checkName(name);
        this.name = name;
        this.nuh = nuh;
        this.oid = SecuredType.NODE.id(name);
        // name may contain dots
        this.healthBus = pbf.create(NodeHealthEvent.class, "node[" + name + "].metrics", 2000/* TODO in config */);
        synchronized (lock) {
            this.builder = NodeInfoImpl.builder(nodeInfo);
        }
    }



    /**
     * Invoke updating state (save into KV-storage) of node with specified ttl.
     * @param ttl in seconds
     */
    public void update(int ttl) {
        if(ttl < MIN_TTL) {
            ttl = MIN_TTL;
        }
        //also convert seconds to ms
        this.endTime = System.currentTimeMillis() + (ttl * 1000L);
        this.ttl = ttl;
    }

    public int getTtl() {
        return this.ttl;
    }

    @Override
    public Subscriptions<NodeHealthEvent> getHealthSubscriptions() {
        return this.healthBus.asSubscriptions();
    }

    private boolean isOn() {
        long now = System.currentTimeMillis();
        return now <= endTime;
    }

    @Override
    public NodeInfoImpl getNodeInfo() {
        NodeInfoImpl ni;
        final boolean onlineChanged;
        synchronized (lock) {
            builder.name(name);
            boolean on = isOn();
            onlineChanged = on != builder.isOn();
            ni = cache;
            if(ni == null || onlineChanged) {
                ni = cache = builder.on(on).build();
            }
        }
        if(onlineChanged) {
            fireNodeChanged(ni.isOn() ? StandardActions.ONLINE : StandardActions.OFFLINE, ni);
        }
        return ni;
    }

    private void fireNodeChanged(String action, NodeInfoImpl ni) {
        this.nuh.fireNodeModification(this, action, ni);
    }

    NodeMetrics updateHealth(NodeMetrics metrics) {
        checkAccessUpdate();
        NodeMetrics nmnew;
        String cluster;
        synchronized (lock) {
            nmnew = NodeMetrics.builder().from(this.builder.getHealth()).fromNonNull(metrics).build();
            this.builder.setHealth(nmnew);
            cluster = this.builder.getCluster();
            cache = null;
        }
        this.healthBus.accept(new NodeHealthEvent(this.name, cluster, nmnew));
        return nmnew;
    }

    private void checkAccessUpdate() {
        AccessContextFactory.getLocalContext().assertGranted(oid, Action.UPDATE);
    }

    /**
     * Update internal node info.
     */
    void updateNodeInfo(Consumer<NodeInfoImpl.Builder> modifier) {
        checkAccessUpdate();
        //
        // do not send node events from this method!
        //
        NodeMetrics nmnew = null;
        String cluster;
        NodeInfoImpl oldni;
        NodeInfoImpl ni;
        synchronized (lock) {
            oldni = getNodeInfo();
            NodeMetrics oldMetrics = this.builder.getHealth();
            boolean on = this.builder.isOn();
            modifier.accept(this.builder);
            NodeMetrics newMetrics = this.builder.getHealth();
            if(!Objects.equals(oldMetrics, newMetrics)) {
                nmnew = newMetrics;
            }
            this.builder.setOn(on);//we must save 'on' flag
            cluster = this.builder.getCluster();
            cache = null;
            ni = getNodeInfo();
        }
        if(!Objects.equals(oldni, ni)) {//we try to reduce count of unnecessary 'update' events
            fireNodeChanged(StandardActions.UPDATE, ni);
        }
        if(nmnew != null) {
            this.healthBus.accept(new NodeHealthEvent(this.name, cluster, nmnew));
        }
    }

    public void setCluster(String cluster) {
        NodeInfoImpl ni = null;
        synchronized (lock) {
            String oldCluster = this.builder.getCluster();
            if(!Objects.equals(oldCluster, cluster)) {
                this.builder.setCluster(cluster);
                cache = null;
                ni = getNodeInfo();
            }
        }
        if(ni != null) {
            fireNodeChanged(StandardActions.UPDATE, ni);
        }
    }

    public String getCluster() {
        synchronized (lock) {
            return this.builder.getCluster();
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public ObjectIdentity getOid() {
        return oid;
    }
}
