package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.ListStateMachineAliasesRequest;
import com.amazonaws.services.stepfunctions.model.ListStateMachineAliasesResult;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

public class ListHandler extends ResourceHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO StateMachineAlias ListHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();
        try {
            final AWSStepFunctions sfnClient = ClientBuilder.getSfnClient();

            final ListStateMachineAliasesRequest listStateMachineAliasesRequest = Translator.translateToListRequest(
                    model.getRoutingConfiguration().stream().findFirst().get().getStateMachineVersionArn(),
                    request.getNextToken()
            );

            final ListStateMachineAliasesResult listStateMachineAliasesResult = proxy.injectCredentialsAndInvoke(
                    listStateMachineAliasesRequest,
                    sfnClient::listStateMachineAliases
            );

            final List<ResourceModel> resourceModels = Translator.translateFromListResult(listStateMachineAliasesResult);
            final String nextTokenToReturn = listStateMachineAliasesResult.getNextToken();

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .resourceModels(resourceModels)
                            .nextToken(nextTokenToReturn)
                            .status(OperationStatus.SUCCESS)
                            .build();
        } catch (final Exception e) {
            logger.log("ERROR listing state machines aliases, caused by " + e);

            return handleDefaultError(e);
        }
    }
}
