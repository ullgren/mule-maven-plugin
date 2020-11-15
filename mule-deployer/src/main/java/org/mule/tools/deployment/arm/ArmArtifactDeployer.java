/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.deployment.arm;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;

import org.mule.tools.client.arm.ApplicationMetadata;
import org.mule.tools.client.arm.ArmClient;
import org.mule.tools.client.core.exception.DeploymentException;
import org.mule.tools.deployment.artifact.ArtifactDeployer;
import org.mule.tools.model.Deployment;
import org.mule.tools.model.anypoint.ArmDeployment;
import org.mule.tools.utils.DeployerLog;
import org.mule.tools.utils.PropertiesUtils;
import org.mule.tools.verification.DeploymentVerification;
import org.mule.tools.verification.arm.ArmDeploymentVerification;

/**
 * Deploys mule artifacts to ARM, using the {@link ArmClient}.
 */
public class ArmArtifactDeployer implements ArtifactDeployer {

  private static final Long DEFAULT_ARM_DEPLOYMENT_TIMEOUT = 1200000L;

  private ArmClient client;
  private final DeployerLog log;
  private Integer applicationId;
  private final ArmDeployment deployment;

  public ArmArtifactDeployer(Deployment deployment, DeployerLog log) {
    this(deployment, new ArmClient(deployment, log), log);
  }

  protected ArmArtifactDeployer(Deployment deployment, ArmClient client, DeployerLog log) {
    checkArgument(client != null, "The client must not be null.");

    this.log = log;
    this.client = client;
    this.deployment = (ArmDeployment) deployment;
    if (this.deployment.getDeploymentTimeout() == null) {
      this.deployment.setDeploymentTimeout(DEFAULT_ARM_DEPLOYMENT_TIMEOUT);
    }
  }

  /**
   * Retrieves the application metadata based on the deployment configuration.
   * 
   * @return The application metadata
   * @throws DeploymentException
   */
  public ApplicationMetadata getApplicationMetadata() throws DeploymentException {
    try {
      return new ApplicationMetadata(deployment.getArtifact(), deployment.getApplicationName(), deployment.getTargetType(),
                                     deployment.getTarget(),
                                     PropertiesUtils.resolvePropertiesFromFile(deployment.getProperties(),
                                                                               deployment.getPropertiesFile(),
                                                                               false));
    } catch (IOException e) {
      throw new DeploymentException(e.getMessage(), e);
    }
  }

  /**
   * Returns the value of the property failIfNotExists. The default value is true.
   * 
   * @return If set in the deployment configuration, the failIfNotExists property value. Otherwise, it returns true.
   */
  public boolean isFailIfNotExists() {
    Boolean isFailIfNotExists = deployment.isFailIfNotExists();
    return isFailIfNotExists != null ? isFailIfNotExists : true;
  }

  @Override
  public void deployDomain() throws DeploymentException {
    throw new DeploymentException("Deployment of domains to ARM is not supported");
  }

  @Override
  public void undeployDomain() throws DeploymentException {
    throw new DeploymentException("Undeployment of domains to ARM is not supported");
  }

  /**
   * Deploys the application specified in the {@link Deployment} to ARM through the {@link ArmClient}.
   *
   * @throws DeploymentException
   */
  @Override
  public void deployApplication() throws DeploymentException {
    client.deployApplication(getApplicationMetadata());
    if (!deployment.getSkipDeploymentVerification()) {
      checkApplicationHasStarted();
    }
  }

  /**
   * Checks if an application in ARM has the {@code STARTED_STATUS} status.
   *
   * @throws DeploymentException In case it timeouts while checking for the status
   */
  protected void checkApplicationHasStarted() throws DeploymentException {
    log.info("Checking application: " + deployment.getApplicationName() + " has started");
    DeploymentVerification verification = getDeploymentVerification();
    verification.assertDeployment(deployment);
  }


  /**
   * Undeploys the application specified in the {@link Deployment} to ARM through the {@link ArmClient}.
   *
   * @throws DeploymentException
   */
  @Override
  public void undeployApplication() throws DeploymentException {
    client.undeployApplication(getApplicationMetadata());
  }

  /**
   * Redeploys the application specified in the {@link Deployment} to ARM through the {@link ArmClient}.
   *
   * @throws DeploymentException
   */
  public void redeployApplication() throws DeploymentException {
    log.info("Found " + getApplicationMetadata().toString() + ". Redeploying application...");
    client.redeployApplication(getApplicationId(), getApplicationMetadata());
  }

  /**
   * Retrieves the application id through the {@link ArmClient}.
   * 
   * @return The application id or null if it cannot find the application.
   * @throws DeploymentException
   */
  public Integer getApplicationId() throws DeploymentException {
    if (applicationId == null) {
      applicationId = client.findApplicationId(getApplicationMetadata());
    }
    return applicationId;
  }

  /**
   * Retrieves the application name.
   * 
   * @return The application name.
   */
  protected String getApplicationName() {
    return deployment.getApplicationName();
  }

  public DeploymentVerification getDeploymentVerification() throws DeploymentException {
    return new ArmDeploymentVerification(client, getApplicationId());
  }
}
