package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.AmazonServiceException;
import software.amazon.cloudformation.exceptions.TerminalException;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public abstract class ResourceHandler extends BaseHandler<CallbackContext> {

    /**
     * Generic strategy to handle errors.
     * https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test-contract.html
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
                } else if (Constants.RESOURCE_NOT_FOUND_ERROR_CODES.contains(errorCode)) {
                    resultBuilder.errorCode(HandlerErrorCode.NotFound);
                    resultBuilder.message(amznException.getMessage());
                    resultBuilder.status(OperationStatus.FAILED);
                } else if (Constants.INVALID_TOKEN.equals(errorCode)) {
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
        } else {
            // Unexpected exceptions default to be InternalFailure
            resultBuilder.errorCode(HandlerErrorCode.InternalFailure);
            resultBuilder.message(Constants.INTERNAL_FAILURE_MESSAGE);
            resultBuilder.status(OperationStatus.FAILED);
        }

        return resultBuilder.build();
    }

    /**
     * Validates that the activity ARN is not null
     * @param resourceArn The resource ARN for the activity
     * @throws AmazonServiceException Thrown if the activity's ARN is null
     */
    protected void verifyActivityArnIsPresent(String resourceArn) throws AmazonServiceException {
        if (resourceArn == null) {
            AmazonServiceException exception = new AmazonServiceException(Constants.ACTIVITY_ARN_NOT_FOUND_MESSAGE);
            exception.setStatusCode(400);
            exception.setErrorCode(Constants.ACTIVITY_DOES_NOT_EXIST_ERROR_CODE);
            throw exception;
        }
    }

}
