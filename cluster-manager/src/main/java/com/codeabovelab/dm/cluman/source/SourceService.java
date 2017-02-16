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

package com.codeabovelab.dm.cluman.source;

import com.codeabovelab.dm.cluman.cluster.docker.management.DockerService;
import com.codeabovelab.dm.cluman.cluster.docker.management.argument.GetContainersArg;
import com.codeabovelab.dm.cluman.cluster.docker.model.*;
import com.codeabovelab.dm.cluman.ds.clusters.*;
import com.codeabovelab.dm.cluman.job.JobInstance;
import com.codeabovelab.dm.cluman.job.JobParameters;
import com.codeabovelab.dm.cluman.job.JobsManager;
import com.codeabovelab.dm.cluman.model.*;
import com.codeabovelab.dm.common.utils.Joiner;
import com.codeabovelab.dm.common.utils.Throwables;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

@AllArgsConstructor(onConstructor = @__(@Autowired))
@Component
public class SourceService {

    private final DiscoveryStorage discoveryStorage;
    private final ContainerSourceFactory containerSourceFactory;
    private final JobsManager jobsManager;

    /**
     * Create source of specified cluster.
     * @param name
     * @return root entry of source
     */
    public RootSource getClusterSource(String name) {
        NodesGroup cluster = discoveryStorage.getCluster(name);
        if(cluster == null) {
            return null;
        }
        RootSource root = new RootSource();
        root.getClusters().add(getClusterSourceInternal(cluster));
        return root;
    }

    private ClusterSource getClusterSourceInternal(NodesGroup cluster) {
        DockerService service = cluster.getDocker();
        ClusterSource clusterSrc = new ClusterSource();
        clusterSrc.setName(cluster.getName());
        List<NodeInfo> nil = service.getInfo().getNodeList();
        List<String> nodes = clusterSrc.getNodes();
        nil.forEach(ni -> nodes.add(ni.getName()));
        AbstractNodesGroupConfig<?> groupCfg = cluster.getConfig();
        NodesGroupConfig.copy(groupCfg, clusterSrc);
        if(groupCfg instanceof DockerBasedClusterConfig) {
            clusterSrc.setConfig(((DockerBasedClusterConfig)groupCfg).getConfig());
        }
        ContainersManager cm = cluster.getContainers();
        Collection<DockerContainer> containers = cm.getContainers();
        List<ContainerSource> containersSrc = clusterSrc.getContainers();
        for(DockerContainer dc: containers) {
            ContainerSource cd = containerSource(cluster, dc.getId());
            if(cd == null) {
                continue;
            }
            containersSrc.add(cd);
        }
        containersSrc.sort(null);
        return clusterSrc;
    }

    public ContainerSource containerSource(final NodesGroup service, final String id) {
        ContainerDetails container = service.getContainers().getContainer(id);
        if(container == null) {
            return null;
        }
        ContainerSource res = new ContainerSource();
        containerSourceFactory.toSource(container, res);
        res.setCluster(service.getName());
        return res;
    }

    /**
     * Create source for all system.
     * @return root entry of source
     */
    public RootSource getRootSource() {
        RootSource root = new RootSource();
        List<NodesGroup> clusters = discoveryStorage.getClusters();
        List<ClusterSource> clustersSrc = root.getClusters();
        for(NodesGroup group: clusters) {
            if(!(group instanceof SwarmCluster)) {
                continue;
            }
            ClusterSource cs = getClusterSourceInternal((SwarmCluster) group);
            clustersSrc.add(cs);
        }
        clustersSrc.sort(null);
        return root;
    }

    /**
     * Run job which deploy root source. <p/>
     * For deploy only one cluster you must simply use RootSource with one cluster.
     * @param root
     * @return
     */
    public JobInstance setRootSource(RootSource root, DeployOptions options) {
        JobParameters.Builder jpb = JobParameters.builder();
        jpb.setType(DeploySourceJob.NAME);
        Joiner<ClusterSource> joiner = Joiner.on(", ")
          .append("Deploy clusters: ")
          .<ClusterSource>converter((sb, i) -> sb.append('\"').append(i.getName()).append('\"'))
          .join(root.getClusters());
        jpb.setTitle(joiner.toString());
        jpb.parameter("source", root);
        if(options != null) {
            jpb.parameter("options", options);
        }
        JobInstance jobInstance = jobsManager.create(jpb.build());
        try {
            jobInstance.start().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw Throwables.asRuntime(e.getCause());
        }
        return jobInstance;
    }
}
