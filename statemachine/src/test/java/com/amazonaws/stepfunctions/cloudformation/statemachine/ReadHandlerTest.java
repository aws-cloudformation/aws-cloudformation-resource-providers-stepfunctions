package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.model.DescribeStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;
import java.util.function.Function;

import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.OPERATION_FAILURE;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.OPERATION_SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends HandlerTestBase {

    private ReadHandler handler = new ReadHandler();

    private final ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    private ResourceHandlerRequest<ResourceModel> request;

    @BeforeEach
    public void setup() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder().arn(STATE_MACHINE_ARN).build())
                .build();
    }

    @Test
    public void testSuccess() {
        DescribeStateMachineResult describeStateMachineResult = new DescribeStateMachineResult();
        describeStateMachineResult.setName(STATE_MACHINE_NAME);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(DescribeStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(describeStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel().getName()).isEqualTo(STATE_MACHINE_NAME);
    }

    @Test
    public void test500() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(DescribeStateMachineRequest.class), Mockito.any(Function.class))).thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exception500.getMessage());
    }

    // Metrics Logging Tests
    @Test
    public void testLogsCorrectOperationType() {
        DescribeStateMachineResult describeStateMachineResult = new DescribeStateMachineResult();
        describeStateMachineResult.setName(STATE_MACHINE_NAME);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(DescribeStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(describeStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(HandlerOperationType.READ.toString());
    }

    @Test
    public void testLogsCorrectOperationStatus_Success() {
        DescribeStateMachineResult describeStateMachineResult = new DescribeStateMachineResult();
        describeStateMachineResult.setName(STATE_MACHINE_NAME);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(DescribeStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(describeStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(OPERATION_SUCCESS.loggingKey);
    }

    @Test
    public void testLogsCorrectOperationStatus_Failure() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(DescribeStateMachineRequest.class), Mockito.any(Function.class))).thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(3)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(OPERATION_FAILURE.loggingKey);
    }

}
