package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineResult;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineRequest;
import com.amazonaws.services.stepfunctions.model.Tag;
import com.google.common.base.Preconditions;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

import static com.amazonaws.stepfunctions.cloudformation.statemachine.DefinitionProcessor.processDefinition;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.DefinitionProcessor.validateDefinitionCount;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.ResourceModelUtils.processStateMachineName;
import static com.amazonaws.stepfunctions.cloudformation.statemachine.ResourceModelUtils.updateModelFromResult;

public class CreateHandler extends ResourceHandler {

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO StateMachine CreateHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        MetricsRecorder metricsRecorder = new MetricsRecorder(HandlerOperationType.CREATE);
        metricsRecorder.setMetricsFromResourceModel(model);

        try {
            AWSStepFunctions sfnClient = ClientBuilder.getClient();

            List<Tag> tags = TaggingHelper.consolidateTags(request);

            processStateMachineName(request, model);
            validateDefinitionCount(model);
            processDefinition(proxy, model, metricsRecorder);

            failIfStateMachineAlreadyExists(request, sfnClient, proxy);

            CreateStateMachineRequest createStateMachineRequest = buildCreateStateMachineRequestFromModel(model, tags);

            CreateStateMachineResult createStateMachineResult = proxy.injectCredentialsAndInvoke(createStateMachineRequest, sfnClient::createStateMachine);

            updateModelFromResult(model, createStateMachineResult);
            // The model's name is only required if the handler operation is successful.
            model.setName(model.getStateMachineName());
            model.setStateMachineRevisionId(Constants.STATE_MACHINE_INITIAL_REVISION_ID);

            ProgressEvent<ResourceModel, CallbackContext> progressEvent = ProgressEvent.<ResourceModel, CallbackContext>builder()
                    .resourceModel(model)
                    .status(OperationStatus.SUCCESS)
                    .build();

            metricsRecorder.setOperationSuccessful(true);

            return progressEvent;
        } catch (Exception e) {
            logger.log("ERROR Creating StateMachine, caused by " + e.toString());

            return handleDefaultError(request, e, metricsRecorder);
        } finally {
            logger.log(metricsRecorder.generateMetricsString());
        }
    }

    private void failIfStateMachineAlreadyExists(final ResourceHandlerRequest<ResourceModel> request,
                                                 final AWSStepFunctions sfnClient,
                                                 final AmazonWebServicesClientProxy proxy) {
        Preconditions.checkNotNull(request.getAwsPartition(), "Request partition should not be null.");
        Preconditions.checkNotNull(request.getRegion(), "Request region should not be null.");
        Preconditions.checkNotNull(request.getAwsAccountId(), "Request accountId should not be null.");
        Preconditions.checkNotNull(request.getDesiredResourceState().getStateMachineName(), "Request state machine name should not be null.");

        // arn:<PARTITION>:states:<REGION>:<ACCOUNT_ID>:stateMachine:<STATE_MACHINE_NAME>
        final String stateMachineArn = String.format(
                "arn:%s:states:%s:%s:stateMachine:%s",
                request.getAwsPartition(),
                request.getRegion(),
                request.getAwsAccountId(),
                request.getDesiredResourceState().getStateMachineName());

        try {
            proxy.injectCredentialsAndInvoke(new DescribeStateMachineRequest().withStateMachineArn(stateMachineArn), sfnClient::describeStateMachine);

            // State machine already exists
            throw getStateMachineAlreadyExistsException();
        } catch (AmazonServiceException e) {
            // State machine does not exist
            if (Constants.STATE_MACHINE_DOES_NOT_EXIST_ERROR_CODE.equals(e.getErrorCode())) {
                return;
            }

            // For backwards compatibility, do not fail the request if DescribeStateMachine permissions are not present
            if (Constants.ACCESS_DENIED_ERROR_CODE.equals(e.getErrorCode())) {
                return;
            }

            throw e;
        }
    }

    private CreateStateMachineRequest buildCreateStateMachineRequestFromModel(ResourceModel model, List<Tag> tags) {
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

        return createStateMachineRequest;
    }
}
