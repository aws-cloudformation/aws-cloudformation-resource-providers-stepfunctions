package com.amazonaws.stepfunctions.cloudformation.statemachine;

public class Constants {
    public static final String THROTTLING_ERROR_CODE = "ThrottlingException";
    public static final String ACCESS_DENIED_ERROR_CODE = "AccessDeniedException";
    public static final String INTERNAL_FAILURE_MESSAGE = "Internal Failure";
    public static final int MAX_ERROR_RETRIES = 10;
    public static final String MANAGED_RULE_EXCEPTION_MESSAGE_SUBSTRING = "managed-rule";
    public static final String STS_AUTHORIZED_TO_ASSUME_MESSAGE_SUBSTRING =
            "Neither the global service principal states.amazonaws.com, nor the regional one is authorized to assume the provided role";
}
