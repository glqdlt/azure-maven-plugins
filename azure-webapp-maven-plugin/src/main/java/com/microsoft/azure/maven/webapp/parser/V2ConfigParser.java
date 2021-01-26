/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.maven.webapp.parser;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.maven.MavenDockerCredentialProvider;
import com.microsoft.azure.maven.utils.MavenArtifactUtils;
import com.microsoft.azure.maven.webapp.AbstractWebAppMojo;
import com.microsoft.azure.maven.webapp.configuration.MavenRuntimeSetting;
import com.microsoft.azure.maven.webapp.validator.AbstractConfigurationValidator;
import com.microsoft.azure.toolkits.appservice.model.DeployType;
import com.microsoft.azure.toolkits.appservice.model.DockerConfiguration;
import com.microsoft.azure.toolkits.appservice.model.JavaVersion;
import com.microsoft.azure.toolkits.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkits.appservice.model.Runtime;
import com.microsoft.azure.toolkits.appservice.model.WebContainer;
import com.microsoft.azure.tools.common.model.Region;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class V2ConfigParser extends AbstractConfigParser {
    public V2ConfigParser(AbstractWebAppMojo mojo, AbstractConfigurationValidator validator) {
        super(mojo, validator);
    }

    @Override
    public Region getRegion() throws AzureExecutionException {
        validate(validator::validateRegion);
        return Region.fromName(mojo.getRegion());
    }

    @Override
    public DockerConfiguration getDockerConfiguration() throws AzureExecutionException {
        final MavenRuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null) {
            return null;
        }
        final OperatingSystem os = getOs();
        if (os != OperatingSystem.DOCKER) {
            return null;
        }
        validate(validator::validateImage);
        final MavenDockerCredentialProvider credentialProvider = getDockerCredential(runtime.getServerId());
        return DockerConfiguration.builder()
                .registryUrl(runtime.getRegistryUrl())
                .image(runtime.getImage())
                .userName(credentialProvider.getUsername())
                .password(credentialProvider.getPassword()).build();
    }

    @Override
    public List<Pair<File, DeployType>> getResources() throws AzureExecutionException {
        if (mojo.getDeployment() == null || mojo.getDeployment().getResources() == null) {
            return Collections.EMPTY_LIST;
        }
        final List<File> files = MavenArtifactUtils.getArtifacts(mojo.getDeployment().getResources());
        return files.stream().map(file -> Pair.of(file, getDeployTypeFromFile(file))).collect(Collectors.toList());
    }

    @Override
    public Runtime getRuntime() throws AzureExecutionException {
        final MavenRuntimeSetting runtime = mojo.getRuntime();
        if (runtime == null) {
            return null;
        }
        final OperatingSystem os = getOs();
        if (os == OperatingSystem.DOCKER) {
            return Runtime.DOCKER;
        }
        validate(validator::validateJavaVersion);
        validate(validator::validateWebContainer);
        validate(validator::validateRuntimeStack);
        final JavaVersion javaVersion = JavaVersion.fromString(runtime.getJavaVersionRaw());
        final WebContainer webContainer = WebContainer.fromString(runtime.getWebContainerRaw());
        return Runtime.getRuntime(os, webContainer, javaVersion);
    }

    private OperatingSystem getOs() throws AzureExecutionException {
        validate(validator::validateOs);
        final MavenRuntimeSetting runtime = mojo.getRuntime();
        return OperatingSystem.fromString(runtime.getOs());
    }
}
