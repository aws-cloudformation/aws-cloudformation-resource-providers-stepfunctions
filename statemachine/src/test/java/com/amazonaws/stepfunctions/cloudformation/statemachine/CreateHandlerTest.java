package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.DEFINITION_INVALID_FORMAT;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.MULTIPLE_DEFINITIONS_PROVIDED;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.OPERATION_FAILURE;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.OPERATION_SUCCESS;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.S3_DEFINITION_JSON;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.S3_DEFINITION_SIZE_LIMIT_EXCEEDED;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.S3_DEFINITION_YAML;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.STATE_MACHINE_EXPRESS_TYPE;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.STATE_MACHINE_NAME_GENERATED;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.STATE_MACHINE_STANDARD_TYPE;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.TEMPLATE_MISSING_DEFINITION;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.MetricsLoggingKeys.TRACING_CONFIGURATION_PROVIDED;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends HandlerTestBase {

    private static final ObjectMapper mapper = new ObjectMapper();
    public static final String DEFAULT_S3_BUCKET = "Bucket";
    public static final String DEFAULT_S3_KEY = "Key";
    public static final String DEFAULT_S3_OBJECT_VERSION = "1";

    @Mock
    protected ObjectMetadata mockS3ObjectMetadata;

    static {
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    private final ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    private final CreateHandler handler = new CreateHandler();

    private ResourceHandlerRequest<ResourceModel> request;

    // Used for testing invalid object format
    private static class ClassThatJacksonCannotSerialize {}

    @BeforeEach
    public void setup() {
        request = ResourceHandlerRequest.<ResourceModel>builder()
                .region(REGION)
                .awsAccountId(AWS_ACCOUNT_ID)
                .desiredResourceState(ResourceModel.builder()
                        .arn(STATE_MACHINE_ARN)
                        .roleArn(ROLE_ARN)
                        .stateMachineType(Constants.STANDARD_STATE_MACHINE_TYPE)
                        .stateMachineName(STATE_MACHINE_NAME)
                        .loggingConfiguration(createLoggingConfiguration())
                        .tracingConfiguration(createTracingConfiguration(TRACING_CONFIGURATION_DISABLED))
                        .build())
                .build();
    }

    @Test
    public void handleSuccess() {
        request.getDesiredResourceState().setDefinitionString("{}");

        CreateStateMachineRequest createStateMachineRequest = new CreateStateMachineRequest();
        createStateMachineRequest.setName(STATE_MACHINE_NAME);
        createStateMachineRequest.setType(Constants.STANDARD_STATE_MACHINE_TYPE);
        createStateMachineRequest.setDefinition("{}");
        createStateMachineRequest.setRoleArn(ROLE_ARN);
        createStateMachineRequest.setLoggingConfiguration(Translator.getLoggingConfiguration(createLoggingConfiguration()));
        createStateMachineRequest.setTracingConfiguration(Translator.getTracingConfiguration(createTracingConfiguration(TRACING_CONFIGURATION_DISABLED)));
        createStateMachineRequest.setTags(new ArrayList<>());

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.eq(createStateMachineRequest), Mockito.any(Function.class))).thenReturn(createStateMachineResult);

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

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateStateMachineRequest.class), Mockito.any(Function.class))).thenThrow(exception500);

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
                "    \"number\": {\n" +
                "      \"Resource\": ${number}\n" +
                "    },\n" +
                "    \"boolean\": {\n" +
                "      \"Resource\": ${boolean}\n" +
                "    },\n" +
                "  }\n" +
                "}";

        Map<String, String> substitutions = new HashMap<>();
        substitutions.put("lambdaArn01", "lambdaArn01");
        substitutions.put("lambdaArn02", "lambdaArn02");
        substitutions.put("number", "5");
        substitutions.put("boolean", "true");

        request.getDesiredResourceState().setDefinitionSubstitutions(substitutions);
        request.getDesiredResourceState().setDefinitionString(definition);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        String transformedDefinition = response.getResourceModel().getDefinitionString();

        assertThat(!transformedDefinition.contains("${lambdaArn01}")).isTrue();
        assertThat(!transformedDefinition.contains("${lambdaArn02}")).isTrue();
        assertThat(!transformedDefinition.contains("${number}")).isTrue();
        assertThat(!transformedDefinition.contains("${boolean}")).isTrue();
    }

    @Test
    public void testCreateSuccess_whenDefinitionFromObject() {
        Map<String, Object> definition = new HashMap<>();

        Map<String, Object> lambdaState = new HashMap<>();
        lambdaState.put("Resource", "${lambdaArn01}");

        Map<String, Object> passState = new HashMap<>();
        passState.put("Next", "${lambdaStateName}");

        Map<String, Object> timeoutSeconds = new HashMap<>();
        passState.put("TimeoutSeconds", "${timeoutSeconds}");

        Map<String, Map<String, Object>> states = new HashMap<>();
        states.put("lambda_01", lambdaState);
        states.put("PassState", passState);
        states.put("TimeoutSeconds", timeoutSeconds);

        definition.put("States", states);

        Map<String, String> substitutions = new HashMap<>();
        substitutions.put("lambdaArn01", "lambdaArn01");
        substitutions.put("lambdaStateName", "lambda_01");
        substitutions.put("timeoutSeconds", "60");

        request.getDesiredResourceState().setDefinitionSubstitutions(substitutions);
        request.getDesiredResourceState().setDefinition(definition);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);

        String transformedDefinition = response.getResourceModel().getDefinitionString();
        assertThat(!transformedDefinition.contains("${lambdaArn01}")).isTrue();
        assertThat(!transformedDefinition.contains("${lambdaStateName}")).isTrue();

        String expectedDefinitionString = "{\n  \"States\" : {\n    \"PassState\" : {\n      \"Next\" : \"lambda_01\"\n    },\n    \"lambda_01\" : {\n      \"Resource\" : \"lambdaArn01\"\n    }\n  }\n}";
        assertThat(transformedDefinition.equals(expectedDefinitionString));
    }

    @Test
    public void testThrowsInvalidRequest_whenDefinitionFromObject_objectIsInvalid() {
        Map<String, Object> definition = new HashMap<>();
        definition.put("InvalidField", new ClassThatJacksonCannotSerialize());
        request.getDesiredResourceState().setDefinition(definition);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(Constants.DEFINITION_INVALID_FORMAT_ERROR_MESSAGE);
    }

    @Test
    public void testDefinitionFromS3() throws Exception {
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new StringInputStream("{}"));
        GetObjectResult getObjectResult = new GetObjectResult(s3Object);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class))).thenReturn(getObjectResult, createStateMachineResult);

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

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class))).thenReturn(getObjectResult, createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModel().getDefinitionString()).isEqualTo(formattedJson);
    }

    @Test
    public void testNonStringDefinitionSubstitutionFromS3Yaml() throws Exception {
        String formattedJson = "{\n" +
                "  \"StartAt\" : \"DummyState\",\n" +
                "  \"TimeoutSeconds\" : 60,\n" +
                "  \"States\" : {\n" +
                "    \"DummyState\" : {\n" +
                "      \"Type\" : \"Pass\",\n" +
                "      \"End\" : true\n" +
                "    }\n" +
                "  }\n" +
                "}";

        Map<String, String> substitutions = new HashMap<>();
        substitutions.put("startState", "DummyState");
        substitutions.put("type", "Pass");
        substitutions.put("timeoutSeconds", "60");
        substitutions.put("isEnd", "true");

        request.getDesiredResourceState().setDefinitionSubstitutions(substitutions);
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        S3Object s3Object = new S3Object();
        String yamlDefinitionInS3 = "StartAt: \"${startState}\"\nTimeoutSeconds: ${timeoutSeconds}\nStates:\n  DummyState:\n    Type: Pass\n    End: ${isEnd}\n  ";
        s3Object.setObjectContent(new StringInputStream(yamlDefinitionInS3));
        GetObjectResult getObjectResult = new GetObjectResult(s3Object);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class))).thenReturn(getObjectResult, createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getResourceModel().getDefinitionString()).isEqualTo(formattedJson);
    }

    @Test
    public void testNonStringDefinitionSubstitutionFromS3Json() throws Exception {
        String formattedJson = "{\n" +
                "  \"StartAt\" : \"DummyState\",\n" +
                "  \"TimeoutSeconds\" : 60,\n" +
                "  \"States\" : {\n" +
                "    \"DummyState\" : {\n" +
                "      \"Type\" : \"Pass\",\n" +
                "      \"End\" : true\n" +
                "    }\n" +
                "  }\n" +
                "}";

        Map<String, String> substitutions = new HashMap<>();
        substitutions.put("startState", "DummyState");
        substitutions.put("type", "Pass");
        substitutions.put("timeoutSeconds", "60");
        substitutions.put("isEnd", "true");

        request.getDesiredResourceState().setDefinitionSubstitutions(substitutions);
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        S3Object s3Object = new S3Object();
        String jsonDefinitionInS3 = "{\n  \"StartAt\": \"${startState}\",\n  \"TimeoutSeconds\": ${timeoutSeconds},\n\"States\": {\n    \"DummyState\": {\n      \"Type\": \"${type}\",\n      \"End\": ${isEnd}\n    },\n        }\n  }\n}";
        s3Object.setObjectContent(new StringInputStream(jsonDefinitionInS3));
        GetObjectResult getObjectResult = new GetObjectResult(s3Object);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class))).thenReturn(getObjectResult, createStateMachineResult);

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

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(getObjectResult, createStateMachineResult);

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

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(getObjectResult, createStateMachineResult);

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
    public void testThrowsDefinitionMissing_whenNoDefinition() {
        request.getDesiredResourceState().setDefinitionString(null);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(Constants.DEFINITION_MISSING_ERROR_MESSAGE);
    }

    @Test
    public void testThrowsDefinitionRedundant_whenMoreThanOneDefinition_DefinitionString_and_DefinitionS3Location() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(Constants.DEFINITION_REDUNDANT_ERROR_MESSAGE);
    }

    @Test
    public void testThrowsDefinitionRedundant_whenMoreThanOneDefinition_DefinitionString_and_DefinitionObject() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getDesiredResourceState().setDefinition(new HashMap<>());

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(Constants.DEFINITION_REDUNDANT_ERROR_MESSAGE);
    }

    @Test
    public void testThrowsDefinitionRedundant_whenMoreThanOneDefinition_DefinitionObject_and_DefinitionS3Location() {
        request.getDesiredResourceState().setDefinition(new HashMap<>());
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(Constants.DEFINITION_REDUNDANT_ERROR_MESSAGE);
    }

    @Test
    public void testThrowsDefinitionRedundant_whenMoreThanOneDefinition_DefinitionObject_and_DefinitionS3Location_and_DefinitionString() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getDesiredResourceState().setDefinition(new HashMap<>());
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo(Constants.DEFINITION_REDUNDANT_ERROR_MESSAGE);
    }

    // Metrics Logging Tests
    @Test
    public void testLogsCorrectOperationType() {
        request.getDesiredResourceState().setDefinitionString("{}");

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(HandlerOperationType.CREATE.toString());
    }

    @Test
    public void testLogsCorrectOperationStatus_Success() {
        request.getDesiredResourceState().setDefinitionString("{}");

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(OPERATION_SUCCESS.loggingKey);
    }

    @Test
    public void testLogsCorrectOperationStatus_Failure() {
        request.getDesiredResourceState().setDefinitionString("{}");

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateStateMachineRequest.class), Mockito.any(Function.class))).thenThrow(exception500);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(3)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(OPERATION_FAILURE.loggingKey);
    }

    @Test
    public void testLogsStateMachineNameGenerated_whenNoNameInRequest() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getDesiredResourceState().setStateMachineName(null);
        request.setLogicalResourceIdentifier("LogicalResourceIdentifier");
        request.setClientRequestToken("ClientRequestToken");

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(createStateMachineResult);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(STATE_MACHINE_NAME_GENERATED.loggingKey);
    }

    @Test
    public void testDoesNotLogStateMachineNameGenerated_whenNameInRequest() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getDesiredResourceState().setStateMachineName("ProvidedName");

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(createStateMachineResult);

        final ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).doesNotContain(STATE_MACHINE_NAME_GENERATED.loggingKey);
    }

    @Test
    public void testLogsStateMachineType_whenTypeIsExpress() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getDesiredResourceState().setStateMachineType(Constants.EXPRESS_STATE_MACHINE_TYPE);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(STATE_MACHINE_EXPRESS_TYPE.loggingKey);
    }

    @Test
    public void testLogsStateMachineType_whenTypeIsStandard() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getDesiredResourceState().setStateMachineType(Constants.STANDARD_STATE_MACHINE_TYPE);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(STATE_MACHINE_STANDARD_TYPE.loggingKey);
    }

    @Test
    public void testLogsStateMachineType_whenTypeIsNull() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getDesiredResourceState().setStateMachineType(null);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(STATE_MACHINE_STANDARD_TYPE.loggingKey);
    }

    @Test
    public void testLogsTracing_whenTracingIsEnabledTrue() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getDesiredResourceState().setTracingConfiguration(createTracingConfiguration(TRACING_CONFIGURATION_ENABLED));

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(TRACING_CONFIGURATION_PROVIDED.loggingKey);
    }

    @Test
    public void testDoesNotLogTracing_whenTracingIsEnabledFalse() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getDesiredResourceState().setTracingConfiguration(createTracingConfiguration(TRACING_CONFIGURATION_DISABLED));

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(CreateStateMachineRequest.class), Mockito.any(Function.class))).thenReturn(createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).doesNotContain(TRACING_CONFIGURATION_PROVIDED.loggingKey);
    }

    @Test
    public void testLogsMissingDefinitions_whenNoDefinitionProvided() {
        request.getDesiredResourceState().setDefinitionString(null);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(3)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(TEMPLATE_MISSING_DEFINITION.loggingKey);
    }

    @Test
    public void testLogsMultipleDefinitions_whenMultipleDefinitionsProvided() {
        request.getDesiredResourceState().setDefinitionString("{}");
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(3)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(MULTIPLE_DEFINITIONS_PROVIDED.loggingKey);
    }

    @Test
    public void testLogsDefinitionObjectInvalidFormat_whenDefinitionObjectInvalidFormat() {
        Map<String, Object> definition = new HashMap<>();
        definition.put("InvalidField", new ClassThatJacksonCannotSerialize());
        request.getDesiredResourceState().setDefinition(definition);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(3)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(DEFINITION_INVALID_FORMAT.loggingKey);
    }

    @Test
    public void testLogsS3DefinitionFormat_whenJson() throws Exception {
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new StringInputStream("{}"));
        GetObjectResult getObjectResult = new GetObjectResult(s3Object);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class))).thenReturn(getObjectResult, createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(S3_DEFINITION_JSON.loggingKey);
    }

    @Test
    public void testLogsS3DefinitionFormat_whenYaml() throws Exception {
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new StringInputStream("Comment: Hello World"));
        GetObjectResult getObjectResult = new GetObjectResult(s3Object);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class))).thenReturn(getObjectResult, createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(2)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(S3_DEFINITION_YAML.loggingKey);
    }

    @Test
    public void testLogsS3DefinitionInvalidFormat_whenInvalidJson() throws Exception {
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new StringInputStream("{"));
        GetObjectResult getObjectResult = new GetObjectResult(s3Object);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class))).thenReturn(getObjectResult, createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(3)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(DEFINITION_INVALID_FORMAT.loggingKey);
    }

    @Test
    public void testLogsS3DefinitionInvalidFormat_whenInvalidYaml() throws Exception {
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new StringInputStream(",,,"));
        GetObjectResult getObjectResult = new GetObjectResult(s3Object);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class))).thenReturn(getObjectResult, createStateMachineResult);

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(3)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(DEFINITION_INVALID_FORMAT.loggingKey);
    }

    @Test
    public void testLogsS3DefinitionSizeLimitExceeded_whenSizeLimitExceeded() throws Exception {
        request.getDesiredResourceState().setDefinitionS3Location(new S3Location(DEFAULT_S3_BUCKET, DEFAULT_S3_KEY, DEFAULT_S3_OBJECT_VERSION));

        S3Object s3Object = new S3Object();
        s3Object.setObjectMetadata(mockS3ObjectMetadata);
        GetObjectResult getObjectResult = new GetObjectResult(s3Object);

        CreateStateMachineResult createStateMachineResult = new CreateStateMachineResult();
        createStateMachineResult.setStateMachineArn(STATE_MACHINE_ARN);

        Mockito.when(proxy.injectCredentialsAndInvoke(Mockito.any(), Mockito.any(Function.class))).thenReturn(getObjectResult, createStateMachineResult);
        Mockito.when(mockS3ObjectMetadata.getContentLength()).thenReturn((long) (Constants.MAX_DEFINITION_SIZE + 1));

        ProgressEvent<ResourceModel, CallbackContext> response
                = handler.handleRequest(proxy, request, null, logger);

        Mockito.verify(logger, Mockito.times(3)).log(argumentCaptor.capture());
        List<String> loggedStrings = argumentCaptor.getAllValues();
        String metricsString = loggedStrings.get(loggedStrings.size() - 1);

        assertThat(metricsString).contains(S3_DEFINITION_SIZE_LIMIT_EXCEEDED.loggingKey);
    }
}
