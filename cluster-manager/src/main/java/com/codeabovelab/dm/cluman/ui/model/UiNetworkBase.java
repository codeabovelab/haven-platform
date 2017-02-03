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

import com.codeabovelab.dm.cluman.cluster.docker.model.CreateNetworkCmd;
import com.codeabovelab.dm.cluman.cluster.docker.model.Network;
import com.codeabovelab.dm.common.utils.Sugar;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
@Data
public class UiNetworkBase {
    private String name;
    private String cluster;
    private String driver;
    private final Map<String, String> labels = new HashMap<>();
    private final Map<String, String> options = new HashMap<>();
    private boolean enableIpv6;
    private boolean internal;
    private Ipam ipam;

    public void to(CreateNetworkCmd cmd) {
        cmd.setName(getName());
        cmd.setDriver(getDriver());
        Sugar.setIfNotNull(cmd.getOptions()::putAll, getOptions());
        Ipam ipam = getIpam();
        if(ipam != null) {
            cmd.setIpam(ipam.to());
        }
        cmd.setInternal(isInternal());
        cmd.setEnableIpv6(isEnableIpv6());
        Sugar.setIfNotNull(cmd.getLabels()::putAll, getLabels());
    }

    public UiNetworkBase from(Network net) {
        setName(net.getName());
        setDriver(net.getDriver());
        Sugar.setIfNotNull(getOptions()::putAll, net.getOptions());
        Network.Ipam ipam = net.getIpam();
        if(ipam != null) {
            setIpam(new Ipam().from(ipam));
        }
        setInternal(net.isInternal());
        setEnableIpv6(net.isEnableIpv6());
        Sugar.setIfNotNull(getLabels()::putAll, net.getLabels());
        return this;
    }

    @Data
    public static class Ipam {

        private String driver;

        private final List<UiNetwork.IpamConfig> config = new ArrayList<>();

        private final Map<String, String> options = new HashMap<>();

        public Network.Ipam to() {
            Network.Ipam.Builder ib = Network.Ipam.builder();
            ib.driver(getDriver());
            Sugar.setIfNotNull(ib::options, getOptions());
            getConfig().forEach(csrc -> ib.config(csrc.to()));
            return ib.build();
        }

        public Ipam from(Network.Ipam ipam) {
            String driver = ipam.getDriver();
            if(driver == null) {
                // docker does not allow null value for this field.
                driver = "default";
            }
            setDriver(driver);
            List<Network.IpamConfig> configs = ipam.getConfigs();
            if(configs != null) {
                configs.forEach(oc -> getConfig().add(IpamConfig.from(oc)));
            }
            return this;
        }
    }


    @Data
    public static class IpamConfig {

        private String subnet;

        private String range;

        private String gateway;

        public static IpamConfig from(Network.IpamConfig oc) {
            IpamConfig ic = new IpamConfig();
            ic.setGateway(oc.getGateway());
            ic.setRange(oc.getIpRange());
            ic.setSubnet(oc.getSubnet());
            return ic;
        }

        public Network.IpamConfig to() {
            return Network.IpamConfig.builder()
              .gateway(getGateway())
              .ipRange(getRange())
              .subnet(getSubnet())
              .build();
        }
    }
}
