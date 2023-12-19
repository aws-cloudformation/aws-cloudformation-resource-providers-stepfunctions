package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.model.CreateStateMachineResult;
import com.amazonaws.services.stepfunctions.model.DescribeStateMachineResult;
import com.amazonaws.services.stepfunctions.model.LoggingConfiguration;
import com.amazonaws.services.stepfunctions.model.Tag;
import com.amazonaws.services.stepfunctions.model.TracingConfiguration;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.resource.IdentifierUtils;

import java.util.List;

/**
 * Encapsulates the logic behind transforming the ResourceModel for the request's desiredResourceState
 */
public class ResourceModelUtils {

    private ResourceModelUtils() {
    }

    // Auto-generate a state machine name if one is not provided in the template.
    public static void processStateMachineName(final ResourceHandlerRequest<ResourceModel> request,
                                               final ResourceModel model) {
        if (model.getStateMachineName() == null) {
            final String generatedName = IdentifierUtils.generateResourceIdentifier(
                    request.getLogicalResourceIdentifier(), request.getClientRequestToken(),
                    Constants.STATE_MACHINE_NAME_MAXLEN);

            model.setStateMachineName(generatedName);
        }
    }

    public static void updateModelFromResult(final ResourceModel model, final CreateStateMachineResult result) {
        model.setArn(result.getStateMachineArn());
    }

    /**
     * Generates a resource model containing the state machine resource's properties
     *
     * @param describeStateMachineResult The result of calling DescribeStateMachine for the state machine
     * @param stateMachineTags           A list of tags associated with the state machine
     * @return A resource model containing the state machine resource's properties
     */
    public static ResourceModel getUpdatedResourceModelFromReadResults(
            final DescribeStateMachineResult describeStateMachineResult,
            final List<Tag> stateMachineTags) {
        final ResourceModel model = ResourceModel.builder()
                .arn(describeStateMachineResult.getStateMachineArn())
                .name(describeStateMachineResult.getName())
                .definitionString(describeStateMachineResult.getDefinition())
                .roleArn(describeStateMachineResult.getRoleArn())
                .stateMachineName(describeStateMachineResult.getName())
                .stateMachineType(describeStateMachineResult.getType())
                .stateMachineRevisionId(describeStateMachineResult.getRevisionId() != null ? describeStateMachineResult.getRevisionId() : Constants.STATE_MACHINE_INITIAL_REVISION_ID)
                .build();

        final LoggingConfiguration loggingConfiguration = describeStateMachineResult.getLoggingConfiguration();
        if (loggingConfiguration != null) {
            model.setLoggingConfiguration(Translator.getLoggingConfiguration(loggingConfiguration));
        }

        final TracingConfiguration tracingConfiguration = describeStateMachineResult.getTracingConfiguration();
        if (tracingConfiguration != null) {
            model.setTracingConfiguration(Translator.getTracingConfiguration(tracingConfiguration));
        }

        if (stateMachineTags != null) {
            model.setTags(Translator.getTagsEntries(stateMachineTags));
        }

        return model;
    }

}
