package com.amazonaws.stepfunctions.cloudformation.statemachineversion;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.stepfunctions.model.ListStateMachineVersionsRequest;
import com.amazonaws.services.stepfunctions.model.ListStateMachineVersionsResult;
import com.amazonaws.services.stepfunctions.model.StateMachineVersionListItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.HandlerErrorCode;

import java.util.Collections;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import static com.amazonaws.stepfunctions.cloudformation.statemachineversion.Constants.ACCESS_DENIED_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachineversion.Constants.STATE_MACHINE_DOES_NOT_EXIST_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachineversion.Constants.THROTTLING_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.statemachineversion.Constants.INVALID_TOKEN;


@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends HandlerTestBase {

    private ListHandler handler;
    private ResourceHandlerRequest<ResourceModel> request;
    private StateMachineVersionListItem stateMachineVersionListItem;
    private ListStateMachineVersionsRequest listStateMachineVersionsRequest;
    private ListStateMachineVersionsResult listStateMachineVersionsResult;
    private final String NEXT_TOKEN = "token";

    @BeforeEach
    public void setup() {
        handler = new ListHandler();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .build();

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

        stateMachineVersionListItem = new StateMachineVersionListItem();
        stateMachineVersionListItem.setStateMachineVersionArn(STATE_MACHINE_VERSION_ARN);

        listStateMachineVersionsRequest = new ListStateMachineVersionsRequest();
        listStateMachineVersionsRequest.setStateMachineArn(STATE_MACHINE_ARN);

        listStateMachineVersionsResult = new ListStateMachineVersionsResult();
        listStateMachineVersionsResult.setStateMachineVersions(Collections.singleton(stateMachineVersionListItem));
    }

    @Test
    public void testSuccess() {
        final ResourceModel expectedModel = ResourceModel.builder()
                .arn(STATE_MACHINE_VERSION_ARN)
                .build();

        when(proxy.injectCredentialsAndInvoke(eq(listStateMachineVersionsRequest), any(Function.class))).thenReturn(listStateMachineVersionsResult);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModels()).containsExactly(expectedModel);
    }

    @Test
    public void testSuccess_returnsNextToken() {
        listStateMachineVersionsResult.setNextToken(NEXT_TOKEN);

        final ResourceModel expectedModel = ResourceModel.builder()
                .arn(STATE_MACHINE_VERSION_ARN)
                .build();

        when(proxy.injectCredentialsAndInvoke(eq(listStateMachineVersionsRequest), any(Function.class))).thenReturn(listStateMachineVersionsResult);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModels()).containsExactly(expectedModel);
    }

    @Test
    public void testSuccess_returnsEmptyList() {
        listStateMachineVersionsResult.setNextToken(NEXT_TOKEN);

        ListStateMachineVersionsResult emptyListStateMachineVersionsResult = new ListStateMachineVersionsResult();
        emptyListStateMachineVersionsResult.setStateMachineVersions(Collections.emptyList());
        when(proxy.injectCredentialsAndInvoke(eq(listStateMachineVersionsRequest), any(Function.class))).thenReturn(emptyListStateMachineVersionsResult);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModels()).isEqualTo(Collections.emptyList());
    }

    @Test
    public void testStateMachineNotFound_returnsNotFoundError() {
        final AmazonServiceException exceptionStateMachineDoesNotExist = createAndMockAmazonServiceException(STATE_MACHINE_DOES_NOT_EXIST_ERROR_CODE);
        assertFailure(exceptionStateMachineDoesNotExist.getMessage(), HandlerErrorCode.NotFound);
    }

    @Test
    public void testInvalidToken_returnsInvalidRequestError() {
        final AmazonServiceException exceptionInvalidToken = createAndMockAmazonServiceException(INVALID_TOKEN);
        assertFailure(exceptionInvalidToken.getMessage(), HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void testFailureAccessDeniedError_returnsAccessDeniedError() {
        final AmazonServiceException exceptionAccessDenied = createAndMockAmazonServiceException(ACCESS_DENIED_ERROR_CODE);
        assertFailure(exceptionAccessDenied.getMessage(), HandlerErrorCode.AccessDenied);
    }

    @Test
    public void testFailureThrottlingError_returnsThrottlingError() {
        final AmazonServiceException exceptionThrottling = createAndMockAmazonServiceException(THROTTLING_ERROR_CODE);
        assertFailure(exceptionThrottling.getMessage(), HandlerErrorCode.Throttling);
    }

    @Test
    public void test500_returnsServiceInternalError() {
        when(proxy.injectCredentialsAndInvoke(eq(listStateMachineVersionsRequest), any(Function.class))).thenThrow(exception500);

        assertFailure(exception500.getMessage(), HandlerErrorCode.ServiceInternalError);
    }

    private AmazonServiceException createAndMockAmazonServiceException(final String errorCode) {
        final AmazonServiceException amazonServiceException = new AmazonServiceException(errorCode);
        amazonServiceException.setStatusCode(400);
        amazonServiceException.setErrorCode(errorCode);

        when(proxy.injectCredentialsAndInvoke(eq(listStateMachineVersionsRequest), any(Function.class))).thenThrow(amazonServiceException);

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
