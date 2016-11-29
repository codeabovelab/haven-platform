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

package com.codeabovelab.dm.cluman.configs.container;

import com.codeabovelab.dm.cluman.configuration.DataLocatinConfiguration;
import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import com.codeabovelab.dm.common.utils.Throwables;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.singleton;

/**
 * Fetches configs from remote REPO
 * Env name = branch if exists
 * Cluster name = dir
 */
@Component
@Order(1)
@ConditionalOnBean(GitSettings.class)
public class ConfigsFetcherGit implements ConfigsFetcher {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigsFetcherGit.class);
    private final static String HEAD = "refs/heads/";

    private final Path gitDirPath;
    private final GitSettings gitSettings;
    private final List<Parser> parser;
    private final List<Function<String, String>> functions = new ArrayList<>(Arrays.asList(new NameFunction(), new NameVersionFunction()));
    private volatile Git git;
    private CredentialsProvider cp;

    @Autowired
    public ConfigsFetcherGit(GitSettings gitSettings, DataLocatinConfiguration locatinConfiguration, List<Parser> parser) {
        this.gitSettings = gitSettings;
        this.parser = parser;
        this.gitDirPath = new File(locatinConfiguration.getLocation(), "git-container-configs").toPath();

    }

    @Override
    public synchronized void resolveProperties(ContainerCreationContext context) {

        try {
            initGitRepo();
            git.fetch().setCredentialsProvider(cp).call();
//            tryBranch(context.getCluster());
            git.pull().setCredentialsProvider(cp).call();
            LOG.info("repo was updated");
            String clusterName = context.getCluster();
            String path = gitDirPath + File.separator + clusterName + File.separator;
            for (Function<String, String> function : functions) {
                for (Parser ps : parser) {
                    ps.parse(path + function.apply(context.getImageName()), context);
                }
            }
        } catch (Exception e) {
            LOG.error("", e);
        }
    }

    private void tryBranch(String cluster) {
        String envName = /*TODO getEnvName*/cluster;
        try {
            Assert.notNull(envName);
            git.checkout()
                    .setCreateBranch(true)
                    .setName(HEAD + envName)
                    .setStartPoint("origin/" + envName)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                    .call();
            LOG.info("co branch  {} ", cluster);
        } catch (Exception e) {
            LOG.warn("branch '{}' doesn't exist, use default: {}", envName, gitSettings.getBranch());
            try {
                git.checkout()
                        .setName(HEAD + gitSettings.getBranch())
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                        .call();
            } catch (GitAPIException e1) {
                LOG.error("branch can't be switched", e1);
            }

        }
    }

    @PostConstruct
    private void initGitRepo() {
        if (git == null) {
            try {
                LOG.info("try to init repo {}", gitSettings.getUrl());
                File gitDir = gitDirPath.toFile();
                FileUtils.deleteQuietly(gitDir);
                gitDir.mkdirs();
                if (StringUtils.hasText(gitSettings.getPassword())) {
                    cp = new UsernamePasswordCredentialsProvider(gitSettings.getUsername(), gitSettings.getPassword());
                }
                git = Git.cloneRepository()
                        .setURI(gitSettings.getUrl())
                        .setCredentialsProvider(cp)
                        .setDirectory(gitDir)
                        .setBranchesToClone(singleton(HEAD + gitSettings.getBranch()))
                        .setBranch(HEAD + gitSettings.getBranch())
                        .call();
                LOG.info("repo was cloned from url: {} to dir: {}, branch: {} ", gitSettings.getUrl(), gitDir, git.getRepository().getBranch());
            } catch (Exception e) {
                git = null;
                throw Throwables.asRuntime(e);
            }
        }
    }

    private final static class NameVersionFunction implements Function<String, String> {

        @Override
        public String apply(String s) {
            return ContainerUtils.getImageVersionName(s).replace(":", "-");
        }
    }

    private final static class NameFunction implements Function<String, String> {

        @Override
        public String apply(String s) {
            return ContainerUtils.getImageNameWithoutPrefix(s);
        }
    }
}
