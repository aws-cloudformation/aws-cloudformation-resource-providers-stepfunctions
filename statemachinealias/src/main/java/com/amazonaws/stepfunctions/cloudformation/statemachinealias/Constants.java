package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.Set;

public class Constants {
    // Client configuration
    public static final int MAX_ERROR_RETRIES = 10;
    public static final int STATE_MACHINE_ALIAS_NAME_MAXLEN = 80;
    public static final int MAX_DEPLOYMENT_TIME_MINUTES = 2100;
    public static final int GRADUAL_DEPLOYMENT_HANDLER_DELAY_SECONDS = 60;

    // Error codes
    public static final String THROTTLING_ERROR_CODE = "ThrottlingException";
    public static final String CONFLICT_EXCEPTION_ERROR_CODE = "ConflictException";
    public static final String ACCESS_DENIED_ERROR_CODE = "AccessDeniedException";
    public static final String INVALID_ARN_ERROR_CODE = "InvalidArn";
    public static final String INVALID_NAME_ERROR_CODE = "InvalidName";
    public static final String INVALID_TOKEN_ERROR_CODE = "InvalidToken";
    public static final String VALIDATION_ERROR_CODE = "ValidationException";
    public static final String RESOURCE_NOT_FOUND_ERROR_CODE = "ResourceNotFound";
    public static final String STATE_MACHINE_ALIAS_ALREADY_EXISTS = "StateMachineAliasAlreadyExists";
    public static final Set<String> INVALID_REQUEST_ERROR_CODES = Collections.unmodifiableSet(ImmutableSet.of(
            INVALID_ARN_ERROR_CODE, INVALID_NAME_ERROR_CODE, INVALID_TOKEN_ERROR_CODE, VALIDATION_ERROR_CODE
    ));

    // Error messages
    public static final String STATE_MACHINE_DOES_NOT_EXIST_ERROR_CODE = "StateMachineDoesNotExist";
    public static final String INTERNAL_FAILURE_MESSAGE = "Internal Failure";
}
