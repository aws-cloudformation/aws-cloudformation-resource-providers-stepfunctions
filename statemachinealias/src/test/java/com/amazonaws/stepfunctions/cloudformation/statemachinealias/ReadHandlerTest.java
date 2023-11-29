package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineAliasRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineAliasResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static com.amazonaws.stepfunctions.cloudformation.statemachinealias.Constants.ACCESS_DENIED_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachinealias.Constants.INVALID_ARN_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachinealias.Constants.RESOURCE_NOT_FOUND_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachinealias.Constants.THROTTLING_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachinealias.Constants.VALIDATION_ERROR_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends HandlerTestBase {

    @BeforeEach
    public void setup() {
        handler = new ReadHandler();

        cfnRequest = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ALIAS_ARN)
                        .build())
                .build();

        awsRequest = new DescribeStateMachineAliasRequest()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN);
    }

    @Test
    public void testSuccess() {
        final DescribeStateMachineAliasResult describeStateMachineAliasResult = new DescribeStateMachineAliasResult()
                .withStateMachineAliasArn(STATE_MACHINE_ALIAS_ARN)
                .withCreationDate(CREATION_DATE)
                .withDescription(DESCRIPTION)
                .withName(ALIAS_NAME)
                .withRoutingConfiguration(SDK_ROUTING_CONFIGURATION);

        when(proxy.injectCredentialsAndInvoke(eq(awsRequest), any(Function.class))).thenReturn(describeStateMachineAliasResult);

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
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void testStateMachineAliasArnNotPresentInModel() {
        cfnRequest.setDesiredResourceState(ResourceModel.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, cfnRequest, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getMessage()).contains(RESOURCE_NOT_FOUND_ERROR_CODE);
    }

    @Test
    public void testStateMachineInvalidRequestErrors() {
        final List<String> invalidRequestErrorCodes = Arrays.asList(
                INVALID_ARN_ERROR_CODE, VALIDATION_ERROR_CODE
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
}
