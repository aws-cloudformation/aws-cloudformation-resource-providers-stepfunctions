package com.amazonaws.stepfunctions.cloudformation.statemachineversion;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineResult;
import com.amazonaws.services.stepfunctions.model.ListStateMachineVersionsRequest;
import com.amazonaws.services.stepfunctions.model.ListStateMachineVersionsResult;
import com.amazonaws.services.stepfunctions.model.PublishStateMachineVersionRequest;
import com.amazonaws.services.stepfunctions.model.PublishStateMachineVersionResult;
import com.amazonaws.services.stepfunctions.model.StateMachineVersionListItem;
import static com.amazonaws.stepfunctions.cloudformation.statemachineversion.Constants.STATE_MACHINE_ALREADY_EXISTS;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.cloudformation.exceptions.TerminalException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collections;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import static com.amazonaws.stepfunctions.cloudformation.statemachineversion.Constants.ACCESS_DENIED_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachineversion.Constants.STATE_MACHINE_DOES_NOT_EXIST_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachineversion.Constants.THROTTLING_ERROR_CODE;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CreateHandlerTest extends HandlerTestBase {

    private CreateHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;
    private DescribeStateMachineRequest describeStateMachineRequest;
    private DescribeStateMachineResult describeStateMachineResult;
    private DescribeStateMachineRequest describeStateMachineVersionRequest;
    private DescribeStateMachineResult describeStateMachineVersionResult;
    private ListStateMachineVersionsRequest listLatestStateMachineVersionsRequest;
    private ListStateMachineVersionsResult listLatestStateMachineVersionsResult;
    private PublishStateMachineVersionRequest publishStateMachineVersionRequest;
    private PublishStateMachineVersionResult publishStateMachineVersionResult;

    @BeforeEach
    public void setup() {
        handler = new CreateHandler();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .stateMachineArn(STATE_MACHINE_ARN)
                        .stateMachineRevisionId(STATE_MACHINE_REVISION_ID)
                        .description(DESCRIPTION)
                        .build())
                .build();

        // Describe state machine request and response
        describeStateMachineRequest = new DescribeStateMachineRequest();
        describeStateMachineRequest.setStateMachineArn(STATE_MACHINE_ARN);

        describeStateMachineResult = new DescribeStateMachineResult();
        describeStateMachineResult.setRevisionId(STATE_MACHINE_REVISION_ID);

        // Describe state machine version request and response
        describeStateMachineVersionRequest = new DescribeStateMachineRequest();
        describeStateMachineVersionRequest.setStateMachineArn(STATE_MACHINE_VERSION_ARN);

        describeStateMachineVersionResult = new DescribeStateMachineResult();
        describeStateMachineVersionResult.setRevisionId(STATE_MACHINE_REVISION_ID);

        // List latest state machine version request and response
        listLatestStateMachineVersionsRequest = new ListStateMachineVersionsRequest();
        listLatestStateMachineVersionsRequest.setStateMachineArn(STATE_MACHINE_ARN);
        listLatestStateMachineVersionsRequest.setMaxResults(1);

        listLatestStateMachineVersionsResult = new ListStateMachineVersionsResult();
        listLatestStateMachineVersionsResult.setStateMachineVersions(Collections.emptyList());

        // Publish state machine version request and response
        publishStateMachineVersionRequest = new PublishStateMachineVersionRequest();
        publishStateMachineVersionRequest.setStateMachineArn(STATE_MACHINE_ARN);
        publishStateMachineVersionRequest.setRevisionId(STATE_MACHINE_REVISION_ID);
        publishStateMachineVersionRequest.setDescription(DESCRIPTION);

        publishStateMachineVersionResult = new PublishStateMachineVersionResult();
        publishStateMachineVersionResult.setStateMachineVersionArn(STATE_MACHINE_VERSION_ARN);

        // Needed by all tests for the state machine version existence check
        when(proxy.injectCredentialsAndInvoke(eq(listLatestStateMachineVersionsRequest), any(Function.class))).thenReturn(listLatestStateMachineVersionsResult);
    }

    @Test
    public void testSuccess() {
        when(proxy.injectCredentialsAndInvoke(eq(publishStateMachineVersionRequest), any(Function.class))).thenReturn(publishStateMachineVersionResult);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getArn()).isEqualTo(STATE_MACHINE_VERSION_ARN);
    }

    @Test
    public void test500() {
        when(proxy.injectCredentialsAndInvoke(eq(publishStateMachineVersionRequest), any(Function.class))).thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.ServiceInternalError);
    }

    @Test
    public void testThrottlingError() {
        final AmazonServiceException throttlingException = new AmazonServiceException(THROTTLING_ERROR_CODE);
        throttlingException.setStatusCode(400);
        throttlingException.setErrorCode(THROTTLING_ERROR_CODE);

        when(proxy.injectCredentialsAndInvoke(eq(publishStateMachineVersionRequest), any(Function.class))).thenThrow(throttlingException);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(throttlingException.getMessage());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.Throttling);
    }

    @Test
    public void testAccessDeniedError() {
        final AmazonServiceException accessDeniedException = new AmazonServiceException(ACCESS_DENIED_ERROR_CODE);
        accessDeniedException.setStatusCode(400);
        accessDeniedException.setErrorCode(ACCESS_DENIED_ERROR_CODE);


        when(proxy.injectCredentialsAndInvoke(eq(publishStateMachineVersionRequest), any(Function.class))).thenThrow(accessDeniedException);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(accessDeniedException.getMessage());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AccessDenied);
    }

    @Test
    public void testStateMachineDoesNotExistError() {
        final AmazonServiceException stateMachineDoesNotExistException = new AmazonServiceException(STATE_MACHINE_DOES_NOT_EXIST_ERROR_CODE);
        stateMachineDoesNotExistException.setStatusCode(400);
        stateMachineDoesNotExistException.setErrorCode(STATE_MACHINE_DOES_NOT_EXIST_ERROR_CODE);

        when(proxy.injectCredentialsAndInvoke(eq(publishStateMachineVersionRequest), any(Function.class))).thenThrow(stateMachineDoesNotExistException);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(stateMachineDoesNotExistException.getMessage());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
    }

    @Test
    public void testTerminalError() {
        final TerminalException terminalException = new TerminalException("Terminal Exception");

        when(proxy.injectCredentialsAndInvoke(eq(publishStateMachineVersionRequest), any(Function.class))).thenThrow(terminalException);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(terminalException.getMessage());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.InternalFailure);
    }

    @Test
    public void testStateMachineVersionAlreadyExistsError() {
        listLatestStateMachineVersionsResult.setStateMachineVersions(Collections.singleton(new StateMachineVersionListItem().withStateMachineVersionArn(STATE_MACHINE_VERSION_ARN)));
        when(proxy.injectCredentialsAndInvoke(eq(listLatestStateMachineVersionsRequest), any(Function.class))).thenReturn(listLatestStateMachineVersionsResult);
        when(proxy.injectCredentialsAndInvoke(eq(describeStateMachineRequest), any(Function.class))).thenReturn(describeStateMachineResult);
        when(proxy.injectCredentialsAndInvoke(eq(describeStateMachineVersionRequest), any(Function.class))).thenReturn(describeStateMachineVersionResult);

        final AmazonServiceException stateMachineAlreadyExistsException = new AmazonServiceException(STATE_MACHINE_ALREADY_EXISTS);
        stateMachineAlreadyExistsException.setStatusCode(400);
        stateMachineAlreadyExistsException.setErrorCode(STATE_MACHINE_ALREADY_EXISTS);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(stateMachineAlreadyExistsException.getMessage());
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.AlreadyExists);
    }
}
