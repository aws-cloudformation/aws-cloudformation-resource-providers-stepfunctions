package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.model.ListStateMachinesRequest;
import com.amazonaws.services.stepfunctions.model.ListStateMachinesResult;
import com.amazonaws.services.stepfunctions.model.StateMachineListItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.OPERATION_FAILURE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ListHandlerTest extends HandlerTestBase {
    private ListHandler handler = new ListHandler();

    @Mock
    private ListStateMachinesResult mockListStateMachinesResult;
    @Mock
    private StateMachineListItem mockStateMachineListItem;

    private final String STATE_MACHINE_ARN = "arn";
    private final String STATE_MACHINE_NAME = "name";
    private final String STATE_MACHINE_TYPE = "type";
    private final String NEXT_TOKEN = "token";

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
        final ResourceModel expectedModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ARN)
                .stateMachineName(STATE_MACHINE_NAME)
                .stateMachineType(STATE_MACHINE_TYPE)
                .build();

        when(proxy.injectCredentialsAndInvoke(any(ListStateMachinesRequest.class), any(Function.class)))
                .thenReturn(mockListStateMachinesResult);

        when(mockListStateMachinesResult.getStateMachines()).thenReturn(Arrays.asList(mockStateMachineListItem));

        when(mockStateMachineListItem.getStateMachineArn()).thenReturn(STATE_MACHINE_ARN);
        when(mockStateMachineListItem.getName()).thenReturn(STATE_MACHINE_NAME);
        when(mockStateMachineListItem.getType()).thenReturn(STATE_MACHINE_TYPE);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModels()).containsExactly(expectedModel);
    }

    @Test
    public void testSuccessReturnsNextToken() {
        final ResourceModel expectedModel = ResourceModel.builder()
                .arn(STATE_MACHINE_ARN)
                .stateMachineName(STATE_MACHINE_NAME)
                .stateMachineType(STATE_MACHINE_TYPE)
                .build();

        when(proxy.injectCredentialsAndInvoke(any(ListStateMachinesRequest.class), any(Function.class)))
                .thenReturn(mockListStateMachinesResult);

        when(mockListStateMachinesResult.getStateMachines()).thenReturn(Arrays.asList(mockStateMachineListItem));
        when(mockListStateMachinesResult.getNextToken()).thenReturn(NEXT_TOKEN);

        when(mockStateMachineListItem.getStateMachineArn()).thenReturn(STATE_MACHINE_ARN);
        when(mockStateMachineListItem.getName()).thenReturn(STATE_MACHINE_NAME);
        when(mockStateMachineListItem.getType()).thenReturn(STATE_MACHINE_TYPE);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModels()).containsExactly(expectedModel);
        assertThat(response.getNextToken()).isEqualTo(NEXT_TOKEN);
    }

    @Test
    public void testSuccessWithEmptyList() {

        when(mockListStateMachinesResult.getStateMachines()).thenReturn(Collections.emptyList());

        when(proxy.injectCredentialsAndInvoke(any(ListStateMachinesRequest.class), any(Function.class)))
                .thenReturn(mockListStateMachinesResult);

        final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModels()).isEmpty();
    }

    @Test
    public void testLogsCorrectOperationStatus_Failure() {
        when(proxy.injectCredentialsAndInvoke(any(ListStateMachinesRequest.class), any(Function.class)))
                .thenThrow(exception500);

        handler.handleRequest(proxy, request, null, logger);

        verify(logger, times(3)).log(argumentCaptor.capture());
        final List<String> loggedStrings = argumentCaptor.getAllValues();
        final String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(OPERATION_FAILURE.loggingKey);
    }
}
