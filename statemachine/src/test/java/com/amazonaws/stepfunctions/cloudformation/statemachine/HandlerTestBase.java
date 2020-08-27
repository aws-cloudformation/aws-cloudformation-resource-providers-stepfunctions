package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.AWSStepFunctionsException;
import org.assertj.core.util.Lists;
import org.mockito.Mock;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;

import java.util.ArrayList;

public abstract class HandlerTestBase {

    protected static final String LOGICAL_RESOURCE_ID = "randomName";
    protected static final String GUID = "AAAABBBBCCCC";
    protected static final String EXPECTED_NAME = LOGICAL_RESOURCE_ID + "-" + GUID;
    protected static final String DEFINITION = "randomDefinition";
    protected static final String ROLE_ARN = "arn:aws:iam::123456789012:role/service-role/StatesExecutionRole-us-east-1";
    protected static final String STANDARD_STATE_MACHINE_TYPE = "STANDARD";
    protected static final String EXPRESS_STATE_MACHINE_TYPE = "EXPRESS";
    protected static final String LOGGING_LEVEL = "ALL";
    protected static final Boolean LOGGING_INCLUDE_EXECUTION_DATA = true;
    protected static final String LOGGING_CLOUDWATCHLOGS_LOGARN = "log-group-arn";
    protected static final Boolean TRACING_CONFIGURATION_ENABLED = false;

    protected static LoggingConfiguration createLoggingConfiguration() {
        LoggingConfiguration loggingConfiguration = new LoggingConfiguration();
        loggingConfiguration.setLevel(LOGGING_LEVEL);
        loggingConfiguration.setIncludeExecutionData(LOGGING_INCLUDE_EXECUTION_DATA);
        loggingConfiguration.setDestinations(Lists.newArrayList(
                new LogDestination(new CloudWatchLogsLogGroup(LOGGING_CLOUDWATCHLOGS_LOGARN))
        ));

        return loggingConfiguration;
    }

    protected static TracingConfiguration createTracingConfiguration() {
        TracingConfiguration tracingConfiguration = new TracingConfiguration();
        tracingConfiguration.setEnabled(TRACING_CONFIGURATION_ENABLED);

        return tracingConfiguration;
    }

    protected final static AmazonServiceException exception500 = new AmazonServiceException("Server error");
    protected final static AmazonServiceException exception400 = new AmazonServiceException("Client error");
    protected final static RuntimeException unknownException = new RuntimeException("Runtime error");
    protected final static AmazonServiceException throttlingException = new AmazonServiceException("Your request has been throttled");
    protected final static AWSStepFunctionsException iamManagedRuleException = new AWSStepFunctionsException(
            "arn:aws:iam::000000000000:role/role' is not authorized to create managed-rule.");

    protected final static String AWS_ACCOUNT_ID = "1234567890";
    protected final static String REGION = "us-east-1";
    protected final static String STATE_MACHINE_NAME = "TestStateMachine";
    protected final static String STATE_MACHINE_ARN = "arn:aws:states:us-east-1:1234567890:stateMachine:TestStateMachine";

    static {
        exception400.setStatusCode(400);
        exception500.setStatusCode(500);
        throttlingException.setStatusCode(400);
        throttlingException.setErrorCode("ThrottlingException");
        iamManagedRuleException.setErrorCode("AccessDeniedException");
        iamManagedRuleException.setStatusCode(400);
    }

    @Mock
    protected AmazonWebServicesClientProxy proxy;

    @Mock
    protected AWSStepFunctions client;

    @Mock
    protected Logger logger;

}
