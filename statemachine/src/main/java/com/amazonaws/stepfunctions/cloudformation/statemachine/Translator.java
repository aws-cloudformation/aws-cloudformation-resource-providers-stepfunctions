package com.amazonaws.stepfunctions.cloudformation.statemachine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.amazonaws.services.stepfunctions.model.LogDestination;
import com.amazonaws.services.stepfunctions.model.CloudWatchLogsLogGroup;

public class Translator {

    public static com.amazonaws.services.stepfunctions.model.LoggingConfiguration getLoggingConfiguration(LoggingConfiguration input) {
        com.amazonaws.services.stepfunctions.model.LoggingConfiguration result = new com.amazonaws.services.stepfunctions.model.LoggingConfiguration();
        result.setLevel(input.getLevel());
        result.setIncludeExecutionData(input.getIncludeExecutionData());

        if (input.getDestinations() != null) {
            List<String> logGroups = input.getDestinations().stream()
                    .map(com.amazonaws.stepfunctions.cloudformation.statemachine.LogDestination::getCloudWatchLogsLogGroup)
                    .map(com.amazonaws.stepfunctions.cloudformation.statemachine.CloudWatchLogsLogGroup::getLogGroupArn)
                    .collect(Collectors.toList());
            List<LogDestination> destinations = new ArrayList<>();
            for (String logGroup : logGroups) {
                destinations.add(new LogDestination()
                    .withCloudWatchLogsLogGroup(new CloudWatchLogsLogGroup()
                            .withLogGroupArn(logGroup))
                );
            }
            result.setDestinations(destinations);
        }

        return result;
    }

    public static com.amazonaws.services.stepfunctions.model.TracingConfiguration getTracingConfiguration(TracingConfiguration input) {

        com.amazonaws.services.stepfunctions.model.TracingConfiguration result = new com.amazonaws.services.stepfunctions.model.TracingConfiguration();
        result.setEnabled(input.getEnabled());

        return result;
    }
}
