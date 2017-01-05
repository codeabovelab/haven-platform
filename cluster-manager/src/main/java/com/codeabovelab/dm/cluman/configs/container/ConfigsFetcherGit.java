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

import com.codeabovelab.dm.cluman.configuration.DataLocationConfiguration;
import com.codeabovelab.dm.cluman.utils.ContainerUtils;
import com.codeabovelab.dm.common.utils.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import static java.util.Collections.singleton;
import static org.springframework.util.StringUtils.hasText;

/**
 * Fetches configs from remote REPO
 * Cluster name = dir
 */
@Slf4j
@Component
@Order(1)
@ConditionalOnProperty("dm.image.configuration.git.url")
public class ConfigsFetcherGit implements ConfigsFetcher {

    private static final String HEAD = "refs/heads/";

    private final Path gitDirPath;
    private final GitSettings gitSettings;
    private final List<Parser> parser;
    private final Git git;
    private final CredentialsProvider cp;

    @Autowired
    public ConfigsFetcherGit(GitSettings gitSettings, DataLocationConfiguration location, List<Parser> parser) {
        this.gitSettings = gitSettings;
        this.parser = parser;
        this.gitDirPath = new File(location.getLocation(), "git-container-configs").toPath();
        this.cp = hasText(gitSettings.getPassword()) ? new UsernamePasswordCredentialsProvider(gitSettings.getUsername(), gitSettings.getPassword()) : null;
        this.git = initGitRepo();
    }

    @Override
    public synchronized void resolveProperties(ContainerCreationContext context) {

        try {
            git.pull().setCredentialsProvider(cp).call();
            log.info("repo {} was updated", gitDirPath);
            String clusterName = context.getCluster();
            String imageNameWithoutPrefix = ContainerUtils.getImageNameWithoutPrefix(context.getImageName());
            String imageVersionName = ContainerUtils.getImageVersionName(context.getImageName());
            String path = gitDirPath + File.separator + clusterName + File.separator;
            parser.forEach(ps -> {
                // search in base dir w/o version
                ps.parse(imageNameWithoutPrefix, context);
                // search in base dir with version
                ps.parse(imageVersionName, context);
                // search in cluster dir w/o version
                ps.parse(path + imageNameWithoutPrefix, context);
                // search in cluster dir with version
                ps.parse(path + imageVersionName, context);
            });
        } catch (Exception e) {
            log.error("", e);
        }
    }

    private Git initGitRepo() {
        try {
            log.info("try to init repo {}", gitSettings.getUrl());
            File gitDir = gitDirPath.toFile();
            FileUtils.deleteQuietly(gitDir);
            gitDir.mkdirs();

            Git git = Git.cloneRepository()
                    .setURI(gitSettings.getUrl())
                    .setCredentialsProvider(cp)
                    .setDirectory(gitDir)
                    .setBranchesToClone(singleton(HEAD + gitSettings.getBranch()))
                    .setBranch(HEAD + gitSettings.getBranch())
                    .call();
            log.info("repo was cloned from url: {} to dir: {}, branch: {} ", gitSettings.getUrl(), gitDir, git.getRepository().getBranch());
            return git;
        } catch (Exception e) {
            throw Throwables.asRuntime(e);
        }
    }

}
