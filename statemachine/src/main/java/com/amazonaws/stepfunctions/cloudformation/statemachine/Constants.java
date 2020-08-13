package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.google.common.collect.Sets;

import java.util.Set;

public class Constants {
    public static final String THROTTLING_ERROR_CODE = "ThrottlingException";
    public static final String ACCESS_DENIED_ERROR_CODE = "AccessDeniedException";
    public static final String INTERNAL_FAILURE_MESSAGE = "Internal Failure";
    public static final int MAX_ERROR_RETRIES = 10;
    public static final int STATE_MACHINE_NAME_MAXLEN = 80;
    public static final String MANAGED_RULE_EXCEPTION_MESSAGE_SUBSTRING = "managed-rule";
    public static final String STS_AUTHORIZED_TO_ASSUME_MESSAGE_SUBSTRING =
            "Neither the global service principal states.amazonaws.com, nor the regional one is authorized to assume the provided role";
    public static final Integer MAX_DEFINITION_SIZE = 1048576;
    public static final String DEFINITION_SIZE_LIMIT_ERROR_MESSAGE = "State Machine definition file cannot exceed 1MB.";
    public static final String DEFINITION_INVALID_FORMAT_ERROR_MESSAGE = "Invalid StateMachine definition file.";
    public static final String DEFINITION_MISSING_ERROR_MESSAGE = "Property validation failed. Required key [DefinitionS3Location] or [DefinitionString] not found.";
    public static final String DEFINITION_REDUNDANT_ERROR_MESSAGE = "Property validation failed. Please use either [DefinitionS3Location] or [DefinitionString] but not both.";
    public static final String STATE_MACHINE_DOES_NOT_EXIST_ERROR_CODE = "StateMachineDoesNotExist";
    public static final Set<String> INVALID_REQUESTS_ERROR_CODES = Sets.newHashSet("InvalidArn", "InvalidDefinition", "InvalidLoggingConfiguration", "InvalidName");
}
