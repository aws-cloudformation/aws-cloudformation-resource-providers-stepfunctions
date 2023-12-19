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

    private final ReadHandler handler = new ReadHandler();

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
        final DescribeStateMachineResult describeStateMachineResult = new DescribeStateMachineResult();
        describeStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);
        describeStateMachineResult.setName(STATE_MACHINE_NAME);
        describeStateMachineResult.setDefinition(DEFINITION);
        describeStateMachineResult.setRoleArn(ROLE_ARN);
        describeStateMachineResult.setType(EXPRESS_TYPE);
        describeStateMachineResult.setRevisionId(STATE_MACHINE_REVISION_ID);

        final LoggingConfiguration loggingConfiguration = createLoggingConfiguration();
        describeStateMachineResult.setLoggingConfiguration(Translator.getLoggingConfiguration(loggingConfiguration));

        final TracingConfiguration tracingConfiguration = createTracingConfiguration(true);
        describeStateMachineResult.setTracingConfiguration(Translator.getTracingConfiguration(tracingConfiguration));

        final List<Tag> stateMachineTags = new ArrayList<>();
        stateMachineTags.add(new Tag().withKey("Key1").withValue("Value1"));
        stateMachineTags.add(new Tag().withKey("Key2").withValue("Value2"));

        final ListTagsForResourceResult listTagsForResourceResult = new ListTagsForResourceResult();
        listTagsForResourceResult.setTags(stateMachineTags);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(describeStateMachineResult)
                .thenReturn(listTagsForResourceResult);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        final List<TagsEntry> expectedTagEntries = new ArrayList<>();
        expectedTagEntries.add(new TagsEntry("Key1", "Value1"));
        expectedTagEntries.add(new TagsEntry("Key2", "Value2"));

        final ResourceModel expectedModel = new ResourceModel(
                STATE_MACHINE_ARN,
                STATE_MACHINE_NAME,
                DEFINITION,
                ROLE_ARN,
                STATE_MACHINE_NAME,
                EXPRESS_TYPE,
                STATE_MACHINE_REVISION_ID,
                loggingConfiguration,
                tracingConfiguration,
                null,
                null,
                null,
                expectedTagEntries);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void testSuccess_whenDescribeCallReturnsNullRevisionId_handlerSetsSpecialInitialRevisionIdKey() {
        final DescribeStateMachineResult describeStateMachineResult = new DescribeStateMachineResult();
        describeStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);
        describeStateMachineResult.setName(STATE_MACHINE_NAME);
        describeStateMachineResult.setDefinition(DEFINITION);
        describeStateMachineResult.setRoleArn(ROLE_ARN);
        describeStateMachineResult.setType(EXPRESS_TYPE);

        final LoggingConfiguration loggingConfiguration = createLoggingConfiguration();
        describeStateMachineResult.setLoggingConfiguration(Translator.getLoggingConfiguration(loggingConfiguration));

        final TracingConfiguration tracingConfiguration = createTracingConfiguration(true);
        describeStateMachineResult.setTracingConfiguration(Translator.getTracingConfiguration(tracingConfiguration));

        final List<Tag> stateMachineTags = new ArrayList<>();
        stateMachineTags.add(new Tag().withKey("Key1").withValue("Value1"));
        stateMachineTags.add(new Tag().withKey("Key2").withValue("Value2"));

        final ListTagsForResourceResult listTagsForResourceResult = new ListTagsForResourceResult();
        listTagsForResourceResult.setTags(stateMachineTags);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(describeStateMachineResult)
                .thenReturn(listTagsForResourceResult);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        final List<TagsEntry> expectedTagEntries = new ArrayList<>();
        expectedTagEntries.add(new TagsEntry("Key1", "Value1"));
        expectedTagEntries.add(new TagsEntry("Key2", "Value2"));

        final ResourceModel expectedModel = new ResourceModel(
                STATE_MACHINE_ARN,
                STATE_MACHINE_NAME,
                DEFINITION,
                ROLE_ARN,
                STATE_MACHINE_NAME,
                EXPRESS_TYPE,
                Constants.STATE_MACHINE_INITIAL_REVISION_ID,
                loggingConfiguration,
                tracingConfiguration,
                null,
                null,
                null,
                expectedTagEntries);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void testReturnsFailed_whenDescribeStateMachineThrows500() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(DescribeStateMachineRequest.class),
                Mockito.any(Function.class))).thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exception500.getMessage());
    }

    @Test
    public void testRedactsTags_whenListTagsForResourceThrowsAccessDenied() {
        final DescribeStateMachineResult describeStateMachineResult = new DescribeStateMachineResult();
        describeStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);
        describeStateMachineResult.setName(STATE_MACHINE_NAME);
        describeStateMachineResult.setDefinition(DEFINITION);
        describeStateMachineResult.setRoleArn(ROLE_ARN);
        describeStateMachineResult.setType(EXPRESS_TYPE);
        describeStateMachineResult.setRevisionId(STATE_MACHINE_REVISION_ID);

        final LoggingConfiguration loggingConfiguration = createLoggingConfiguration();
        describeStateMachineResult.setLoggingConfiguration(Translator.getLoggingConfiguration(loggingConfiguration));

        final TracingConfiguration tracingConfiguration = createTracingConfiguration(true);
        describeStateMachineResult.setTracingConfiguration(Translator.getTracingConfiguration(tracingConfiguration));

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(describeStateMachineResult)
                .thenThrow(accessDeniedException);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        final ResourceModel expectedModel = new ResourceModel(
                STATE_MACHINE_ARN,
                STATE_MACHINE_NAME,
                DEFINITION,
                ROLE_ARN,
                STATE_MACHINE_NAME,
                EXPRESS_TYPE,
                STATE_MACHINE_REVISION_ID,
                loggingConfiguration,
                tracingConfiguration,
                null,
                null,
                null,
                null);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModel()).isEqualTo(expectedModel);
    }

    @Test
    public void testReturnsFailed_whenListTagsForResourceThrows500() {
        final DescribeStateMachineResult describeStateMachineResult = new DescribeStateMachineResult();
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
        final DescribeStateMachineResult describeStateMachineResult = new DescribeStateMachineResult();
        describeStateMachineResult.setName(STATE_MACHINE_NAME);

        final List<Tag> stateMachineTags = new ArrayList<>();
        stateMachineTags.add(new Tag().withKey("Key1").withValue("Value1"));
        stateMachineTags.add(new Tag().withKey("Key2").withValue("Value2"));

        final ListTagsForResourceResult listTagsForResourceResult = new ListTagsForResourceResult();
        listTagsForResourceResult.setTags(stateMachineTags);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(describeStateMachineResult)
                .thenReturn(listTagsForResourceResult);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        final List<String> loggedStrings = argumentCaptor.getAllValues();
        final String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(HandlerOperationType.READ.toString());
    }

    @Test
    public void testLogsCorrectOperationStatus_Success() {
        final DescribeStateMachineResult describeStateMachineResult = new DescribeStateMachineResult();
        describeStateMachineResult.setName(STATE_MACHINE_NAME);

        final List<Tag> stateMachineTags = new ArrayList<>();
        stateMachineTags.add(new Tag().withKey("Key1").withValue("Value1"));
        stateMachineTags.add(new Tag().withKey("Key2").withValue("Value2"));

        final ListTagsForResourceResult listTagsForResourceResult = new ListTagsForResourceResult();
        listTagsForResourceResult.setTags(stateMachineTags);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(describeStateMachineResult)
                .thenReturn(listTagsForResourceResult);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        final List<String> loggedStrings = argumentCaptor.getAllValues();
        final String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(OPERATION_SUCCESS.loggingKey);
    }

    @Test
    public void testLogsCorrectOperationStatus_Failure() {
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(DescribeStateMachineRequest.class),
                Mockito.any(Function.class))).thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(3)).log(argumentCaptor.capture());
        final List<String> loggedStrings = argumentCaptor.getAllValues();
        final String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(OPERATION_FAILURE.loggingKey);
    }

}
