package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.DeleteStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineRequest;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class DeleteHandler extends ResourceHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO StateMachine DeleteHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();
        final CallbackContext currentContext = callbackContext == null ?
                CallbackContext.builder().build() :
                callbackContext;

        try {
            verifyStateMachineArnIsPresent(model.getArn());

            AWSStepFunctions sfnClient = ClientBuilder.getClient();

            if (!currentContext.isDeletionStarted()) {
                if (!doesStateMachineExist(model, proxy, sfnClient)) {
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .status(OperationStatus.SUCCESS)
                            .build();
                }

                deleteStateMachine(model, proxy, sfnClient);
                currentContext.setDeletionStarted(true);

                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .callbackContext(currentContext)
                        .resourceModel(model)
                        .status(OperationStatus.IN_PROGRESS)
                        .build();
            } else {
                if (doesStateMachineExist(model, proxy, sfnClient)) {
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .callbackContext(currentContext)
                            .resourceModel(model)
                            .status(OperationStatus.IN_PROGRESS)
                            .build();
                } else {
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .status(OperationStatus.SUCCESS)
                            .build();
                }
            }
        } catch (Exception e) {
            logger.log("ERROR Deleting StateMachine, caused by " + e.toString());

            return handleDefaultError(request, e, null);
        }
    }

    // Returns true if the state machine in the model exists, else false
    private boolean doesStateMachineExist(final ResourceModel model,
                                          final AmazonWebServicesClientProxy proxy,
                                          final AWSStepFunctions sfnClient) {
        DescribeStateMachineRequest describeStateMachineRequest = buildDescribeStateMachineRequestFromModel(model);
        try {
            proxy.injectCredentialsAndInvoke(describeStateMachineRequest, sfnClient::describeStateMachine);
            return true;
        } catch (Exception e) {
            if (isStateMachineNotFoundError(e)) {
                return false;
            }

            throw e;
        }
    }

    private DescribeStateMachineRequest buildDescribeStateMachineRequestFromModel(final ResourceModel model)  {
        DescribeStateMachineRequest describeStateMachineRequest = new DescribeStateMachineRequest();
        describeStateMachineRequest.setStateMachineArn(model.getArn());

        return describeStateMachineRequest;
    }

    // Returns true if the exception is a StateMachineDoesNotExist error, else false
    private boolean isStateMachineNotFoundError(final Exception e) {
        if (!(e instanceof AmazonServiceException)) {
            return false;
        }

        final AmazonServiceException amazonServiceException = (AmazonServiceException) e;

        final int errorStatus = amazonServiceException.getStatusCode();
        final String errorCode = amazonServiceException.getErrorCode();

        return errorStatus == 400 && Constants.STATE_MACHINE_DOES_NOT_EXIST_ERROR_CODE.equals(errorCode);
    }

    private void deleteStateMachine(final ResourceModel model,
                                    final AmazonWebServicesClientProxy proxy,
                                    final AWSStepFunctions sfnClient) {
        DeleteStateMachineRequest deleteStateMachineRequest = buildDeleteStateMachineRequestFromModel(model);
        proxy.injectCredentialsAndInvoke(deleteStateMachineRequest, sfnClient::deleteStateMachine);
    }

    private DeleteStateMachineRequest buildDeleteStateMachineRequestFromModel(final ResourceModel model) {
        final DeleteStateMachineRequest deleteStateMachineRequest = new DeleteStateMachineRequest();
        deleteStateMachineRequest.setStateMachineArn(model.getArn());

        return deleteStateMachineRequest;
    }

}
