package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.stepfunctions.cloudformation.statemachine.s3.GetObjectFunction;
import com.amazonaws.stepfunctions.cloudformation.statemachine.s3.GetObjectResult;
import org.apache.commons.lang3.StringUtils;
import software.amazon.cloudformation.exceptions.BaseHandlerException;
import software.amazon.cloudformation.exceptions.CfnInternalFailureException;
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

    /**
     * Generic strategy to handle errors.
     * https://w.amazon.com/bin/view/AWS21/Design/Uluru/HandlerContract/
     */
    protected ProgressEvent<ResourceModel, CallbackContext> handleDefaultError(ResourceHandlerRequest<ResourceModel> request, Exception e) {
        ProgressEvent.ProgressEventBuilder<ResourceModel, CallbackContext> resultBuilder = ProgressEvent.<ResourceModel, CallbackContext>builder()
                .resourceModel(request.getDesiredResourceState());

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

    protected String fetchS3Definition(S3Location definitionS3, AmazonWebServicesClientProxy proxy) {
        AmazonS3 s3Client = ClientBuilder.getS3Client();
        GetObjectRequest getObjectRequest = new GetObjectRequest(definitionS3.getBucket(), definitionS3.getKey());
        if (definitionS3.getVersion() != null && !definitionS3.getVersion().isEmpty()) {
            getObjectRequest.setVersionId(definitionS3.getVersion());
        }

        GetObjectResult getObjectResult = proxy.injectCredentialsAndInvoke(getObjectRequest, new GetObjectFunction(s3Client)::get);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getObjectResult.getS3Object().getObjectContent()))) {
            return reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new CfnInternalFailureException(e);
        }
    }

}
