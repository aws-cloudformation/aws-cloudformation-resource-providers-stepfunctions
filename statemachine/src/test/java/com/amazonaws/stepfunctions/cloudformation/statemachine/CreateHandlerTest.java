package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.model.CreateStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;

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
                .desiredResourceState(ResourceModel.builder()
                        .id(STATE_MACHINE_ARN)
                        .roleArn(ROLE_ARN)
                        .definitionString("{}")
                        .stateMachineType(STANDARD_STATE_MACHINE_TYPE)
                        .stateMachineName(STATE_MACHINE_NAME)
                        .loggingConfiguration(createLoggingConfiguration())
                        .build())
                .build();
    }

    @Test
    public void handleSuccess() {
        CreateStateMachineRequest createStateMachineRequest = new CreateStateMachineRequest();
        createStateMachineRequest.setName(STATE_MACHINE_NAME);
        createStateMachineRequest.setType(STANDARD_STATE_MACHINE_TYPE);
        createStateMachineRequest.setDefinition("{}");
        createStateMachineRequest.setRoleArn(ROLE_ARN);
        createStateMachineRequest.setLoggingConfiguration(Translator.getLoggingConfiguration(createLoggingConfiguration()));
        createStateMachineRequest.setTags(new ArrayList<>());

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.eq(createStateMachineRequest), Mockito.any())).thenReturn(createStateMachineResult);

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
        assertThat(response.getResourceModel().getId()).isEqualTo(STATE_MACHINE_ARN);
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
