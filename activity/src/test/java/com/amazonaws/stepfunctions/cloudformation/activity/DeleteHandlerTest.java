package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.stepfunctions.model.DescribeActivityRequest;
import com.amazonaws.services.stepfunctions.model.DescribeActivityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends HandlerTestBase {

    private DeleteHandler handler = new DeleteHandler();

    private ResourceHandlerRequest<ResourceModel> request;

    @BeforeEach
    public void setup() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(ACTIVITY_ARN)
                        .name(ACTIVITY_NAME)
                        .build()
                )
                .build();
    }

    @Test
    public void testDeleteHandler_pausesFor60sToStabilize_returnsSuccess() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(DescribeActivityRequest.class), Mockito.any(Function.class)))
                .thenReturn(new DescribeActivityResult());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(60);

        final ProgressEvent<ResourceModel, CallbackContext> responseFromCallback
                = handler.handleRequest(proxy, request, response.getCallbackContext(), logger);

        assertThat(responseFromCallback).isNotNull();
        assertThat(responseFromCallback.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(responseFromCallback.getCallbackContext()).isNull();
        assertThat(responseFromCallback.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(responseFromCallback.getResourceModel()).isNull();
        assertThat(responseFromCallback.getResourceModels()).isNull();
        assertThat(responseFromCallback.getMessage()).isNull();
        assertThat(responseFromCallback.getErrorCode()).isNull();
    }

    @Test
    public void test400() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(new DescribeActivityResult())
                .thenThrow(exception400);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    public void test500() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(new DescribeActivityResult())
                .thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exception500.getMessage());
    }

    @Test
    public void testUnknownException() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(new DescribeActivityResult())
                .thenThrow(unknownException);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(Constants.INTERNAL_FAILURE_MESSAGE);
    }

    @Test
    public void testResourceArnIsNull_returnsNotFound() {
        request.setDesiredResourceState(ResourceModel.builder()
                .name(ACTIVITY_NAME)
                .build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getMessage()).contains(Constants.ACTIVITY_ARN_NOT_FOUND_MESSAGE);
    }

    @Test
    public void testActivityDoesNotExist_returnsNotFound() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(DescribeActivityRequest.class), Mockito.any(Function.class))).thenThrow(activityDoesNotExistException);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

}
