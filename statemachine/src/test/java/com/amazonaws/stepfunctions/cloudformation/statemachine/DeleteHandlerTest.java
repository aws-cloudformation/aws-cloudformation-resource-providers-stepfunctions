package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.model.DescribeStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineResult;
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

    private CallbackContext callbackContext;

    private ResourceHandlerRequest<ResourceModel> request;

    @BeforeEach
    public void setup() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ARN)
                        .build()
                )
                .build();

        callbackContext = CallbackContext.builder().build();
    }

    @Test
    public void testSuccess_initialCall_returnsInProgress_withDeletionStarted() {
        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(
                proxy,
                request,
                null,
                logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackContext().isDeletionStarted()).isTrue();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testSuccess_deletionStarted_andStillDeleting_returnsInProgress() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(DescribeStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(new DescribeStateMachineResult());

        callbackContext.setDeletionStarted(true);

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(
                proxy,
                request,
                callbackContext,
                logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        assertThat(response.getCallbackContext()).isNotNull();
        assertThat(response.getCallbackContext().isDeletionStarted()).isTrue();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNotNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void testSuccess_deletionStarted_andCompletes_returnsSuccess() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(DescribeStateMachineRequest.class), Mockito.any(Function.class))).thenThrow(stateMachineDoesNotExistException);

        callbackContext.setDeletionStarted(true);

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(
                proxy,
                request,
                callbackContext,
                logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void test400() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class))).thenThrow(exception400);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    public void test500() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class))).thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exception500.getMessage());
    }

    @Test
    public void testUnknownException() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class))).thenThrow(unknownException);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(Constants.INTERNAL_FAILURE_MESSAGE);
    }

    @Test
    public void testThrowsStateMachineDoesNotExist_whenArnMissing() {
        request.setDesiredResourceState(ResourceModel.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getMessage()).contains(Constants.STATE_MACHINE_DOES_NOT_EXIST_ERROR_MESSAGE);
    }

    @Test
    public void testThrowsStateMachineDoesNotExist_whenStateMachineDoesNotExist() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(DescribeStateMachineRequest.class), Mockito.any(Function.class))).thenThrow(stateMachineDoesNotExistException);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getMessage()).contains(Constants.STATE_MACHINE_DOES_NOT_EXIST_ERROR_MESSAGE);
    }

    @Test
    public void testThrowsException_whenDescribeStateMachineThrowsNotStateMachineDoesNotExistException() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(DescribeStateMachineRequest.class), Mockito.any(Function.class))).thenThrow(unknownException);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(Constants.INTERNAL_FAILURE_MESSAGE);
    }
}
