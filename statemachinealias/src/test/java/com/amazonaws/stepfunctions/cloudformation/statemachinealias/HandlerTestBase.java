package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.AWSStepFunctionsException;
import com.amazonaws.services.stepfunctions.model.RoutingConfigurationListItem;
import org.mockito.Mock;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class HandlerTestBase {

    // Exceptions
    protected final static AmazonServiceException exception500 = new AmazonServiceException("Server error");
    protected final static AmazonServiceException exception400 = new AmazonServiceException("Client error");
    protected final static AmazonServiceException resourceNotFoundException =
            new AmazonServiceException("Resource does not exist");
    protected final static AmazonServiceException throttlingException =
            new AmazonServiceException("Your request has been throttled");
    protected final static AWSStepFunctionsException iamManagedRuleException = new AWSStepFunctionsException(
            "arn:aws:iam::000000000000:role/role' is not authorized to create managed-rule.");
    protected final static AmazonServiceException accessDeniedException = new AmazonServiceException("");
    protected final static AmazonServiceException stateMachineAliasAlreadyExistsException = new AmazonServiceException(Constants.STATE_MACHINE_ALIAS_ALREADY_EXISTS);


    // Test constants
    protected final static String AWS_ACCOUNT_ID = "123456789012";
    protected final static String REGION = "us-east-1";
    protected final static Date CREATION_DATE = new Date();
    protected final static String ALIAS_NAME = "ALIAS";
    protected final static String STATE_MACHINE_ARN = "arn:aws:states:us-east-1:123456789012:stateMachine:TestStateMachine";
    protected final static String STATE_MACHINE_ALIAS_ARN = STATE_MACHINE_ARN + ":" + ALIAS_NAME;
    protected final static String STATE_MACHINE_VERSION_1_ARN = STATE_MACHINE_ARN + ":1";
    protected final static String STATE_MACHINE_VERSION_2_ARN = STATE_MACHINE_ARN + ":2";

    protected final static String DESCRIPTION = "TestStateMachine version description.";
    protected final static Set<RoutingConfigurationVersion> CFN_ROUTING_CONFIGURATION = new HashSet<>(Arrays.asList(
            new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 50),
            new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 50)
    ));
    protected final static CallbackContext context = CallbackContext.builder().build();
    protected final static Set<RoutingConfigurationListItem> SDK_ROUTING_CONFIGURATION = new HashSet<>(Arrays.asList(
            new RoutingConfigurationListItem()
                    .withStateMachineVersionArn(STATE_MACHINE_VERSION_1_ARN)
                    .withWeight(50),
            new RoutingConfigurationListItem()
                    .withStateMachineVersionArn(STATE_MACHINE_VERSION_2_ARN)
                    .withWeight(50)
    ));
    protected final String DEPLOYMENT_PREFERENCE_TYPE_ALL_AT_ONCE = "ALL_AT_ONCE";
    protected final String DEPLOYMENT_PREFERENCE_TYPE_LINEAR = "LINEAR";
    protected final String DEPLOYMENT_PREFERENCE_TYPE_CANARY = "CANARY";

    static {
        exception400.setStatusCode(400);
        exception500.setStatusCode(500);
        throttlingException.setStatusCode(400);
        resourceNotFoundException.setStatusCode(400);
        throttlingException.setErrorCode("ThrottlingException");
        accessDeniedException.setErrorCode(Constants.ACCESS_DENIED_ERROR_CODE);
        iamManagedRuleException.setErrorCode("AccessDeniedException");
        iamManagedRuleException.setStatusCode(400);
        resourceNotFoundException.setErrorCode(Constants.RESOURCE_NOT_FOUND_ERROR_CODE);
        stateMachineAliasAlreadyExistsException.setStatusCode(400);
        stateMachineAliasAlreadyExistsException.setErrorCode(Constants.STATE_MACHINE_ALIAS_ALREADY_EXISTS);
    }

    @Mock
    protected AmazonWebServicesClientProxy proxy;

    @Mock
    protected AmazonCloudWatch cwClient;

    @Mock
    protected AWSStepFunctions sfnClient;

    @Mock
    protected Logger logger;

    protected ResourceHandler handler;
    protected AmazonWebServiceRequest awsRequest;
    protected ResourceHandlerRequest<ResourceModel> cfnRequest;

    protected AmazonServiceException createAndMockAmazonServiceException(final String errorCode) {
        final AmazonServiceException amazonServiceException = new AmazonServiceException(errorCode);
        amazonServiceException.setStatusCode(400);
        amazonServiceException.setErrorCode(errorCode);

        when(proxy.injectCredentialsAndInvoke(eq(awsRequest), any(Function.class))).thenThrow(amazonServiceException);

        return amazonServiceException;
    }

    protected void assertFailure(final String message, final HandlerErrorCode code) {
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, cfnRequest, context, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getErrorCode()).isEqualTo(code);
    }

    protected DeploymentPreference getAllAtOnceDeploymentPreference(final String stateMachineVersionArn) {
        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_ALL_AT_ONCE);
        deploymentPreference.setStateMachineVersionArn(stateMachineVersionArn);
        return deploymentPreference;
    }

    protected DeploymentPreference getLinearDeploymentPreference(final String versionArn, final int interval, final int percentage) {
        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_LINEAR);
        deploymentPreference.setStateMachineVersionArn(versionArn);
        deploymentPreference.setInterval(interval);
        deploymentPreference.setPercentage(percentage);
        return deploymentPreference;
    }

    protected DeploymentPreference getCanaryDeploymentPreference(final String versionArn, final int interval, final int percentage) {
        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_CANARY);
        deploymentPreference.setStateMachineVersionArn(versionArn);
        deploymentPreference.setInterval(interval);
        deploymentPreference.setPercentage(percentage);
        return deploymentPreference;
    }

    protected Set<RoutingConfigurationVersion> getVersionRoutingConfigCfn(final String versionArn) {
        final Set<RoutingConfigurationVersion> routingConfig = new HashSet<>();
        routingConfig.add(new RoutingConfigurationVersion(versionArn, 100));
        return routingConfig;
    }

    protected Set<RoutingConfigurationVersion> getVersionRoutingConfigCfn(final String originVersionArn, final int originVersionWeight,
                                                                          final String targetVersionArn, final int targetVersionWeight) {
        final Set<RoutingConfigurationVersion> routingConfig = new HashSet<>();
        routingConfig.add(new RoutingConfigurationVersion(originVersionArn, originVersionWeight));
        routingConfig.add(new RoutingConfigurationVersion(targetVersionArn, targetVersionWeight));
        return routingConfig;
    }

    protected List<RoutingConfigurationListItem> getVersionRoutingConfigSdk(final String versionArn) {
        return Collections.singletonList(
                new RoutingConfigurationListItem().withStateMachineVersionArn(versionArn).withWeight(100)
        );
    }

    protected List<RoutingConfigurationListItem> getVersionRoutingConfigSdk(final String originVersionArn, final int originVersionWeight,
                                                                            final String targetVersionArn, final int targetVersionWeight) {
        return Arrays.asList(
                new RoutingConfigurationListItem().withStateMachineVersionArn(originVersionArn).withWeight(originVersionWeight),
                new RoutingConfigurationListItem().withStateMachineVersionArn(targetVersionArn).withWeight(targetVersionWeight)

        );
    }
}
