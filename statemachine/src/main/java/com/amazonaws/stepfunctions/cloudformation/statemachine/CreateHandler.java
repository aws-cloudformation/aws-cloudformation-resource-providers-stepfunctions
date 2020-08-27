package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineResult;
import com.amazonaws.services.stepfunctions.model.Tag;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.List;

public class CreateHandler extends ResourceHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO StateMachine CreateHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        try {
            AWSStepFunctions sfnClient = ClientBuilder.getClient();

            List<Tag> tags = TaggingHelper.consolidateTags(request);

            // Auto-generate a state machine name if one is not provided in the template.
            if (model.getStateMachineName() == null) {
                model.setStateMachineName(IdentifierUtils.generateResourceIdentifier(
                    request.getLogicalResourceIdentifier(), request.getClientRequestToken(), Constants.STATE_MACHINE_NAME_MAXLEN)
                );
            }

            processDefinition(proxy, model);

            CreateStateMachineRequest createStateMachineRequest = new CreateStateMachineRequest();
            createStateMachineRequest.setRoleArn(model.getRoleArn());
            createStateMachineRequest.setName(model.getStateMachineName());
            createStateMachineRequest.setTags(tags);
            createStateMachineRequest.setDefinition(model.getDefinitionString());
            createStateMachineRequest.setType(model.getStateMachineType());

            if (model.getLoggingConfiguration() != null) {
                createStateMachineRequest.setLoggingConfiguration(Translator.getLoggingConfiguration(model.getLoggingConfiguration()));
            }

            if (model.getTracingConfiguration() != null) {
                createStateMachineRequest.setTracingConfiguration(Translator.getTracingConfiguration(model.getTracingConfiguration()));
            }

            CreateStateMachineResult createStateMachineResult = proxy.injectCredentialsAndInvoke(createStateMachineRequest, sfnClient::createStateMachine);
            model.setArn(createStateMachineResult.getStateMachineArn());
            model.setName(model.getStateMachineName());

            return ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();
        } catch (Exception e) {
            logger.log("ERROR Creating StateMachine, caused by " + e.toString());
            return handleDefaultError(request, e);
        }
    }

}