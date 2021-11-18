package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.services.stepfunctions.model.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Encapsulates the logic behind converting between CloudFormation resource
 * model properties and Step Functions model properties
 */
public class Translator {

    private Translator() {
    }

    public static com.amazonaws.services.stepfunctions.model.LoggingConfiguration getLoggingConfiguration(
            final LoggingConfiguration cfnLoggingConfiguration) {
        final com.amazonaws.services.stepfunctions.model.LoggingConfiguration sfnLoggingConfiguration =
                new com.amazonaws.services.stepfunctions.model.LoggingConfiguration();
        sfnLoggingConfiguration.setLevel(cfnLoggingConfiguration.getLevel());
        sfnLoggingConfiguration.setIncludeExecutionData(cfnLoggingConfiguration.getIncludeExecutionData());

        if (cfnLoggingConfiguration.getDestinations() != null) {
            final List<String> logGroups = cfnLoggingConfiguration.getDestinations().stream()
                    .map(LogDestination::getCloudWatchLogsLogGroup)
                    .map(CloudWatchLogsLogGroup::getLogGroupArn)
                    .collect(Collectors.toList());
            final List<com.amazonaws.services.stepfunctions.model.LogDestination> destinations = new ArrayList<>();
            for (final String logGroup : logGroups) {
                destinations.add(new com.amazonaws.services.stepfunctions.model.LogDestination()
                        .withCloudWatchLogsLogGroup(
                                new com.amazonaws.services.stepfunctions.model.CloudWatchLogsLogGroup()
                                        .withLogGroupArn(logGroup))
                );
            }
            sfnLoggingConfiguration.setDestinations(destinations);
        }

        return sfnLoggingConfiguration;
    }

    public static LoggingConfiguration getLoggingConfiguration(
            final com.amazonaws.services.stepfunctions.model.LoggingConfiguration sfnLoggingConfiguration) {
        final LoggingConfiguration cfnLoggingConfiguration = new LoggingConfiguration();

        cfnLoggingConfiguration.setLevel(sfnLoggingConfiguration.getLevel());
        cfnLoggingConfiguration.setIncludeExecutionData(sfnLoggingConfiguration.getIncludeExecutionData());

        if (sfnLoggingConfiguration.getDestinations() != null) {
            final List<LogDestination> destinations = sfnLoggingConfiguration.getDestinations().stream()
                    .map(com.amazonaws.services.stepfunctions.model.LogDestination::getCloudWatchLogsLogGroup)
                    .map(com.amazonaws.services.stepfunctions.model.CloudWatchLogsLogGroup::getLogGroupArn)
                    .map(logGroupArn -> new LogDestination(new CloudWatchLogsLogGroup(logGroupArn)))
                    .collect(Collectors.toList());

            cfnLoggingConfiguration.setDestinations(destinations);
        }

        return cfnLoggingConfiguration;
    }

    public static com.amazonaws.services.stepfunctions.model.TracingConfiguration getTracingConfiguration(
            final TracingConfiguration cfnTracingConfiguration) {
        final com.amazonaws.services.stepfunctions.model.TracingConfiguration sfnTracingConfiguration =
                new com.amazonaws.services.stepfunctions.model.TracingConfiguration();
        sfnTracingConfiguration.setEnabled(cfnTracingConfiguration.getEnabled());

        return sfnTracingConfiguration;
    }

    public static TracingConfiguration getTracingConfiguration(
            final com.amazonaws.services.stepfunctions.model.TracingConfiguration sfnTracingConfiguration) {
        final TracingConfiguration cfnTracingConfiguration = new TracingConfiguration();
        cfnTracingConfiguration.setEnabled(sfnTracingConfiguration.getEnabled());

        return cfnTracingConfiguration;
    }

    /**
     * Converts a list of Step Functions model tags to a list of CloudFormation
     * resource model tag entries
     */
    public static List<TagsEntry> getTagsEntries(final List<Tag> stateMachineTags) {
        return stateMachineTags.stream().map(e -> new TagsEntry(
                        e.getKey(),
                        e.getValue()))
                .collect(Collectors.toList());
    }
}
