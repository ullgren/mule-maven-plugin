/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.deployment.fabric;

import static com.google.common.base.Preconditions.checkArgument;

import org.mule.tools.client.core.exception.ClientException;
import org.mule.tools.client.core.exception.DeploymentException;
import org.mule.tools.client.fabric.RuntimeFabricClient;
import org.mule.tools.client.fabric.model.DeploymentModify;
import org.mule.tools.client.fabric.model.DeploymentRequest;
import org.mule.tools.client.fabric.model.Target;
import org.mule.tools.deployment.artifact.ArtifactDeployer;
import org.mule.tools.model.Deployment;
import org.mule.tools.model.anypoint.RuntimeFabricDeployment;
import org.mule.tools.utils.DeployerLog;
import org.mule.tools.verification.DeploymentVerification;
import org.mule.tools.verification.fabric.RuntimeFabricDeploymentVerification;

public class RuntimeFabricArtifactDeployer implements ArtifactDeployer {

  private static final Long DEFAULT_RUNTIME_FABRIC_DEPLOYMENT_TIMEOUT = 1200000L;
  public static final int BAD_REQUEST = 400;
  private RequestBuilder requestBuilder;
  private RuntimeFabricClient client;
  private final DeployerLog log;
  private final RuntimeFabricDeployment deployment;

  public RuntimeFabricArtifactDeployer(Deployment deployment, DeployerLog log) {
    this(deployment, new RuntimeFabricClient((RuntimeFabricDeployment) deployment, log), log);
  }

  protected RuntimeFabricArtifactDeployer(Deployment deployment, RuntimeFabricClient client, DeployerLog log) {
    checkArgument(client != null, "The client must not be null.");

    this.log = log;
    this.client = client;
    this.deployment = (RuntimeFabricDeployment) deployment;
    this.requestBuilder = new RequestBuilder(this.deployment, this.client);
    if (this.deployment.getDeploymentTimeout() == null) {
      this.deployment.setDeploymentTimeout(DEFAULT_RUNTIME_FABRIC_DEPLOYMENT_TIMEOUT);
    }
  }

  @Override
  public void deployApplication() throws DeploymentException {
    try {
      log.info("Starting deployment to " + deployment.getTarget());
      DeploymentRequest request = requestBuilder.buildDeploymentRequest();
      client.deploy(request);
    } catch (ClientException e) {
      if (e.getStatusCode() == BAD_REQUEST) {
        redeployApplication();
      } else {
        throw new DeploymentException("Could not deploy application.", e);
      }
    }
    if (!deployment.getSkipDeploymentVerification()) {
      checkApplicationHasStarted(getDepoymentId());
    }
  }

  /**
   * Retrieves the deployment id through the {@link RequestBuilder}.
   * 
   * @return The deployment id or null if it cannot find the application.
   */
  public String getDepoymentId() {
    String deploymentId;
    try {
      deploymentId = requestBuilder.getDeploymentId(requestBuilder.buildTarget());
    } catch (Exception e) {
      deploymentId = null;
    }
    return deploymentId;
  }

  private void redeployApplication() throws DeploymentException {
    try {
      DeploymentModify modify = requestBuilder.buildDeploymentModify();
      String deploymentId = requestBuilder.getDeploymentId(modify.target);
      client.redeploy(modify, deploymentId);
    } catch (IllegalStateException e) {
      throw new DeploymentException("Could not redeploy application.", e);
    }

  }

  @Override
  public void undeployApplication() throws DeploymentException {
    try {
      Target target = requestBuilder.buildTarget();
      String deploymentId = requestBuilder.getDeploymentId(target);
      client.deleteDeployment(deploymentId);
    } catch (ClientException | IllegalStateException e) {
      throw new DeploymentException("Could not undeploy application.", e);
    }
  }

  @Override
  public void deployDomain() throws DeploymentException {
    throw new DeploymentException("Deployment of domains to Runtime Fabric is not supported");

  }

  @Override
  public void undeployDomain() throws DeploymentException {
    throw new DeploymentException("Undeployment of domains to Runtime Fabric is not supported");
  }


  public void setRequestBuilder(RequestBuilder requestBuilder) {
    this.requestBuilder = requestBuilder;
  }

  /**
   * Checks if an application in Runtime Fabric has the {@code APPLIED_STATUS} status.
   *
   * @param deploymentId Id of the deployment to check.
   * @throws DeploymentException In case it timeouts while checking for the status
   */
  protected void checkApplicationHasStarted(String deploymentId) throws DeploymentException {
    log.info("Checking if application: " + deployment.getApplicationName() + " has started");
    DeploymentVerification verification = new RuntimeFabricDeploymentVerification(client, deploymentId);
    verification.assertDeployment(deployment);
  }
}
