package com.amazonaws.stepfunctions.cloudformation.statemachineversion;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.DeleteStateMachineVersionRequest;
import com.amazonaws.services.stepfunctions.model.DeleteStateMachineVersionResult;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import static com.amazonaws.stepfunctions.cloudformation.statemachineversion.Constants.ACCESS_DENIED_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachineversion.Constants.STATE_MACHINE_DOES_NOT_EXIST_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachineversion.Constants.THROTTLING_ERROR_CODE;


@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends HandlerTestBase {

    private AWSStepFunctions sfnClient;
    private DeleteHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;
    private DescribeStateMachineRequest describeStateMachineVersionRequest;
    private DeleteStateMachineVersionRequest deleteStateMachineVersionRequest;

    @BeforeEach
    public void setup() {
        sfnClient = ClientBuilder.getClient();
        handler = new DeleteHandler();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .stateMachineArn(STATE_MACHINE_ARN)
                        .arn(STATE_MACHINE_VERSION_ARN)
                        .stateMachineRevisionId(STATE_MACHINE_REVISION_ID)
                        .description(DESCRIPTION)
                        .build())
                .build();

        describeStateMachineVersionRequest = new DescribeStateMachineRequest();
        describeStateMachineVersionRequest.setStateMachineArn(STATE_MACHINE_VERSION_ARN);

        deleteStateMachineVersionRequest = new DeleteStateMachineVersionRequest();
        deleteStateMachineVersionRequest.setStateMachineVersionArn(STATE_MACHINE_VERSION_ARN);
    }

    @Test
    public void testSuccess() {
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()).isNull();
    }

    @Test
    public void testStateMachineVersionArnNotPresent_returnsNotFoundError() {
        request.setDesiredResourceState(ResourceModel.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getMessage()).contains(STATE_MACHINE_DOES_NOT_EXIST_ERROR_CODE);
    }

    @Test
    public void testStateMachineVersionNotFound_whenDescribing_returnsNotFoundError() {
        final AmazonServiceException exceptionThrottling = createAndMockAmazonServiceExceptionForDescribe(STATE_MACHINE_DOES_NOT_EXIST_ERROR_CODE);
        assertFailure(exceptionThrottling.getMessage(), HandlerErrorCode.NotFound);
    }

    @Test
    public void testFailureAccessDeniedError_whenDescribing_returnsAccessDeniedError() {
        final AmazonServiceException exceptionAccessDenied = createAndMockAmazonServiceExceptionForDescribe(ACCESS_DENIED_ERROR_CODE);
        assertFailure(exceptionAccessDenied.getMessage(), HandlerErrorCode.AccessDenied);
    }

    @Test
    public void testFailureThrottlingError_whenDescribing_returnsThrottlingError() {
        final AmazonServiceException exceptionThrottling = createAndMockAmazonServiceExceptionForDescribe(THROTTLING_ERROR_CODE);
        assertFailure(exceptionThrottling.getMessage(), HandlerErrorCode.Throttling);
    }

    @Test
    public void test500_whenDescribing_returnsServiceInternalError() {
        when(proxy.injectCredentialsAndInvoke(eq(describeStateMachineVersionRequest), any(Function.class))).thenThrow(exception500);

        assertFailure(exception500.getMessage(), HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void testFailureAccessDeniedError_whenDeleting_returnsAccessDeniedError() {
        final AmazonServiceException exceptionAccessDenied = createAndMockAmazonServiceExceptionForDelete(ACCESS_DENIED_ERROR_CODE);
        assertFailure(exceptionAccessDenied.getMessage(), HandlerErrorCode.AccessDenied);
    }

    @Test
    public void testFailureThrottlingError_whenDeleting_returnsThrottlingError() {
        final AmazonServiceException exceptionThrottling = createAndMockAmazonServiceExceptionForDelete(THROTTLING_ERROR_CODE);
        assertFailure(exceptionThrottling.getMessage(), HandlerErrorCode.Throttling);
    }

    @Test
    public void test500_whenDeleting_returnsServiceInternalError() {
        when(proxy.injectCredentialsAndInvoke(any(), any(Function.class))).thenReturn(null).thenThrow(exception500);

        assertFailure(exception500.getMessage(), HandlerErrorCode.ServiceInternalError);
    }

    private AmazonServiceException createAndMockAmazonServiceExceptionForDescribe(final String errorCode) {
        final AmazonServiceException amazonServiceException = new AmazonServiceException(errorCode);
        amazonServiceException.setStatusCode(400);
        amazonServiceException.setErrorCode(errorCode);

        when(proxy.injectCredentialsAndInvoke(eq(describeStateMachineVersionRequest), any(Function.class))).thenThrow(amazonServiceException);

        return amazonServiceException;
    }

    private AmazonServiceException createAndMockAmazonServiceExceptionForDelete(final String errorCode) {
        final AmazonServiceException amazonServiceException = new AmazonServiceException(errorCode);
        amazonServiceException.setStatusCode(400);
        amazonServiceException.setErrorCode(errorCode);

        when(proxy.injectCredentialsAndInvoke(any(), any(Function.class))).thenReturn(null).thenThrow(amazonServiceException);

        return amazonServiceException;
    }

    private void assertFailure(final String message, final HandlerErrorCode code) {
        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(message);
        assertThat(response.getErrorCode()).isEqualTo(code);
    }
}
