package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineAliasRequest;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineAliasResult;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineAliasRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineAliasResult;
import com.amazonaws.services.stepfunctions.model.RoutingConfigurationListItem;
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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.amazonaws.stepfunctions.cloudformation.statemachinealias.Constants.ACCESS_DENIED_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachinealias.Constants.INVALID_ARN_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachinealias.Constants.INVALID_NAME_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachinealias.Constants.RESOURCE_NOT_FOUND_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachinealias.Constants.THROTTLING_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachinealias.Constants.VALIDATION_ERROR_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CreateHandlerTest extends HandlerTestBase {
    final DescribeStateMachineAliasRequest describeStateMachineAliasRequest = new DescribeStateMachineAliasRequest();

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .routingConfiguration(CFN_ROUTING_CONFIGURATION)
                        .build())
                .build();

        describeStateMachineAliasRequest.setStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN);
        when(proxy.injectCredentialsAndInvoke(eq(describeStateMachineAliasRequest), any(Function.class))).thenThrow(resourceNotFoundException);

        awsRequest = new CreateStateMachineAliasRequest()
                .withName(ALIAS_NAME)
                .withDescription(DESCRIPTION)
                .withRoutingConfiguration(SDK_ROUTING_CONFIGURATION);
    }

    @Test
    public void testSuccess_withRoutingConfig() {
        final CreateStateMachineAliasResult createStateMachineAliasResult = new CreateStateMachineAliasResult()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withCreationDate(CREATION_DATE);

        when(proxy.injectCredentialsAndInvoke(eq(awsRequest), any(Function.class))).thenReturn(createStateMachineAliasResult);

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
    public void testSuccess_withDeploymentPreference() {
        final DeploymentPreference deploymentPreference = getAllAtOnceDeploymentPreference(STATE_MACHINE_VERSION_1_ARN);
        final List<RoutingConfigurationListItem> routingConfigSdk = getVersionRoutingConfigSdk(STATE_MACHINE_VERSION_1_ARN);
        final Set<RoutingConfigurationVersion> routingConfigCfn = getVersionRoutingConfigCfn(STATE_MACHINE_VERSION_1_ARN);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(deploymentPreference)
                        .build())
                .build();

        awsRequest = new CreateStateMachineAliasRequest()
                .withName(ALIAS_NAME)
                .withDescription(DESCRIPTION)
                .withRoutingConfiguration(routingConfigSdk);

        final CreateStateMachineAliasResult createStateMachineAliasResult = new CreateStateMachineAliasResult()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withCreationDate(CREATION_DATE);

        when(proxy.injectCredentialsAndInvoke(eq(awsRequest), any(Function.class))).thenReturn(createStateMachineAliasResult);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, cfnRequest, null, logger);

        final ResourceModel expectedModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ALIAS_ARN)
                .name(ALIAS_NAME)
                .description(DESCRIPTION)
                .routingConfiguration(routingConfigCfn)
                .deploymentPreference(deploymentPreference)
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
    public void testSuccess_whenDeploymentPreferenceValidationFails_thenThrowsValidationException() {
        final DeploymentPreference deploymentPreference = new DeploymentPreference();
        deploymentPreference.setType(DEPLOYMENT_PREFERENCE_TYPE_LINEAR);

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .name(ALIAS_NAME)
                        .description(DESCRIPTION)
                        .deploymentPreference(deploymentPreference)
                        .build())
                .build();

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, cfnRequest, context, logger);
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InvalidRequest);
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
                INVALID_ARN_ERROR_CODE, INVALID_NAME_ERROR_CODE, VALIDATION_ERROR_CODE
        );

        for (String errorCode: invalidRequestErrorCodes) {
            final AmazonServiceException invalidRequestException = createAndMockAmazonServiceException(errorCode);
            assertFailure(invalidRequestException.getMessage(), HandlerErrorCode.InvalidRequest);
        }
    }

    @Test
    public void testResourceNotFoundError() {
        final AmazonServiceException exceptionThrottling = createAndMockAmazonServiceException(RESOURCE_NOT_FOUND_ERROR_CODE);
        assertFailure(exceptionThrottling.getMessage(), HandlerErrorCode.NotFound);
    }

    @Test
    public void testAccessDeniedError() {
        final AmazonServiceException exceptionAccessDenied = createAndMockAmazonServiceException(ACCESS_DENIED_ERROR_CODE);
        assertFailure(exceptionAccessDenied.getMessage(), HandlerErrorCode.AccessDenied);
    }

    @Test
    public void testThrottlingError() {
        final AmazonServiceException exceptionThrottling = createAndMockAmazonServiceException(THROTTLING_ERROR_CODE);
        assertFailure(exceptionThrottling.getMessage(), HandlerErrorCode.Throttling);
    }

    @Test
    public void testServiceInternalError() {
        when(proxy.injectCredentialsAndInvoke(eq(awsRequest), any(Function.class))).thenThrow(exception500);
        assertFailure(exception500.getMessage(), HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void testStateMachineAliasAlreadyExistsError() {
        when(proxy.injectCredentialsAndInvoke(eq(describeStateMachineAliasRequest), any(Function.class))).thenReturn(new DescribeStateMachineAliasResult());
        assertFailure(stateMachineAliasAlreadyExistsException.getMessage(), HandlerErrorCode.AlreadyExists);
    }
}
