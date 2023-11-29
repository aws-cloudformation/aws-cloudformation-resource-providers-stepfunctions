package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.DescribeAlarmsResult;
import com.amazonaws.services.cloudwatch.model.MetricAlarm;
import com.amazonaws.services.cloudwatch.model.StateValue;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineAliasRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineAliasResult;
import com.amazonaws.services.stepfunctions.model.RoutingConfigurationListItem;
import com.amazonaws.services.stepfunctions.model.UpdateStateMachineAliasRequest;
import com.amazonaws.services.stepfunctions.model.UpdateStateMachineAliasResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Instant;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UpdateHandlerTest extends HandlerTestBase {

    private static final Date UPDATE_DATE = new Date();

    @BeforeEach
    public void setup() {
        handler = new UpdateHandler();

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .routingConfiguration(CFN_ROUTING_CONFIGURATION)
                        .build())
                .build();

        awsRequest = new UpdateStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withDescription(DESCRIPTION)
                .withRoutingConfiguration(SDK_ROUTING_CONFIGURATION);
    }

    @Test
    public void testSuccess() {
        final UpdateStateMachineAliasResult updateStateMachineAliasResult = new UpdateStateMachineAliasResult()
                .withUpdateDate(UPDATE_DATE);

        when(proxy.injectCredentialsAndInvoke(eq(awsRequest), any(Function.class))).thenReturn(updateStateMachineAliasResult);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, cfnRequest, null, logger);

        final ResourceModel expectedModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(CFN_ROUTING_CONFIGURATION)
                .build();

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void test500() {
        when(proxy.injectCredentialsAndInvoke(eq(awsRequest), any(Function.class))).thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, cfnRequest, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exception500.getMessage());
    }

    @Test
    public void testStateMachineInvalidRequestErrors() {
        final List<String> invalidRequestErrorCodes = Arrays.asList(
                Constants.INVALID_ARN_ERROR_CODE, Constants.VALIDATION_ERROR_CODE
        );

        for (String errorCode: invalidRequestErrorCodes) {
            final AmazonServiceException invalidRequestException = createAndMockAmazonServiceException(errorCode);
            assertFailure(invalidRequestException.getMessage(), HandlerErrorCode.InvalidRequest);
        }
    }

    @Test
    public void testResourceNotFoundError() {
        final AmazonServiceException exceptionThrottling = createAndMockAmazonServiceException(Constants.RESOURCE_NOT_FOUND_ERROR_CODE);
        assertFailure(exceptionThrottling.getMessage(), HandlerErrorCode.NotFound);
    }

    @Test
    public void testAccessDeniedError() {
        final AmazonServiceException exceptionAccessDenied = createAndMockAmazonServiceException(Constants.ACCESS_DENIED_ERROR_CODE);
        assertFailure(exceptionAccessDenied.getMessage(), HandlerErrorCode.AccessDenied);
    }

    @Test
    public void testThrottlingError() {
        final AmazonServiceException exceptionThrottling = createAndMockAmazonServiceException(Constants.THROTTLING_ERROR_CODE);
        assertFailure(exceptionThrottling.getMessage(), HandlerErrorCode.Throttling);
    }

    @Test
    public void testServiceInternalError() {
        when(proxy.injectCredentialsAndInvoke(eq(awsRequest), any(Function.class))).thenThrow(exception500);
        assertFailure(exception500.getMessage(), HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void testHandleGradualDeployment_whenPreviousResourceStateHasSameVersion_thenPerformsSimpleUpdate() {
        final Set<RoutingConfigurationVersion> previousRoutingConfig = new HashSet<>(Collections.singletonList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 100)
        ));
        final DeploymentPreference desiredDeploymentPreference = getLinearDeploymentPreference(STATE_MACHINE_VERSION_1_ARN, 1, 1);
        final Set<RoutingConfigurationVersion> newRoutingConfigCfn = getVersionRoutingConfigCfn(STATE_MACHINE_VERSION_1_ARN);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(desiredDeploymentPreference)
                        .build())
                .previousResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .routingConfiguration(previousRoutingConfig)
                        .build())
                .build();

        final ResourceModel expectedResourceModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(newRoutingConfigCfn)
                .deploymentPreference(desiredDeploymentPreference)
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> actual = handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(actual.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(actual.getResourceModels()).isNull();
        assertThat(actual.getMessage()).isNull();
        assertThat(actual.getErrorCode()).isNull();
    }

    @Test
    public void testHandleAllAtOnceDeployment_whenPreviousResourceStateHasDeploymentPreference_thenReturnsSuccess() {
        final DeploymentPreference previousDeploymentPreference = getAllAtOnceDeploymentPreference(STATE_MACHINE_VERSION_1_ARN);
        final DeploymentPreference desiredDeploymentPreference = getAllAtOnceDeploymentPreference(STATE_MACHINE_VERSION_2_ARN);
        final Set<RoutingConfigurationVersion> newRoutingConfigCfn = getVersionRoutingConfigCfn(STATE_MACHINE_VERSION_2_ARN);
        final List<RoutingConfigurationListItem> newRoutingConfigSdk = getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_2_ARN);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(desiredDeploymentPreference)
                        .build())
                .previousResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(previousDeploymentPreference)
                        .build())
                .build();

        final ResourceModel expectedResourceModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(newRoutingConfigCfn)
                .deploymentPreference(desiredDeploymentPreference)
                .build();

        final UpdateStateMachineAliasRequest updateStateMachineAliasRequest = new UpdateStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withDescription(DESCRIPTION)
                .withRoutingConfiguration(newRoutingConfigSdk);

        final UpdateStateMachineAliasResult updateStateMachineAliasResult = new UpdateStateMachineAliasResult()
                .withUpdateDate(UPDATE_DATE);

        when(proxy.injectCredentialsAndInvoke(eq(updateStateMachineAliasRequest), any(Function.class))).thenReturn(updateStateMachineAliasResult);

        final ProgressEvent<ResourceModel, CallbackContext> actual = handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(actual.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(actual.getResourceModels()).isNull();
        assertThat(actual.getMessage()).isNull();
        assertThat(actual.getErrorCode()).isNull();
    }

    @Test
    public void testHandleAllAtOnceDeployment_whenPreviousResourceStateHasSingleRoutingConfiguration_thenReturnsSuccess() {
        final Set<RoutingConfigurationVersion> previousRoutingConfig = new HashSet<>(Collections.singletonList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 100)
        ));
        final DeploymentPreference desiredDeploymentPreference = getAllAtOnceDeploymentPreference(STATE_MACHINE_VERSION_2_ARN);
        final Set<RoutingConfigurationVersion> newRoutingConfigCfn = getVersionRoutingConfigCfn(STATE_MACHINE_VERSION_2_ARN);
        final List<RoutingConfigurationListItem> newRoutingConfigSdk = getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_2_ARN);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(desiredDeploymentPreference)
                        .build())
                .previousResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .routingConfiguration(previousRoutingConfig)
                        .build())
                .build();

        final ResourceModel expectedResourceModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(newRoutingConfigCfn)
                .deploymentPreference(desiredDeploymentPreference)
                .build();

        final UpdateStateMachineAliasRequest updateStateMachineAliasRequest = new UpdateStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withDescription(DESCRIPTION)
                .withRoutingConfiguration(newRoutingConfigSdk);

        final UpdateStateMachineAliasResult updateStateMachineAliasResult = new UpdateStateMachineAliasResult()
                .withUpdateDate(UPDATE_DATE);

        when(proxy.injectCredentialsAndInvoke(eq(updateStateMachineAliasRequest), any(Function.class))).thenReturn(updateStateMachineAliasResult);

        final ProgressEvent<ResourceModel, CallbackContext> actual = handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(actual.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(actual.getResourceModels()).isNull();
        assertThat(actual.getMessage()).isNull();
        assertThat(actual.getErrorCode()).isNull();
    }

    @Test
    public void testHandleAllAtOnceDeployment_whenPreviousResourceStateHasDoubleRoutingConfiguration_thenReturnsSuccess() {
        final Set<RoutingConfigurationVersion> previousRoutingConfig = new HashSet<>(Arrays.asList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 50),
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 50)
        ));
        final DeploymentPreference desiredDeploymentPreference = getAllAtOnceDeploymentPreference(STATE_MACHINE_VERSION_2_ARN);
        final Set<RoutingConfigurationVersion> newRoutingConfigCfn = getVersionRoutingConfigCfn(STATE_MACHINE_VERSION_2_ARN);
        final List<RoutingConfigurationListItem> newRoutingConfigSdk = getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_2_ARN);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(desiredDeploymentPreference)
                        .build())
                .previousResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .routingConfiguration(previousRoutingConfig)
                        .build())
                .build();

        final ResourceModel expectedResourceModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(newRoutingConfigCfn)
                .deploymentPreference(desiredDeploymentPreference)
                .build();

        final UpdateStateMachineAliasRequest updateStateMachineAliasRequest = new UpdateStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withDescription(DESCRIPTION)
                .withRoutingConfiguration(newRoutingConfigSdk);

        final UpdateStateMachineAliasResult updateStateMachineAliasResult = new UpdateStateMachineAliasResult()
                .withUpdateDate(UPDATE_DATE);

        when(proxy.injectCredentialsAndInvoke(eq(updateStateMachineAliasRequest), any(Function.class))).thenReturn(updateStateMachineAliasResult);

        final ProgressEvent<ResourceModel, CallbackContext> actual = handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(actual.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(actual.getResourceModels()).isNull();
        assertThat(actual.getMessage()).isNull();
        assertThat(actual.getErrorCode()).isNull();
    }

    @Test
    public void testHandleLinearDeployment_whenInitialTrafficShift_thenReturnsInProgress() {
        final DeploymentPreference previousDeploymentPreference = getAllAtOnceDeploymentPreference(STATE_MACHINE_VERSION_1_ARN);
        final DeploymentPreference desiredDeploymentPreference = getLinearDeploymentPreference(STATE_MACHINE_VERSION_2_ARN, 1, 10);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(desiredDeploymentPreference)
                        .build())
                .previousResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(previousDeploymentPreference)
                        .build())
                .build();

        final ResourceModel expectedResourceModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(getVersionRoutingConfigCfn(STATE_MACHINE_VERSION_1_ARN, 90, STATE_MACHINE_VERSION_2_ARN, 10))
                .deploymentPreference(desiredDeploymentPreference)
                .build();

        final UpdateStateMachineAliasRequest updateStateMachineAliasRequest = new UpdateStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withDescription(DESCRIPTION)
                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_1_ARN, 90, STATE_MACHINE_VERSION_2_ARN, 10));

        final UpdateStateMachineAliasResult updateStateMachineAliasResult = new UpdateStateMachineAliasResult()
                .withUpdateDate(UPDATE_DATE);

        when(proxy.injectCredentialsAndInvoke(eq(updateStateMachineAliasRequest), any(Function.class))).thenReturn(updateStateMachineAliasResult);

        final ProgressEvent<ResourceModel, CallbackContext> actual = handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(actual.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(actual.getCallbackDelaySeconds()).isEqualTo(Constants.GRADUAL_DEPLOYMENT_HANDLER_DELAY_SECONDS);
        assertThat(actual.getCallbackContext()).isNotNull();
        assertThat(actual.getCallbackContext().getOriginVersionArn()).isEqualTo(STATE_MACHINE_VERSION_1_ARN);
        assertThat(actual.getCallbackContext().getTargetVersionArn()).isEqualTo(STATE_MACHINE_VERSION_2_ARN);
        assertThat(actual.getCallbackContext().getOriginVersionWeight()).isEqualTo(90);
        assertThat(actual.getCallbackContext().getTargetVersionWeight()).isEqualTo(10);
        assertThat(actual.getCallbackContext().getLastShiftedTime()).isGreaterThan(Instant.now().minusSeconds(3));
        assertThat(actual.getResourceModels()).isNull();
        assertThat(actual.getMessage()).isNull();
        assertThat(actual.getErrorCode()).isNull();
    }

    @Test
    public void testHandleLinearDeployment_whenTimeIntervalHasPassed_thenUpdatesAliasAndReturnsInProgress() {
        final DeploymentPreference desiredDeploymentPreference = getLinearDeploymentPreference(STATE_MACHINE_VERSION_2_ARN, 1, 10);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(desiredDeploymentPreference)
                        .build())
                .build();

        final ResourceModel expectedResourceModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(getVersionRoutingConfigCfn(STATE_MACHINE_VERSION_1_ARN, 80, STATE_MACHINE_VERSION_2_ARN, 20))
                .deploymentPreference(desiredDeploymentPreference)
                .build();

        final DescribeStateMachineAliasRequest describeStateMachineAliasRequest = new DescribeStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN);

        final DescribeStateMachineAliasResult describeStateMachineAliasResult = new DescribeStateMachineAliasResult()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_1_ARN, 90, STATE_MACHINE_VERSION_2_ARN, 10));

        final UpdateStateMachineAliasRequest updateStateMachineAliasRequest = new UpdateStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withDescription(DESCRIPTION)
                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_1_ARN, 80, STATE_MACHINE_VERSION_2_ARN, 20));

        final UpdateStateMachineAliasResult updateStateMachineAliasResult = new UpdateStateMachineAliasResult()
                .withUpdateDate(UPDATE_DATE);

        final CallbackContext callbackContext = CallbackContext.builder()
                .isTrafficShifting(true)
                .originVersionArn(STATE_MACHINE_VERSION_1_ARN)
                .originVersionWeight(90)
                .targetVersionArn(STATE_MACHINE_VERSION_2_ARN)
                .targetVersionWeight(10)
                .lastShiftedTime(Instant.now().minusSeconds(60))
                .build();

        when(proxy.injectCredentialsAndInvoke(eq(describeStateMachineAliasRequest), any(Function.class))).thenReturn(describeStateMachineAliasResult);
        when(proxy.injectCredentialsAndInvoke(eq(updateStateMachineAliasRequest), any(Function.class))).thenReturn(updateStateMachineAliasResult);

        final ProgressEvent<ResourceModel, CallbackContext> actual = handler.handleRequest(proxy, cfnRequest, callbackContext, logger);
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(actual.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(actual.getCallbackDelaySeconds()).isEqualTo(Constants.GRADUAL_DEPLOYMENT_HANDLER_DELAY_SECONDS);
        assertThat(actual.getCallbackContext()).isNotNull();
        assertThat(actual.getCallbackContext().getOriginVersionArn()).isEqualTo(STATE_MACHINE_VERSION_1_ARN);
        assertThat(actual.getCallbackContext().getTargetVersionArn()).isEqualTo(STATE_MACHINE_VERSION_2_ARN);
        assertThat(actual.getCallbackContext().getOriginVersionWeight()).isEqualTo(80);
        assertThat(actual.getCallbackContext().getTargetVersionWeight()).isEqualTo(20);
        assertThat(actual.getCallbackContext().getLastShiftedTime()).isGreaterThan(Instant.now().minusSeconds(3));
        assertThat(actual.getResourceModels()).isNull();
        assertThat(actual.getMessage()).isNull();
        assertThat(actual.getErrorCode()).isNull();
    }

    @Test
    public void testHandleLinearDeployment_whenFinalShift_thenUpdatesAliasAndReturnsSuccess() {
        final DeploymentPreference desiredDeploymentPreference = getLinearDeploymentPreference(STATE_MACHINE_VERSION_2_ARN, 1, 10);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(desiredDeploymentPreference)
                        .build())
                .build();

        final ResourceModel expectedResourceModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(getVersionRoutingConfigCfn(STATE_MACHINE_VERSION_2_ARN))
                .deploymentPreference(desiredDeploymentPreference)
                .build();

        final DescribeStateMachineAliasRequest describeStateMachineAliasRequest = new DescribeStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN);

        final DescribeStateMachineAliasResult describeStateMachineAliasResult = new DescribeStateMachineAliasResult()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_1_ARN, 10, STATE_MACHINE_VERSION_2_ARN, 90));

        final UpdateStateMachineAliasRequest updateStateMachineAliasRequest = new UpdateStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withDescription(DESCRIPTION)

                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_2_ARN));
        final UpdateStateMachineAliasResult updateStateMachineAliasResult = new UpdateStateMachineAliasResult()
                .withUpdateDate(UPDATE_DATE);

        final CallbackContext callbackContext = CallbackContext.builder()
                .isTrafficShifting(true)
                .originVersionArn(STATE_MACHINE_VERSION_1_ARN)
                .originVersionWeight(10)
                .targetVersionArn(STATE_MACHINE_VERSION_2_ARN)
                .targetVersionWeight(90)
                .lastShiftedTime(Instant.now().minusSeconds(60))
                .build();

        when(proxy.injectCredentialsAndInvoke(eq(describeStateMachineAliasRequest), any(Function.class))).thenReturn(describeStateMachineAliasResult);
        when(proxy.injectCredentialsAndInvoke(eq(updateStateMachineAliasRequest), any(Function.class))).thenReturn(updateStateMachineAliasResult);

        final ProgressEvent<ResourceModel, CallbackContext> actual = handler.handleRequest(proxy, cfnRequest, callbackContext, logger);
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(actual.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(actual.getResourceModels()).isNull();
        assertThat(actual.getMessage()).isNull();
        assertThat(actual.getErrorCode()).isNull();
    }

    @Test
    public void testHandleLinearDeployment_whenTimeIntervalHasNotPassed_thenDoesNothingAndReturnsInProgress() {
        final DeploymentPreference desiredDeploymentPreference = getLinearDeploymentPreference(STATE_MACHINE_VERSION_2_ARN, 1, 50);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(desiredDeploymentPreference)
                        .build())
                .build();

        final ResourceModel expectedResourceModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(getVersionRoutingConfigCfn(STATE_MACHINE_VERSION_1_ARN, 90, STATE_MACHINE_VERSION_2_ARN, 10))
                .deploymentPreference(desiredDeploymentPreference)
                .build();

        final DescribeStateMachineAliasRequest describeStateMachineAliasRequest = new DescribeStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN);

        final DescribeStateMachineAliasResult describeStateMachineAliasResult = new DescribeStateMachineAliasResult()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_1_ARN, 90, STATE_MACHINE_VERSION_2_ARN, 10));

        final CallbackContext callbackContext = CallbackContext.builder()
                .isTrafficShifting(true)
                .originVersionArn(STATE_MACHINE_VERSION_1_ARN)
                .originVersionWeight(90)
                .targetVersionArn(STATE_MACHINE_VERSION_2_ARN)
                .targetVersionWeight(10)
                .lastShiftedTime(Instant.now())
                .build();

        when(proxy.injectCredentialsAndInvoke(eq(describeStateMachineAliasRequest), any(Function.class))).thenReturn(describeStateMachineAliasResult);

        final ProgressEvent<ResourceModel, CallbackContext> actual = handler.handleRequest(proxy, cfnRequest, callbackContext, logger);
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(actual.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(actual.getCallbackDelaySeconds()).isEqualTo(Constants.GRADUAL_DEPLOYMENT_HANDLER_DELAY_SECONDS);
        assertThat(actual.getCallbackContext()).isNotNull();
        assertThat(actual.getCallbackContext().getOriginVersionArn()).isEqualTo(STATE_MACHINE_VERSION_1_ARN);
        assertThat(actual.getCallbackContext().getTargetVersionArn()).isEqualTo(STATE_MACHINE_VERSION_2_ARN);
        assertThat(actual.getCallbackContext().getOriginVersionWeight()).isEqualTo(90);
        assertThat(actual.getCallbackContext().getTargetVersionWeight()).isEqualTo(10);
        assertThat(actual.getCallbackContext().getLastShiftedTime()).isEqualTo(callbackContext.getLastShiftedTime());
        assertThat(actual.getResourceModels()).isNull();
        assertThat(actual.getMessage()).isNull();
        assertThat(actual.getErrorCode()).isNull();
    }

    @Test
    public void testHandleLinearDeployment_whenCloudWatchAlarmsAreNotOK_thenAbortsDeploymentAndReturnsFailed() {
        final DeploymentPreference desiredDeploymentPreference = getLinearDeploymentPreference(STATE_MACHINE_VERSION_2_ARN, 1, 50);
        desiredDeploymentPreference.setAlarms(new HashSet<>(Collections.singleton("alarm name")));

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(desiredDeploymentPreference)
                        .build())
                .build();

        final ResourceModel expectedResourceModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(getVersionRoutingConfigCfn(STATE_MACHINE_VERSION_1_ARN))
                .deploymentPreference(desiredDeploymentPreference)
                .build();

        final DescribeStateMachineAliasRequest describeStateMachineAliasRequest = new DescribeStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN);

        final DescribeStateMachineAliasResult describeStateMachineAliasResult = new DescribeStateMachineAliasResult()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_1_ARN, 90, STATE_MACHINE_VERSION_2_ARN, 10));

        final DescribeAlarmsRequest describeAlarmsRequest = new DescribeAlarmsRequest()
                .withAlarmNames(new HashSet<>(Collections.singleton("alarm name")));

        final DescribeAlarmsResult describeAlarmsResult = new DescribeAlarmsResult()
                .withMetricAlarms(new MetricAlarm().withAlarmName("alarm name").withStateValue(StateValue.ALARM));

        final UpdateStateMachineAliasRequest updateStateMachineAliasRequest = new UpdateStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withDescription(DESCRIPTION)
                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_1_ARN));

        final UpdateStateMachineAliasResult updateStateMachineAliasResult = new UpdateStateMachineAliasResult()
                .withUpdateDate(UPDATE_DATE);

        final CallbackContext callbackContext = CallbackContext.builder()
                .isTrafficShifting(true)
                .originVersionArn(STATE_MACHINE_VERSION_1_ARN)
                .originVersionWeight(90)
                .targetVersionArn(STATE_MACHINE_VERSION_2_ARN)
                .targetVersionWeight(10)
                .lastShiftedTime(Instant.now())
                .build();

        when(proxy.injectCredentialsAndInvoke(eq(describeStateMachineAliasRequest), any(Function.class))).thenReturn(describeStateMachineAliasResult);
        when(proxy.injectCredentialsAndInvoke(eq(describeAlarmsRequest), any(Function.class))).thenReturn(describeAlarmsResult);
        when(proxy.injectCredentialsAndInvoke(eq(updateStateMachineAliasRequest), any(Function.class))).thenReturn(updateStateMachineAliasResult);

        final ProgressEvent<ResourceModel, CallbackContext> actual = handler.handleRequest(proxy, cfnRequest, callbackContext, logger);
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(actual.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(actual.getResourceModels()).isNull();
        assertThat(actual.getMessage()).isEqualTo("Aborting deployment. The following CloudWatch alarms are in an 'ALARM' state: [alarm name].");
        assertThat(actual.getErrorCode()).isNull();
    }

    @Test
    public void testHandleCanaryDeployment_whenInitialShift_thenReturnsInProgress() {
        final DeploymentPreference desiredDeploymentPreference = getCanaryDeploymentPreference(STATE_MACHINE_VERSION_2_ARN, 1, 10);
        final DeploymentPreference previousDeploymentPreference = getCanaryDeploymentPreference(STATE_MACHINE_VERSION_1_ARN, 1, 10);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(desiredDeploymentPreference)
                        .build())
                .previousResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(previousDeploymentPreference)
                        .build())
                .build();

        final ResourceModel expectedResourceModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(getVersionRoutingConfigCfn(STATE_MACHINE_VERSION_1_ARN, 90, STATE_MACHINE_VERSION_2_ARN, 10))
                .deploymentPreference(desiredDeploymentPreference)
                .build();

        final UpdateStateMachineAliasRequest updateStateMachineAliasRequest = new UpdateStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withDescription(DESCRIPTION)
                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_2_ARN));

        final UpdateStateMachineAliasResult updateStateMachineAliasResult = new UpdateStateMachineAliasResult()
                .withUpdateDate(UPDATE_DATE);

        when(proxy.injectCredentialsAndInvoke(eq(updateStateMachineAliasRequest), any(Function.class))).thenReturn(updateStateMachineAliasResult);

        final ProgressEvent<ResourceModel, CallbackContext> actual = handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(actual.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(actual.getCallbackDelaySeconds()).isEqualTo(Constants.GRADUAL_DEPLOYMENT_HANDLER_DELAY_SECONDS);
        assertThat(actual.getCallbackContext()).isNotNull();
        assertThat(actual.getCallbackContext().getOriginVersionArn()).isEqualTo(STATE_MACHINE_VERSION_1_ARN);
        assertThat(actual.getCallbackContext().getTargetVersionArn()).isEqualTo(STATE_MACHINE_VERSION_2_ARN);
        assertThat(actual.getCallbackContext().getOriginVersionWeight()).isEqualTo(90);
        assertThat(actual.getCallbackContext().getTargetVersionWeight()).isEqualTo(10);
        assertThat(actual.getCallbackContext().getLastShiftedTime()).isGreaterThan(Instant.now().minusSeconds(3));
        assertThat(actual.getResourceModels()).isNull();
        assertThat(actual.getMessage()).isNull();
        assertThat(actual.getErrorCode()).isNull();
    }

    @Test
    public void testHandleCanaryDeployment_whenFinalShift_thenUpdatesAliasAndReturnsSuccess() {
        final DeploymentPreference desiredDeploymentPreference = getCanaryDeploymentPreference(STATE_MACHINE_VERSION_2_ARN, 1, 10);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(desiredDeploymentPreference)
                        .build())
                .build();

        final ResourceModel expectedResourceModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(getVersionRoutingConfigCfn(STATE_MACHINE_VERSION_2_ARN))
                .deploymentPreference(desiredDeploymentPreference)
                .build();

        final DescribeStateMachineAliasRequest describeStateMachineAliasRequest = new DescribeStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN);

        final DescribeStateMachineAliasResult describeStateMachineAliasResult = new DescribeStateMachineAliasResult()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_1_ARN, 90, STATE_MACHINE_VERSION_2_ARN, 10));

        final UpdateStateMachineAliasRequest updateStateMachineAliasRequest = new UpdateStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withDescription(DESCRIPTION)

                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_2_ARN));
        final UpdateStateMachineAliasResult updateStateMachineAliasResult = new UpdateStateMachineAliasResult()
                .withUpdateDate(UPDATE_DATE);

        final CallbackContext callbackContext = CallbackContext.builder()
                .isTrafficShifting(true)
                .originVersionArn(STATE_MACHINE_VERSION_1_ARN)
                .originVersionWeight(90)
                .targetVersionArn(STATE_MACHINE_VERSION_2_ARN)
                .targetVersionWeight(10)
                .lastShiftedTime(Instant.now().minusSeconds(60))
                .build();

        when(proxy.injectCredentialsAndInvoke(eq(describeStateMachineAliasRequest), any(Function.class))).thenReturn(describeStateMachineAliasResult);
        when(proxy.injectCredentialsAndInvoke(eq(updateStateMachineAliasRequest), any(Function.class))).thenReturn(updateStateMachineAliasResult);

        final ProgressEvent<ResourceModel, CallbackContext> actual = handler.handleRequest(proxy, cfnRequest, callbackContext, logger);
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(actual.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(actual.getResourceModels()).isNull();
        assertThat(actual.getMessage()).isNull();
        assertThat(actual.getErrorCode()).isNull();
    }

    @Test
    public void testHandleCanaryDeployment_whenTimeIntervalHasNotPassed_thenDoesNothingAndReturnsInProgress() {
        final DeploymentPreference desiredDeploymentPreference = getCanaryDeploymentPreference(STATE_MACHINE_VERSION_2_ARN, 1, 50);

        final ResourceModel expectedResourceModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(getVersionRoutingConfigCfn(STATE_MACHINE_VERSION_1_ARN, 90, STATE_MACHINE_VERSION_2_ARN, 10))
                .deploymentPreference(desiredDeploymentPreference)
                .build();

        final DescribeStateMachineAliasRequest describeStateMachineAliasRequest = new DescribeStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN);

        final DescribeStateMachineAliasResult describeStateMachineAliasResult = new DescribeStateMachineAliasResult()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_1_ARN, 90, STATE_MACHINE_VERSION_2_ARN, 10));

        final CallbackContext callbackContext = CallbackContext.builder()
                .isTrafficShifting(true)
                .originVersionArn(STATE_MACHINE_VERSION_1_ARN)
                .originVersionWeight(90)
                .targetVersionArn(STATE_MACHINE_VERSION_2_ARN)
                .targetVersionWeight(10)
                .lastShiftedTime(Instant.now())
                .build();

        when(proxy.injectCredentialsAndInvoke(eq(describeStateMachineAliasRequest), any(Function.class))).thenReturn(describeStateMachineAliasResult);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(desiredDeploymentPreference)
                        .build())
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> actual = handler.handleRequest(proxy, cfnRequest, callbackContext, logger);
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(actual.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(actual.getCallbackDelaySeconds()).isEqualTo(Constants.GRADUAL_DEPLOYMENT_HANDLER_DELAY_SECONDS);
        assertThat(actual.getCallbackContext()).isNotNull();
        assertThat(actual.getCallbackContext().getOriginVersionArn()).isEqualTo(STATE_MACHINE_VERSION_1_ARN);
        assertThat(actual.getCallbackContext().getTargetVersionArn()).isEqualTo(STATE_MACHINE_VERSION_2_ARN);
        assertThat(actual.getCallbackContext().getOriginVersionWeight()).isEqualTo(90);
        assertThat(actual.getCallbackContext().getTargetVersionWeight()).isEqualTo(10);
        assertThat(actual.getCallbackContext().getLastShiftedTime()).isGreaterThan(Instant.now().minusSeconds(3));
        assertThat(actual.getResourceModels()).isNull();
        assertThat(actual.getMessage()).isNull();
        assertThat(actual.getErrorCode()).isNull();
    }

    @Test
    public void testHandleCanaryDeployment_whenCloudWatchAlarmsAreNotOK_thenAbortsDeploymentAndReturnsFailed() {
        final DeploymentPreference desiredDeploymentPreference = getCanaryDeploymentPreference(STATE_MACHINE_VERSION_2_ARN, 1, 50);
        desiredDeploymentPreference.setAlarms(new HashSet<>(Collections.singleton("alarm name")));

        final ResourceModel expectedResourceModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(getVersionRoutingConfigCfn(STATE_MACHINE_VERSION_1_ARN))
                .deploymentPreference(desiredDeploymentPreference)
                .build();

        final DescribeStateMachineAliasRequest describeStateMachineAliasRequest = new DescribeStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN);

        final DescribeStateMachineAliasResult describeStateMachineAliasResult = new DescribeStateMachineAliasResult()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_1_ARN, 90, STATE_MACHINE_VERSION_2_ARN, 10));

        final DescribeAlarmsRequest describeAlarmsRequest = new DescribeAlarmsRequest()
                .withAlarmNames(new HashSet<>(Collections.singleton("alarm name")));

        final DescribeAlarmsResult describeAlarmsResult = new DescribeAlarmsResult()
                .withMetricAlarms(new MetricAlarm().withAlarmName("alarm name").withStateValue(StateValue.ALARM));

        final UpdateStateMachineAliasRequest updateStateMachineAliasRequest = new UpdateStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withDescription(DESCRIPTION)
                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_1_ARN));

        final UpdateStateMachineAliasResult updateStateMachineAliasResult = new UpdateStateMachineAliasResult()
                .withUpdateDate(UPDATE_DATE);

        final CallbackContext callbackContext = CallbackContext.builder()
                .isTrafficShifting(true)
                .originVersionArn(STATE_MACHINE_VERSION_1_ARN)
                .originVersionWeight(90)
                .targetVersionArn(STATE_MACHINE_VERSION_2_ARN)
                .targetVersionWeight(10)
                .lastShiftedTime(Instant.now())
                .build();

        when(proxy.injectCredentialsAndInvoke(eq(describeStateMachineAliasRequest), any(Function.class))).thenReturn(describeStateMachineAliasResult);
        when(proxy.injectCredentialsAndInvoke(eq(describeAlarmsRequest), any(Function.class))).thenReturn(describeAlarmsResult);
        when(proxy.injectCredentialsAndInvoke(eq(updateStateMachineAliasRequest), any(Function.class))).thenReturn(updateStateMachineAliasResult);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(desiredDeploymentPreference)
                        .build())
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> actual = handler.handleRequest(proxy, cfnRequest, callbackContext, logger);
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(actual.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(actual.getResourceModels()).isNull();
        assertThat(actual.getMessage()).isEqualTo("Aborting deployment. The following CloudWatch alarms are in an 'ALARM' state: [alarm name].");
        assertThat(actual.getErrorCode()).isNull();
    }

    @Test
    public void testHandleGradualDeployment_whenAliasHasDoubleVersionRoutingConfig_thenFailsPreflightCheckAndThrowsValidationException() {
        final Set<RoutingConfigurationVersion> previousResourceStateRoutingConfig = new HashSet<>(Arrays.asList(
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_1_ARN, 50),
                new RoutingConfigurationVersion(STATE_MACHINE_VERSION_2_ARN, 50)
        ));
        final DeploymentPreference desiredDeploymentPreference = getLinearDeploymentPreference(STATE_MACHINE_VERSION_2_ARN, 1, 1);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .deploymentPreference(desiredDeploymentPreference)
                        .build())
                .previousResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .routingConfiguration(previousResourceStateRoutingConfig)
                        .build())
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void testHandleGradualDeployment_whenInflightCheckFails_thenThrowsValidationException() {
        final DeploymentPreference desiredDeploymentPreference = getLinearDeploymentPreference(STATE_MACHINE_VERSION_2_ARN, 1, 10);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .deploymentPreference(desiredDeploymentPreference)
                        .build())
                .build();

        final DescribeStateMachineAliasRequest describeStateMachineAliasRequest = new DescribeStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN);

        final DescribeStateMachineAliasResult describeStateMachineAliasResult = new DescribeStateMachineAliasResult()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_1_ARN));

        final CallbackContext callbackContext = CallbackContext.builder()
                .isTrafficShifting(true)
                .originVersionArn(STATE_MACHINE_VERSION_1_ARN)
                .targetVersionArn(STATE_MACHINE_VERSION_2_ARN)
                .lastShiftedTime(Instant.now())
                .build();

        when(proxy.injectCredentialsAndInvoke(eq(describeStateMachineAliasRequest), any(Function.class))).thenReturn(describeStateMachineAliasResult);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, cfnRequest, callbackContext, logger);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void testHandleGradualDeployment_whenRollbackIsTrue_thenPerformsAllAtOnceDeployment() {
        final DeploymentPreference deploymentPreference = getLinearDeploymentPreference(STATE_MACHINE_VERSION_1_ARN, 1, 10);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(deploymentPreference)
                        .build())
                .rollback(true)
                .build();

        final ResourceModel expectedResourceModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(getVersionRoutingConfigCfn(STATE_MACHINE_VERSION_1_ARN))
                .deploymentPreference(deploymentPreference)
                .build();

        final UpdateStateMachineAliasRequest updateStateMachineAliasRequest = new UpdateStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withDescription(DESCRIPTION)
                .withRoutingConfiguration(getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_1_ARN));

        final UpdateStateMachineAliasResult updateStateMachineAliasResult = new UpdateStateMachineAliasResult()
                .withUpdateDate(UPDATE_DATE);

        when(proxy.injectCredentialsAndInvoke(eq(updateStateMachineAliasRequest), any(Function.class))).thenReturn(updateStateMachineAliasResult);

        final ProgressEvent<ResourceModel, CallbackContext> actual = handler.handleRequest(proxy, cfnRequest, null, logger);
        assertThat(actual).isNotNull();
        assertThat(actual.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(actual.getResourceModel()).isEqualTo(expectedResourceModel);
        assertThat(actual.getResourceModels()).isNull();
        assertThat(actual.getMessage()).isNull();
        assertThat(actual.getErrorCode()).isNull();
    }
}
