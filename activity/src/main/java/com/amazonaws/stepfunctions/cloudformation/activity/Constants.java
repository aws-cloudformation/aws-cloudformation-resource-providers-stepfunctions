package com.amazonaws.stepfunctions.cloudformation.activity;

import com.google.common.collect.Sets;

import java.util.Set;

public class Constants {
    public static final String THROTTLING_ERROR_CODE = "ThrottlingException";
    public static final String ACCESS_DENIED_ERROR_CODE = "AccessDeniedException";
    public static final String INTERNAL_FAILURE_MESSAGE = "Internal Failure";
    public static final String ACTIVITY_DOES_NOT_EXIST_ERROR_CODE = "ActivityDoesNotExist";
    public static final String RESOURCE_DOES_NOT_EXIST_ERROR_CODE = "ResourceNotFound";
    public static final String INVALID_ARN_ERROR_CODE = "InvalidArn";

    public static final Set<String> RESOURCE_NOT_FOUND_ERROR_CODES = Sets.newHashSet(
            RESOURCE_DOES_NOT_EXIST_ERROR_CODE,
            ACTIVITY_DOES_NOT_EXIST_ERROR_CODE,
            INVALID_ARN_ERROR_CODE
    );
}