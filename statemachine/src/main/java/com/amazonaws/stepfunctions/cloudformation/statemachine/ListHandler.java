package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.ListStateMachinesRequest;
import com.amazonaws.services.stepfunctions.model.ListStateMachinesResult;
import com.amazonaws.services.stepfunctions.model.StateMachineListItem;

import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.ArrayList;
import java.util.List;

public class ListHandler extends ResourceHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO StateMachine ListHandler with clientRequestToken: " + request.getClientRequestToken());
        final MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.LIST);

        try {
            final AWSStepFunctions sfnClient = ClientBuilder.getClient();
            final List<ResourceModel> models = new ArrayList<>();
            final String nextTokenProvided = request.getNextToken();

            final ListStateMachinesRequest listStateMachinesRequest = new ListStateMachinesRequest();
            listStateMachinesRequest.setNextToken(nextTokenProvided);

            final ListStateMachinesResult listStateMachinesResult = proxy.injectCredentialsAndInvoke(
                    listStateMachinesRequest,
                    sfnClient::listStateMachines
            );

            listStateMachinesResult.getStateMachines()
                    .stream()
                    .map(this::buildResourceModelFromStateMachineListItem)
                    .forEach(models::add);

            final String nextTokenToReturn = listStateMachinesResult.getNextToken();

            final ProgressEvent<ResourceModel, CallbackContext> progressEvent =
                    ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .resourceModels(models)
                            .nextToken(nextTokenToReturn)
                            .status(OperationStatus.SUCCESS)
                            .build();

            metricsRecorder.setOperationSuccessful(true);

            return progressEvent;
        } catch (final Exception e) {
            logger.log("ERROR listing state machines, caused by " + e);

            return handleDefaultError(request, e, metricsRecorder);
        } finally {
            logger.log(metricsRecorder.generateMetricsString());
        }
    }

    private ResourceModel buildResourceModelFromStateMachineListItem(final StateMachineListItem stateMachineListItem) {
        return ResourceModel.builder()
                .arn(stateMachineListItem.getStateMachineArn())
                .stateMachineName(stateMachineListItem.getName())
                .stateMachineType(stateMachineListItem.getType())
                .build();
    }
}
