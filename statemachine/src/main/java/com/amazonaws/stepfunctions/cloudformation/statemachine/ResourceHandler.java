package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.AmazonServiceException;
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
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
import software.amazon.cloudformation.exceptions.TerminalException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ResourceHandler extends BaseHandler<CallbackContext> {

    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    static {
        jsonMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        yamlMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    /**
     * Generic strategy to handle errors.
     * https://w.amazon.com/bin/view/AWS21/Design/Uluru/HandlerContract/
     */
    protected ProgressEvent<ResourceModel, CallbackContext> handleDefaultError(ResourceHandlerRequest<ResourceModel> request, Exception e) {
        ProgressEvent.ProgressEventBuilder<ResourceModel, CallbackContext> resultBuilder = ProgressEvent.<ResourceModel, CallbackContext>builder();

        if (e instanceof AmazonServiceException) {
            AmazonServiceException amznException = (AmazonServiceException) e;
            int errorStatus = amznException.getStatusCode();
            if (errorStatus >= 400 && errorStatus < 500) {
                final String errorCode = amznException.getErrorCode();
                if (Constants.THROTTLING_ERROR_CODE.equals(errorCode)) {
                    // throttling exception
                    resultBuilder.errorCode(HandlerErrorCode.Throttling);
                    resultBuilder.message(amznException.getMessage());
                    resultBuilder.status(OperationStatus.FAILED);
                } else if (Constants.ACCESS_DENIED_ERROR_CODE.equals(errorCode)) {
                    resultBuilder.errorCode(HandlerErrorCode.AccessDenied);
                    resultBuilder.message(amznException.getMessage());
                    resultBuilder.status(OperationStatus.FAILED);
                } else if (Constants.STATE_MACHINE_DOES_NOT_EXIST_ERROR_CODE.equals(errorCode)) {
                    resultBuilder.errorCode(HandlerErrorCode.NotFound);
                    resultBuilder.message(amznException.getMessage());
                    resultBuilder.status(OperationStatus.FAILED);
                } else if (Constants.INVALID_REQUESTS_ERROR_CODES.contains(errorCode)) {
                    resultBuilder.errorCode(HandlerErrorCode.InvalidRequest);
                    resultBuilder.message(amznException.getMessage());
                    resultBuilder.status(OperationStatus.FAILED);
                } else {
                    // 400s except for throttle default to FAILURE
                    resultBuilder.errorCode(HandlerErrorCode.GeneralServiceException);
                    resultBuilder.message(Constants.INTERNAL_FAILURE_MESSAGE);
                    resultBuilder.status(OperationStatus.FAILED);
                }
            } else {
                // 500s default to FAILED but Retriable (ErrorCode = ServiceInternalError)
                resultBuilder.errorCode(HandlerErrorCode.ServiceInternalError);
                resultBuilder.message(Constants.INTERNAL_FAILURE_MESSAGE);
                resultBuilder.status(OperationStatus.FAILED);
            }
            resultBuilder.message(e.getMessage());
        } else if (e instanceof TerminalException) {
            resultBuilder.errorCode(HandlerErrorCode.InternalFailure);
            resultBuilder.message(e.getMessage());
            resultBuilder.status(OperationStatus.FAILED);
        } else if (e instanceof BaseHandlerException) {
            resultBuilder.errorCode(((BaseHandlerException) e).getErrorCode());
            resultBuilder.message(e.getMessage());
            resultBuilder.status(OperationStatus.FAILED);
        } else {
            // Unexpected exceptions default to be InternalFailure
            resultBuilder.errorCode(HandlerErrorCode.InternalFailure);
            resultBuilder.message(Constants.INTERNAL_FAILURE_MESSAGE);
            resultBuilder.status(OperationStatus.FAILED);
        }

        return resultBuilder.build();
    }

    protected String transformDefinition(String definitionString, Map<String, String> resourceMappings) {
        List<String> searchList = new ArrayList<>();
        List<String> replacementList = new ArrayList<>();
        for (Map.Entry<String, String> e : resourceMappings.entrySet()) {
            searchList.add("${" + e.getKey() + "}");
            replacementList.add(e.getValue());
        }
        return StringUtils.replaceEachRepeatedly(definitionString, searchList.toArray(new String[0]), replacementList.toArray(new String[0]));
    }

    protected String convertDefinitionObject(Map<String, Object> definitionObject) {
        try {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(definitionObject);
        } catch (JsonProcessingException e) {
            throw new CfnInvalidRequestException("Invalid JSON: " + e.getMessage());
        }
    }

    protected String fetchS3Definition(S3Location s3Location, AmazonWebServicesClientProxy proxy) {
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
        } catch (IOException jsonException) {
            try {
                JsonNode root = yamlMapper.readTree(definition);
                definition = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            } catch (IOException yamlException) {
                throw new TerminalException(Constants.DEFINITION_INVALID_FORMAT_ERROR_MESSAGE);
            }
        }

        return definition;
    }

    protected void processDefinition(AmazonWebServicesClientProxy proxy, ResourceModel model) {
        // Validate that only one Definition is present
        List<Object> definitions = new ArrayList<>();

        if (model.getDefinitionString() != null) {
            definitions.add(model.getDefinitionString());
        }

        if (model.getDefinitionS3Location() != null) {
            definitions.add(model.getDefinitionS3Location());
        }

        if (definitions.isEmpty()) {
            throw new TerminalException(Constants.DEFINITION_MISSING_ERROR_MESSAGE);
        }

        if (definitions.size() > 1) {
            throw new TerminalException(Constants.DEFINITION_REDUNDANT_ERROR_MESSAGE);
        }

        if (model.getDefinitionS3Location() != null) {
            model.setDefinitionString(fetchS3Definition(model.getDefinitionS3Location(), proxy));
        }

        if (model.getDefinitionSubstitutions() != null) {
            model.setDefinitionString(transformDefinition(model.getDefinitionString(), model.getDefinitionSubstitutions()));
        }
    }

}
