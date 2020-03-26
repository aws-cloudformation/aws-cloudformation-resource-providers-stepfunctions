package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.model.DescribeStateMachineResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends HandlerTestBase {

    private ReadHandler handler = new ReadHandler();

    private ResourceHandlerRequest<ResourceModel> request;

    @BeforeEach
    public void setup() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder().id(STATE_MACHINE_ARN).build())
                .build();
    }

    @Test
    public void testSuccess() {
        DescribeStateMachineResult describeStateMachineResult = new DescribeStateMachineResult();
        describeStateMachineResult.setName(STATE_MACHINE_NAME);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any())).thenReturn(describeStateMachineResult);

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
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any())).thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exception500.getMessage());
    }

}
