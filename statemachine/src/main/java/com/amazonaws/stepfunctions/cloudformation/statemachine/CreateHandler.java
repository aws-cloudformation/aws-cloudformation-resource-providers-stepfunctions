package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineResult;
import com.amazonaws.services.stepfunctions.model.Tag;
import software.amazon.cloudformation.exceptions.CfnInvalidRequestException;
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
                    request.getLogicalResourceIdentifier(), request.getClientRequestToken())
                );
            }

            // Fetch S3 definition and apply resource mappings.
            if (model.getDefinitionS3Location() == null && model.getDefinitionString() == null) {
                throw new CfnInvalidRequestException(Constants.DEFINITION_MISSING_ERROR_MESSAGE);
            }

            if (model.getDefinitionS3Location() != null) {
                model.setDefinitionString(fetchS3Definition(model.getDefinitionS3Location(), proxy));
            }

            if (model.getDefinitionSubstitutions() != null) {
                model.setDefinitionString(transformDefinition(model.getDefinitionString(), model.getDefinitionSubstitutions()));
            }

            CreateStateMachineRequest createStateMachineRequest = new CreateStateMachineRequest();
            createStateMachineRequest.setRoleArn(model.getRoleArn());
            createStateMachineRequest.setName(model.getStateMachineName());
            createStateMachineRequest.setTags(tags);
            createStateMachineRequest.setDefinition(model.getDefinitionString());
            createStateMachineRequest.setType(model.getStateMachineType());

            if (model.getLoggingConfiguration() != null) {
                createStateMachineRequest.setLoggingConfiguration(Translator.getLoggingConfiguration(model.getLoggingConfiguration()));
            }

            CreateStateMachineResult createStateMachineResult = proxy.injectCredentialsAndInvoke(createStateMachineRequest, sfnClient::createStateMachine);
            model.setId(createStateMachineResult.getStateMachineArn());

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