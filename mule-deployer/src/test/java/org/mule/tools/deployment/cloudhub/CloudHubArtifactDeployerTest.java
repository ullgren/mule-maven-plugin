/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.deployment.cloudhub;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mule.tools.client.cloudhub.CloudHubClient;
import org.mule.tools.client.cloudhub.model.Application;
import org.mule.tools.client.core.exception.DeploymentException;
import org.mule.tools.model.anypoint.CloudHubDeployment;
import org.mule.tools.utils.DeployerLog;
import org.mule.tools.verification.cloudhub.CloudHubDeploymentVerification;

public class CloudHubArtifactDeployerTest {

  private static final String FAKE_APPLICATION_NAME = "fake-name";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private File applicationFile;
  private DeployerLog logMock;
  private CloudHubClient clientMock;
  private Application applicationMock;
  private CloudHubDeployment deploymentMock;
  private CloudHubArtifactDeployer cloudHubArtifactDeployerSpy;

  private CloudHubArtifactDeployer cloudHubArtifactDeployer;

  @Before
  public void setUp() throws IOException {
    applicationFile = temporaryFolder.newFile();

    logMock = mock(DeployerLog.class);
    clientMock = mock(CloudHubClient.class);
    applicationMock = mock(Application.class);
    deploymentMock = mock(CloudHubDeployment.class);

    when(deploymentMock.getApplicationName()).thenReturn(FAKE_APPLICATION_NAME);
    when(deploymentMock.getMuleVersion()).thenReturn("4.0.0");
    when(deploymentMock.getArtifact()).thenReturn(applicationFile);
    when(deploymentMock.getWorkers()).thenReturn(1);
    when(deploymentMock.getWorkerType()).thenReturn("Micro");


    when(clientMock.getApplications(FAKE_APPLICATION_NAME)).thenReturn(applicationMock);

    cloudHubArtifactDeployer = new CloudHubArtifactDeployer(deploymentMock, clientMock, logMock);
    cloudHubArtifactDeployerSpy = spy(cloudHubArtifactDeployer);
  }

  @Test(expected = DeploymentException.class)
  public void deployDomainTest() throws DeploymentException {
    cloudHubArtifactDeployer.deployDomain();
  }

  @Test(expected = DeploymentException.class)
  public void undeployDomainTest() throws DeploymentException {
    cloudHubArtifactDeployer.undeployDomain();
  }

  @Test
  public void deployApplicationNew() throws DeploymentException {
    when(clientMock.isDomainAvailable(any(String.class))).thenReturn(true);

    doNothing().when(cloudHubArtifactDeployerSpy).checkApplicationHasStarted();

    cloudHubArtifactDeployerSpy.deployApplication();

    verify(clientMock).isDomainAvailable(any(String.class));
    verify(cloudHubArtifactDeployerSpy).createOrUpdateApplication();
    verify(cloudHubArtifactDeployerSpy).createApplication();
    verify(cloudHubArtifactDeployerSpy).startApplication();
    verify(clientMock).startApplications(FAKE_APPLICATION_NAME);
    verify(cloudHubArtifactDeployerSpy).checkApplicationHasStarted();
  }

  @Test
  public void deployApplicationSkipVerification() throws DeploymentException {
    when(clientMock.isDomainAvailable(any(String.class))).thenReturn(true);

    when(deploymentMock.getSkipDeploymentVerification()).thenReturn(true);

    cloudHubArtifactDeployerSpy.deployApplication();


    verify(clientMock).isDomainAvailable(any(String.class));
    verify(cloudHubArtifactDeployerSpy).createOrUpdateApplication();
    verify(cloudHubArtifactDeployerSpy).createApplication();
    verify(cloudHubArtifactDeployerSpy).startApplication();
    verify(clientMock).startApplications(FAKE_APPLICATION_NAME);
    verify(cloudHubArtifactDeployerSpy, never()).checkApplicationHasStarted();
  }

  @Test
  public void deployApplicationUpdate() throws DeploymentException {
    when(clientMock.isDomainAvailable(any(String.class))).thenReturn(false);

    doNothing().when(cloudHubArtifactDeployerSpy).checkApplicationHasStarted();

    cloudHubArtifactDeployerSpy.deployApplication();

    verify(clientMock).isDomainAvailable(any(String.class));
    verify(cloudHubArtifactDeployerSpy).createOrUpdateApplication();
    verify(cloudHubArtifactDeployerSpy).updateApplication();
    verify(cloudHubArtifactDeployerSpy).startApplication();
    verify(clientMock).startApplications(FAKE_APPLICATION_NAME);
    verify(cloudHubArtifactDeployerSpy).checkApplicationHasStarted();
  }

  @Test
  public void deployApplicationVerificationStartedFail() throws DeploymentException {
    expectedException.expect(DeploymentException.class);

    when(clientMock.isDomainAvailable(any(String.class))).thenReturn(true);

    CloudHubDeploymentVerification verificationMock = mock(CloudHubDeploymentVerification.class);
    doThrow(DeploymentException.class).when(verificationMock).assertDeployment(deploymentMock);
    cloudHubArtifactDeployer.setDeploymentVerification(verificationMock);

    cloudHubArtifactDeployer.deployApplication();

    verify(clientMock).isDomainAvailable(any(String.class));
    verify(cloudHubArtifactDeployerSpy).createOrUpdateApplication();
    verify(cloudHubArtifactDeployerSpy).createApplication();
    verify(cloudHubArtifactDeployerSpy).startApplication();
    verify(clientMock).startApplications(FAKE_APPLICATION_NAME);
    verify(cloudHubArtifactDeployerSpy).checkApplicationHasStarted();
    verify(verificationMock).assertDeployment(eq(deploymentMock));
  }

  @Test
  public void undeployApplicationTest() throws DeploymentException {
    cloudHubArtifactDeployer.undeployApplication();
    verify(clientMock, times(1)).stopApplications(FAKE_APPLICATION_NAME);
    verify(clientMock, times(1)).deleteApplications(FAKE_APPLICATION_NAME);
  }

  @Test
  public void getApplicationNameTest() {
    assertThat("Application name is not the expected", cloudHubArtifactDeployer.getApplicationName(),
               equalTo(FAKE_APPLICATION_NAME));
  }
}
