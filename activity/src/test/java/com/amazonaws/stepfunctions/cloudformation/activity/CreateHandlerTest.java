package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.stepfunctions.model.CreateActivityRequest;
import com.amazonaws.services.stepfunctions.model.CreateActivityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.exceptions.TerminalException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.function.Function;

import static com.amazonaws.stepfunctions.cloudformation.activity.Constants.ACCESS_DENIED_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.activity.Constants.ACTIVITY_DOES_NOT_EXIST_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.activity.Constants.THROTTLING_ERROR_CODE;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends HandlerTestBase {

    private CreateHandler handler = new CreateHandler();

    private ResourceHandlerRequest<ResourceModel> request;

    @BeforeEach
    public void setup() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder().name(ACTIVITY_NAME).build())
                .build();
    }

    @Test
    public void testSuccess() {
        CreateActivityRequest createActivityRequest = new CreateActivityRequest();
        createActivityRequest.setName(ACTIVITY_NAME);
        createActivityRequest.setTags(new ArrayList<>());

        CreateActivityResult createActivityResult = new CreateActivityResult();
        createActivityResult.setActivityArn(ACTIVITY_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.eq(createActivityRequest), Mockito.any(Function.class))).thenReturn(createActivityResult);

        final ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getArn()).isEqualTo(ACTIVITY_ARN);
        assertThat(response.getResourceModel().getName()).isEqualTo(ACTIVITY_NAME);
    }

    @Test
    public void test500() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateActivityRequest.class), Mockito.any(Function.class))).thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exception500.getMessage());
    }

    @Test
    public void testThrottlingError() {
        AmazonServiceException exceptionThrottling = new AmazonServiceException(THROTTLING_ERROR_CODE);
        exceptionThrottling.setStatusCode(400);
        exceptionThrottling.setErrorCode(THROTTLING_ERROR_CODE);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateActivityRequest.class), Mockito.any(Function.class))).thenThrow(exceptionThrottling);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exceptionThrottling.getMessage());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }

    @Test
    public void testAccessDeniedError() {
        AmazonServiceException exceptionAccessDenied = new AmazonServiceException(ACCESS_DENIED_ERROR_CODE);
        exceptionAccessDenied.setStatusCode(400);
        exceptionAccessDenied.setErrorCode(ACCESS_DENIED_ERROR_CODE);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateActivityRequest.class), Mockito.any(Function.class))).thenThrow(exceptionAccessDenied);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exceptionAccessDenied.getMessage());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }

    @Test
    public void testActivityDoesNotExistError() {
        AmazonServiceException exceptionActivityDoesNotExist = new AmazonServiceException(ACTIVITY_DOES_NOT_EXIST_ERROR_CODE);
        exceptionActivityDoesNotExist.setStatusCode(400);
        exceptionActivityDoesNotExist.setErrorCode(ACTIVITY_DOES_NOT_EXIST_ERROR_CODE);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateActivityRequest.class), Mockito.any(Function.class))).thenThrow(exceptionActivityDoesNotExist);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exceptionActivityDoesNotExist.getMessage());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void testTerminalException() {
        TerminalException terminalException = new TerminalException("Terminal Exception");

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateActivityRequest.class), Mockito.any(Function.class))).thenThrow(terminalException);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(terminalException.getMessage());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }

}
