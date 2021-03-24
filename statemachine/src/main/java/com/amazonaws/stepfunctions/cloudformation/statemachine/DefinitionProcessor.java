package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.stepfunctions.cloudformation.statemachine.s3.GetObjectFunction;
import com.amazonaws.stepfunctions.cloudformation.statemachine.s3.GetObjectResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.StringUtils;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.TerminalException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Encapsulates the logic behind generating the final state machine definition string
 */
public class DefinitionProcessor {

    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    static {
        jsonMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        yamlMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    /**
     * Validates that the resource model contains exactly one of DefinitionString, Definition, and DefinitionS3Location
     * @param model The resource model representing the ResourceHandlerRequest's desiredResourceState
     * @throws TerminalException Thrown if the number of definitions in the model does not equal 1.
     */
    public static void validateDefinitionCount(final ResourceModel model) throws TerminalException {
        final int numDefinitionsInModel = getNumDefinitionsInModel(model);

        if (numDefinitionsInModel == 0) {
            throw new TerminalException(Constants.DEFINITION_MISSING_ERROR_MESSAGE);
        }

        if (numDefinitionsInModel > 1) {
            throw new TerminalException(Constants.DEFINITION_REDUNDANT_ERROR_MESSAGE);
        }
    }

    private static int getNumDefinitionsInModel(final ResourceModel model) {
        int definitionsCount = 0;

        if (model.getDefinitionString() != null) {
            definitionsCount++;
        }

        if (model.getDefinitionS3Location() != null) {
            definitionsCount++;
        }

        if (model.getDefinition() != null) {
            definitionsCount++;
        }

        return definitionsCount;
    }

    /**
     * Sets definitionString of the resource model to be the value of the definition in string format with
     *   any definition substitutions applied.
     * @param proxy The AmazonWebsServicesClientProxy used to retrieve the definition from S3 if DefinitionS3Location is provided
     * @param model The resource model representing the ResourceHandlerRequest's desiredResourceState
     * @param metricsRecorder The MetricsRecorder object used for collecting anonymous property usage metrics
     */
    public static void processDefinition(final AmazonWebServicesClientProxy proxy, final ResourceModel model, final MetricsRecorder metricsRecorder) {
        if (model.getDefinitionS3Location() != null) {
            model.setDefinitionString(fetchS3Definition(model.getDefinitionS3Location(), proxy, metricsRecorder));
        }

        if (model.getDefinition() != null) {
            model.setDefinitionString(convertDefinitionObjectToString(model.getDefinition()));
        }

        if (model.getDefinitionSubstitutions() != null) {
            model.setDefinitionString(transformDefinition(model.getDefinitionString(), model.getDefinitionSubstitutions()));
        }
    }

    private static String fetchS3Definition(final S3Location s3Location, final AmazonWebServicesClientProxy proxy, final MetricsRecorder metricsRecorder) {
        AmazonS3 s3Client = ClientBuilder.getS3Client();
        GetObjectRequest getObjectRequest = new GetObjectRequest(s3Location.getBucket(), s3Location.getKey());
        if (s3Location.getVersion() != null && !s3Location.getVersion().isEmpty()) {
            getObjectRequest.setVersionId(s3Location.getVersion());
        }

        GetObjectResult getObjectResult = proxy.injectCredentialsAndInvoke(getObjectRequest, new GetObjectFunction(s3Client)::get);
        if (getObjectResult.getS3Object().getObjectMetadata().getContentLength() > Constants.MAX_DEFINITION_SIZE) {
            throw new CfnInvalidRequestException(Constants.DEFINITION_SIZE_LIMIT_ERROR_MESSAGE);
        }

        String definition;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getObjectResult.getS3Object().getObjectContent()))) {
            definition = reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new CfnInternalFailureException(e);
        }

        // Parse JSON format first, then YAML.
        try {
            jsonMapper.readTree(definition);
            metricsRecorder.setS3DefinitionJson(true);
        } catch (IOException jsonException) {
            try {
                JsonNode root = yamlMapper.readTree(definition);
                definition = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
                metricsRecorder.setS3DefinitionYaml(true);
            } catch (IOException yamlException) {
                throw new TerminalException(Constants.DEFINITION_INVALID_FORMAT_ERROR_MESSAGE);
            }
        }

        return definition;
    }

    private static String convertDefinitionObjectToString(final Map<String, Object> definitionObject) {
        try {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(definitionObject);
        } catch (JsonProcessingException e) {
            throw new TerminalException(Constants.DEFINITION_INVALID_FORMAT_ERROR_MESSAGE);
        }
    }

    private static String transformDefinition(final String definitionString, final Map<String, String> resourceMappings) {
        List<String> searchList = new ArrayList<>();
        List<String> replacementList = new ArrayList<>();
        for (Map.Entry<String, String> e : resourceMappings.entrySet()) {
            searchList.add("${" + e.getKey() + "}");
            replacementList.add(e.getValue());
        }
        return StringUtils.replaceEachRepeatedly(definitionString, searchList.toArray(new String[0]), replacementList.toArray(new String[0]));
    }

}
