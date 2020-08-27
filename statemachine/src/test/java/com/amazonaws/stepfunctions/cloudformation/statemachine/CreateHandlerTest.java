package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineResult;
import com.amazonaws.stepfunctions.cloudformation.statemachine.s3.GetObjectFunction;
import com.amazonaws.stepfunctions.cloudformation.statemachine.s3.GetObjectResult;
import com.amazonaws.util.StringInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends HandlerTestBase {

    private static final ObjectMapper mapper = new ObjectMapper();
    public static final String DEFAULT_S3_BUCKET = "Bucket";
    public static final String DEFAULT_S3_KEY = "Key";
    public static final String DEFAULT_S3_OBJECT_VERSION = "1";

    static {
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    private CreateHandler handler = new CreateHandler();

    private ResourceHandlerRequest<ResourceModel> request;

    @BeforeEach
    public void setup() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ARN)
                        .roleArn(ROLE_ARN)
                        .stateMachineType(STANDARD_STATE_MACHINE_TYPE)
                        .stateMachineName(STATE_MACHINE_NAME)
                        .loggingConfiguration(createLoggingConfiguration())
                        .tracingConfiguration(createTracingConfiguration())
                        .build())
                .build();
    }

    @Test
    public void handleSuccess() {
        request.getDesiredResourceState().setDefinitionString("{}");

        CreateStateMachineRequest createStateMachineRequest = new CreateStateMachineRequest();
        createStateMachineRequest.setName(STATE_MACHINE_NAME);
        createStateMachineRequest.setType(STANDARD_STATE_MACHINE_TYPE);
        createStateMachineRequest.setDefinition("{}");
        createStateMachineRequest.setRoleArn(ROLE_ARN);
        createStateMachineRequest.setLoggingConfiguration(Translator.getLoggingConfiguration(createLoggingConfiguration()));
        createStateMachineRequest.setTracingConfiguration(Translator.getTracingConfiguration(createTracingConfiguration()));
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
        assertThat(response.getResourceModel().getArn()).isEqualTo(STATE_MACHINE_ARN);
    }

    @Test
    public void test500() {
        request.getDesiredResourceState().setDefinitionString("{}");

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any())).thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(exception500.getMessage());
    }

    @Test
    public void testDefinitionReplacement() {
        String definition = "{\n" +
                "  \"States\": {\n" +
                "    \"lambda_01\": {\n" +
                "      \"Resource\": \"${lambdaArn01}\"\n" +
                "    },\n" +
                "    \"lambda_02\": {\n" +
                "      \"Resource\": \"${lambdaArn02}\"\n" +
                "    },\n" +
                "    \"lambda_03\": {\n" +
                "      \"Parameters\": {\n" +
                "        \"Lambda01\": \"${lambdaArn01}\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        Map<String, String> substitutions = new HashMap<>();
        substitutions.put("lambdaArn01", "lambdaArn01");
        substitutions.put("lambdaArn02", "lambdaArn02");

        request.getDesiredResourceState().setDefinitionSubstitutions(substitutions);
        request.getDesiredResourceState().setDefinitionString(definition);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any())).thenReturn(createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        String transformedDefinition = response.getResourceModel().getDefinitionString();

        assertThat(!transformedDefinition.contains("${lambdaArn01}")).isTrue();
        assertThat(!transformedDefinition.contains("${lambdaArn02}")).isTrue();
    }

    @Test
    public void testDefinitionFromS3() throws Exception {
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new StringInputStream("{}"));
        GetObjectResult getObjectResult = new GetObjectResult(s3Object);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any())).thenReturn(getObjectResult, createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModel().getDefinitionString()).isEqualTo("{}");
    }

    @Test
    public void testDefinitionFromS3InYAML() throws Exception {
        String formattedJson = "{\n" +
                "  \"Comment\" : \"Hello World\"\n" +
                "}";

        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new StringInputStream("Comment: Hello World"));
        GetObjectResult getObjectResult = new GetObjectResult(s3Object);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any())).thenReturn(getObjectResult, createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModel().getDefinitionString()).isEqualTo(formattedJson);
    }

    @Test
    public void testInvalidYamlDefinitionFromS3() throws Exception {
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new StringInputStream("Invalid: -"));
        GetObjectResult getObjectResult = new GetObjectResult(s3Object);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any())).thenReturn(getObjectResult, createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    public void testInvalidJsonDefinitionFromS3() throws Exception {
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new StringInputStream("Invalid: -"));
        GetObjectResult getObjectResult = new GetObjectResult(s3Object);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any())).thenReturn(getObjectResult, createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    public void testGetObjectFunction() {
        AmazonS3 client = Mockito.mock(AmazonS3.class);

        S3Object s3Object = new S3Object();
        GetObjectRequest request = new GetObjectRequest("Bucket", "Key");

        GetObjectFunction function = new GetObjectFunction(client);

        Mockito.when(client.getObject(request)).thenReturn(s3Object);

        assertThat(function.get(request)).isEqualTo(new GetObjectResult(s3Object));
    }

    @Test
    public void testWithoutDefinition() {
        request.getDesiredResourceState().setDefinitionString(null);
        request.getDesiredResourceState().setDefinitionS3Location(null);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(Constants.DEFINITION_MISSING_ERROR_MESSAGE);
    }

    @Test
    public void testWithMoreThanOneDefinitions() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(Constants.DEFINITION_REDUNDANT_ERROR_MESSAGE);
    }

}
