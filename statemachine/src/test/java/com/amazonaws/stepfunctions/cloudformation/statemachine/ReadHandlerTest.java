package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.model.DescribeStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineResult;
import com.amazonaws.services.stepfunctions.model.ListTagsForResourceResult;
import com.amazonaws.services.stepfunctions.model.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
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
        describeStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);
        describeStateMachineResult.setName(STATE_MACHINE_NAME);
        describeStateMachineResult.setDefinition(DEFINITION);
        describeStateMachineResult.setRoleArn(ROLE_ARN);
        describeStateMachineResult.setType(EXPRESS_TYPE);

        LoggingConfiguration loggingConfiguration = createLoggingConfiguration();
        describeStateMachineResult.setLoggingConfiguration(Translator.getLoggingConfiguration(loggingConfiguration));

        TracingConfiguration tracingConfiguration = createTracingConfiguration(true);
        describeStateMachineResult.setTracingConfiguration(Translator.getTracingConfiguration(tracingConfiguration));

        List<Tag> stateMachineTags = new ArrayList<>();
        stateMachineTags.add(new Tag().withKey("Key1").withValue("Value1"));
        stateMachineTags.add(new Tag().withKey("Key2").withValue("Value2"));

        ListTagsForResourceResult listTagsForResourceResult = new ListTagsForResourceResult();
        listTagsForResourceResult.setTags(stateMachineTags);

        List<TagsEntry> expectedTagEntries = new ArrayList<>();
        expectedTagEntries.add(new TagsEntry("Key1", "Value1"));
        expectedTagEntries.add(new TagsEntry("Key2", "Value2"));

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(describeStateMachineResult)
                .thenReturn(listTagsForResourceResult);

        ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();

        ResourceModel resourceModel = response.getResourceModel();

        assertThat(resourceModel.getTags()).isEqualTo(expectedTagEntries);
        assertThat(resourceModel.getArn()).isEqualTo(STATE_MACHINE_ARN);
        assertThat(resourceModel.getName()).isEqualTo(STATE_MACHINE_NAME);
        assertThat(resourceModel.getDefinitionString()).isEqualTo(DEFINITION);
        assertThat(resourceModel.getRoleArn()).isEqualTo(ROLE_ARN);
        assertThat(resourceModel.getStateMachineName()).isEqualTo(STATE_MACHINE_NAME);
        assertThat(resourceModel.getStateMachineType()).isEqualTo(EXPRESS_TYPE);

        LoggingConfiguration modelLoggingConfiguration = resourceModel.getLoggingConfiguration();
        assertThat(modelLoggingConfiguration.getIncludeExecutionData())
                .isEqualTo(loggingConfiguration.getIncludeExecutionData());
        assertThat(modelLoggingConfiguration.getLevel()).isEqualTo(loggingConfiguration.getLevel());

        List<LogDestination> modelLogDestinations = modelLoggingConfiguration.getDestinations();
        assertThat(modelLogDestinations.size())
                .isEqualTo(loggingConfiguration.getDestinations().size());
        for (int i=0; i<modelLogDestinations.size(); i++) {
            assertThat(modelLogDestinations.get(i).getCloudWatchLogsLogGroup().getLogGroupArn())
                    .isEqualTo(loggingConfiguration.getDestinations().get(i).getCloudWatchLogsLogGroup().getLogGroupArn());
        }

        TracingConfiguration modelTracingConfiguration = resourceModel.getTracingConfiguration();
        assertThat(modelTracingConfiguration.getEnabled()).isEqualTo(tracingConfiguration.getEnabled());
    }

    @Test
    public void testReturnsFailed_whenDescribeStateMachineThrows500() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(DescribeStateMachineRequest.class), Mockito.any(Function.class))).thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exception500.getMessage());
    }

    @Test
    public void testReturnsFailed_whenListTagsForResourceThrows500() {
        DescribeStateMachineResult describeStateMachineResult = new DescribeStateMachineResult();
        describeStateMachineResult.setName(STATE_MACHINE_NAME);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(describeStateMachineResult)
                .thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exception500.getMessage());
    }

    @Test
    public void testWhenArnMissing_throwsStateMachineDoesNotExist() {
        request.setDesiredResourceState(ResourceModel.builder().build());

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getErrorCode()).isEqualTo(HandlerErrorCode.NotFound);
        assertThat(response.getMessage()).contains(Constants.STATE_MACHINE_DOES_NOT_EXIST_ERROR_MESSAGE);
    }

    // Metrics Logging Tests
    @Test
    public void testLogsCorrectOperationType() {
        DescribeStateMachineResult describeStateMachineResult = new DescribeStateMachineResult();
        describeStateMachineResult.setName(STATE_MACHINE_NAME);

        List<Tag> stateMachineTags = new ArrayList<>();
        stateMachineTags.add(new Tag().withKey("Key1").withValue("Value1"));
        stateMachineTags.add(new Tag().withKey("Key2").withValue("Value2"));

        ListTagsForResourceResult listTagsForResourceResult = new ListTagsForResourceResult();
        listTagsForResourceResult.setTags(stateMachineTags);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(describeStateMachineResult)
                .thenReturn(listTagsForResourceResult);

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

        List<Tag> stateMachineTags = new ArrayList<>();
        stateMachineTags.add(new Tag().withKey("Key1").withValue("Value1"));
        stateMachineTags.add(new Tag().withKey("Key2").withValue("Value2"));

        ListTagsForResourceResult listTagsForResourceResult = new ListTagsForResourceResult();
        listTagsForResourceResult.setTags(stateMachineTags);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(describeStateMachineResult)
                .thenReturn(listTagsForResourceResult);

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
