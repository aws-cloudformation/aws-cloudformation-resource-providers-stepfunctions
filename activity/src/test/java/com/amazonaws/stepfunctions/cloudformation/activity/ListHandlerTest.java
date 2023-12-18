package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.stepfunctions.model.ActivityListItem;
import com.amazonaws.services.stepfunctions.model.ListActivitiesRequest;
import com.amazonaws.services.stepfunctions.model.ListActivitiesResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Function;

import static com.amazonaws.stepfunctions.cloudformation.activity.Constants.ACCESS_DENIED_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.activity.Constants.INVALID_TOKEN;
import static com.amazonaws.stepfunctions.cloudformation.activity.Constants.THROTTLING_ERROR_CODE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends HandlerTestBase {
    private ListHandler handler = new ListHandler();

    @Mock
    private ListActivitiesResult mockListActivitiesResult;
    @Mock
    private ActivityListItem mockActivityListItem;

    private final String ACTIVITY_ARN = "arn:aws:states:us-east-1:1234567890:activity:foo";
    private final String ACTIVITY_NAME = "name";
    private final String NEXT_TOKEN = "token";

    private ResourceHandlerRequest<ResourceModel> request;

    @BeforeEach
    public void setup() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .build();
    }

    @Test
    public void testSuccess() {
        final ResourceModel expectedModel = ResourceModel.builder()
                .arn(ACTIVITY_ARN)
                .name(ACTIVITY_NAME)
                .build();

        when(proxy.injectCredentialsAndInvoke(any(ListActivitiesRequest.class), any(Function.class)))
                .thenReturn(mockListActivitiesResult);

        when(mockListActivitiesResult.getActivities()).thenReturn(Arrays.asList(mockActivityListItem));
        when(mockListActivitiesResult.getNextToken()).thenReturn(null);
        when(mockActivityListItem.getActivityArn()).thenReturn(ACTIVITY_ARN);
        when(mockActivityListItem.getName()).thenReturn(ACTIVITY_NAME);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModels()).containsExactly(expectedModel);
        assertThat(response.getNextToken()).isNull();
    }

    @Test
    public void testSuccessReturnsNextToken() {
        final ResourceModel expectedModel = ResourceModel.builder()
                .arn(ACTIVITY_ARN)
                .name(ACTIVITY_NAME)
                .build();

        when(proxy.injectCredentialsAndInvoke(any(ListActivitiesRequest.class), any(Function.class)))
                .thenReturn(mockListActivitiesResult);

        when(mockListActivitiesResult.getActivities()).thenReturn(Arrays.asList(mockActivityListItem));
        when(mockListActivitiesResult.getNextToken()).thenReturn(NEXT_TOKEN);

        when(mockActivityListItem.getActivityArn()).thenReturn(ACTIVITY_ARN);
        when(mockActivityListItem.getName()).thenReturn(ACTIVITY_NAME);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModels()).containsExactly(expectedModel);
        assertThat(response.getNextToken()).isEqualTo(NEXT_TOKEN);
    }

    @Test
    public void testSuccessWithEmptyList() {
        when(mockListActivitiesResult.getActivities()).thenReturn(Collections.emptyList());

        when(proxy.injectCredentialsAndInvoke(any(ListActivitiesRequest.class), any(Function.class)))
                .thenReturn(mockListActivitiesResult);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModels()).isEmpty();
    }

    @Test
    public void testFailureWithInvalidTokenError() {
        final AmazonServiceException exceptionInvalidToken = createAndMockAmazonServiceException(INVALID_TOKEN);
        assertFailure(exceptionInvalidToken.getMessage(), HandlerErrorCode.InvalidRequest);
    }

    @Test
    public void testFailureAccessDeniedError() {
        final AmazonServiceException exceptionAccessDenied = createAndMockAmazonServiceException(ACCESS_DENIED_ERROR_CODE);
        assertFailure(exceptionAccessDenied.getMessage(), HandlerErrorCode.AccessDenied);
    }

    @Test
    public void testFailureThrottlingError() {
        final AmazonServiceException exceptionThrottling = createAndMockAmazonServiceException(THROTTLING_ERROR_CODE);
        assertFailure(exceptionThrottling.getMessage(), HandlerErrorCode.Throttling);
    }

    @Test
    public void testFailureWithInternalError() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(ListActivitiesRequest.class), Mockito.any(Function.class)))
                .thenThrow(exception500);

        assertFailure(exception500.getMessage(), HandlerErrorCode.ServiceInternalError);
    }

    private AmazonServiceException createAndMockAmazonServiceException(final String errorCode) {
        final AmazonServiceException amazonServiceException = new AmazonServiceException(errorCode);
        amazonServiceException.setStatusCode(400);
        amazonServiceException.setErrorCode(errorCode);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(ListActivitiesRequest.class), Mockito.any(Function.class)))
                .thenThrow(amazonServiceException);

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
