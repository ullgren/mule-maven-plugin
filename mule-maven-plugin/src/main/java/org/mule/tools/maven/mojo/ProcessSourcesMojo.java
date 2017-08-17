/*
 * Mule ESB Maven Tools
 * <p>
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * <p>
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.maven.mojo;

import static java.lang.String.format;

import org.apache.maven.RepositoryUtils;
import org.eclipse.aether.repository.RemoteRepository;
import org.mule.maven.client.internal.AetherMavenClient;
import org.mule.tools.api.classloader.model.ApplicationClassLoaderModelAssembler;
import org.mule.tools.api.classloader.model.ClassLoaderModel;
import org.mule.tools.api.repository.ArtifactInstaller;
import org.mule.tools.api.repository.MuleMavenPluginClientProvider;
import org.mule.tools.api.repository.RepositoryGenerator;

import java.text.MessageFormat;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "process-sources",
    defaultPhase = LifecyclePhase.PROCESS_SOURCES,
    requiresDependencyResolution = ResolutionScope.RUNTIME)
public class ProcessSourcesMojo extends AbstractMuleMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    long start = System.currentTimeMillis();
    getLog().debug("Processing sources...");

    if (!lightweightPackage) {
      RepositoryGenerator repositoryGenerator =
          new RepositoryGenerator(project.getFile(), outputDirectory, new ArtifactInstaller(getLog()),
                                  getClassLoaderModelAssembler());
      try {
        ClassLoaderModel classLoaderModel = repositoryGenerator.generate();
        getContentGenerator().createApplicationClassLoaderModelJsonFile(classLoaderModel);
      } catch (Exception e) {
        String message = format("There was an exception while creating the repository of [%s]", project.toString());
        throw new MojoFailureException(message, e);
      }
    }

    getLog().debug(MessageFormat.format("Process sources done ({0}ms)", System.currentTimeMillis() - start));
  }


  protected ApplicationClassLoaderModelAssembler getClassLoaderModelAssembler() {
    List<RemoteRepository> remoteRepositories = RepositoryUtils.toRepos(remoteArtifactRepositories);
    AetherMavenClient aetherMavenClient = new MuleMavenPluginClientProvider(remoteRepositories,
                                                                            getLog())
                                                                                .buildMavenClient();
    return new ApplicationClassLoaderModelAssembler(aetherMavenClient);
  }
}