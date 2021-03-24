package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.stepfunctions.model.ListTagsForResourceResult;
import com.amazonaws.services.stepfunctions.model.Tag;
import com.amazonaws.services.stepfunctions.model.TagResourceRequest;
import com.amazonaws.services.stepfunctions.model.TagResourceResult;
import com.amazonaws.services.stepfunctions.model.UntagResourceRequest;
import com.amazonaws.services.stepfunctions.model.UntagResourceResult;
import com.amazonaws.services.stepfunctions.model.UpdateStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.UpdateStateMachineResult;
import com.amazonaws.stepfunctions.cloudformation.statemachine.s3.GetObjectResult;
import com.amazonaws.util.StringInputStream;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.OPERATION_FAILURE;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.OPERATION_SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class UpdateHandlerTest extends HandlerTestBase {

    private UpdateHandler handler = new UpdateHandler();
    public static final String DEFAULT_S3_BUCKET = "Bucket";
    public static final String DEFAULT_S3_KEY = "Key";
    public static final String DEFAULT_S3_OBJECT_VERSION = "1";

    private final ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    private ResourceHandlerRequest<ResourceModel> request;

    @BeforeEach
    public void setup() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder().arn(STATE_MACHINE_ARN).roleArn(ROLE_ARN).build())
                .previousResourceState(ResourceModel.builder().arn(STATE_MACHINE_ARN).roleArn(ROLE_ARN).build())
                .build();
    }

    @Test
    public void testSuccess() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getPreviousResourceState().setDefinitionString("{}");

        UntagResourceRequest untagResourceRequest = new UntagResourceRequest();
        untagResourceRequest.setResourceArn(STATE_MACHINE_ARN);
        untagResourceRequest.setTagKeys(Lists.newArrayList("K1", "K2"));

        TagResourceRequest tagResourceRequest = new TagResourceRequest();
        tagResourceRequest.setResourceArn(STATE_MACHINE_ARN);
        tagResourceRequest.setTags(Lists.newArrayList(
                new Tag().withKey("K3").withValue("V3")
        ));

        Map<String, String> resourceTags = new HashMap<>();
        Map<String, String> previousResourceTags = new HashMap<>();

        resourceTags.put("K3", "V3");
        previousResourceTags.put("K1", "V1");
        previousResourceTags.put("K2", "V2");

        request.setDesiredResourceTags(resourceTags);
        request.setPreviousResourceTags(previousResourceTags);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.eq(untagResourceRequest), Mockito.any(Function.class))).thenReturn(new UntagResourceResult());
        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.eq(tagResourceRequest), Mockito.any(Function.class))).thenReturn(new TagResourceResult());

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackContext()).isNull();
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    public void test500() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getPreviousResourceState().setDefinitionString("{}");

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(UpdateStateMachineRequest.class), Mockito.any(Function.class))).thenThrow(exception500);

        ProgressEvent<ResourceModel, CallbackContext> response
            = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exception500.getMessage());
    }

    @Test
    public void testUpdateExpressStateMachineWithLoggingConfiguration() {
        request.getDesiredResourceState().setLoggingConfiguration(createLoggingConfiguration());
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getPreviousResourceState().setDefinitionString("{}");

        UpdateStateMachineRequest updateStateMachineRequest = new UpdateStateMachineRequest();
        updateStateMachineRequest.setStateMachineArn(STATE_MACHINE_ARN);
        updateStateMachineRequest.setRoleArn(ROLE_ARN);
        updateStateMachineRequest.setDefinition("{}");
        updateStateMachineRequest.setLoggingConfiguration(Translator.getLoggingConfiguration(createLoggingConfiguration()));

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(proxy, Mockito.times(1))
                .injectCredentialsAndInvoke(Mockito.eq(updateStateMachineRequest), Mockito.any(Function.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    }

    @Test
    public void testUpdateExpressStateMachineWithTracingConfiguration() {

        ListTagsForResourceResult listTagsForResourceResult = new ListTagsForResourceResult();
        listTagsForResourceResult.setTags(new ArrayList<>());

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(UpdateStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(listTagsForResourceResult);

        request.getDesiredResourceState().setTracingConfiguration(createTracingConfiguration(TRACING_CONFIGURATION_DISABLED));
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getPreviousResourceState().setDefinitionString("{}");

        UpdateStateMachineRequest updateStateMachineRequest = new UpdateStateMachineRequest();
        updateStateMachineRequest.setStateMachineArn(STATE_MACHINE_ARN);
        updateStateMachineRequest.setRoleArn(ROLE_ARN);
        updateStateMachineRequest.setDefinition("{}");
        updateStateMachineRequest.setTracingConfiguration(Translator.getTracingConfiguration(createTracingConfiguration(TRACING_CONFIGURATION_DISABLED)));

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(proxy, Mockito.times(1))
                .injectCredentialsAndInvoke(Mockito.eq(updateStateMachineRequest), Mockito.any(Function.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    }

    @Test
    public void testDefinitionFromS3() throws Exception {
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));
        request.getPreviousResourceState().setDefinitionString("{}");

        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new StringInputStream("{}"));
        GetObjectResult getObjectResult = new GetObjectResult(s3Object);

        UpdateStateMachineResult updateStateMachineResult = new UpdateStateMachineResult();
        ListTagsForResourceResult listTagsForResourceResult = new ListTagsForResourceResult();

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class)))
                .thenReturn(getObjectResult, updateStateMachineResult, listTagsForResourceResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModel().getDefinitionString()).isEqualTo("{}");
    }

    // Metrics Logging Tests
    @Test
    public void testLogsCorrectOperationType() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getPreviousResourceState().setDefinitionString("{}");

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(HandlerOperationType.UPDATE.toString());
    }

    @Test
    public void testLogsCorrectOperationStatus_Success() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getPreviousResourceState().setDefinitionString("{}");

        ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(OPERATION_SUCCESS.loggingKey);
    }

    @Test
    public void testLogsCorrectOperationStatus_Failure() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getPreviousResourceState().setDefinitionString("{}");

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(UpdateStateMachineRequest.class), Mockito.any(Function.class))).thenThrow(exception500);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(3)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(OPERATION_FAILURE.loggingKey);
    }

}
