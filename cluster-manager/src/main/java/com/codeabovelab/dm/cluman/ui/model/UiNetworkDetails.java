/*
 * Copyright 2017 Code Above Lab LLC
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

package com.codeabovelab.dm.cluman.ui.model;

import com.codeabovelab.dm.cluman.cluster.docker.model.Network;
import com.codeabovelab.dm.cluman.ds.container.ContainerStorage;
import com.codeabovelab.dm.common.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UiNetworkDetails extends UiNetwork {
    private ZonedDateTime created;
    private final List<Peer> peers = new ArrayList<>();


    public UiNetworkDetails from(Network net, ContainerStorage cs) {
        super.from(net, cs);
        this.setCreated(net.getCreated());

        List<Network.PeerInfo> peers = net.getPeers();
        if(peers != null) {
            peers.forEach(peer -> {
                getPeers().add(new Peer().from(peer));
            });
        }
        return this;
    }

    @Data
    public static class Peer {

        private String name;

        private String ip;

        public Peer from(Network.PeerInfo peer) {
            String nameAndId = peer.getName();
            // see https://github.com/docker/docker/issues/28984
            String name = StringUtils.beforeLast(nameAndId, '-');
            setName(name);
            setIp(peer.getIp());
            return this;
        }
    }
}
