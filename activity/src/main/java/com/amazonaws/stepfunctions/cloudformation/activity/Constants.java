package com.amazonaws.stepfunctions.cloudformation.activity;

import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.Set;

public class Constants {
    public static final String THROTTLING_ERROR_CODE = "ThrottlingException";
    public static final String ACCESS_DENIED_ERROR_CODE = "AccessDeniedException";
    public static final String INTERNAL_FAILURE_MESSAGE = "Internal Failure";
    public static final String ACTIVITY_ARN_NOT_FOUND_MESSAGE = "ActivityArnNotFound";
    public static final String ACTIVITY_DOES_NOT_EXIST_ERROR_CODE = "ActivityDoesNotExist";
    public static final String RESOURCE_NOT_FOUND_ERROR_CODE = "ResourceNotFound";
    public static final String INVALID_TOKEN = "InvalidToken";

    public static final Set<String> RESOURCE_NOT_FOUND_ERROR_CODES = Collections.unmodifiableSet(ImmutableSet.of(
            RESOURCE_NOT_FOUND_ERROR_CODE,
            ACTIVITY_DOES_NOT_EXIST_ERROR_CODE
    ));

    // Setting 60 seconds stabilization delay to account for eventual consistency of DescribeActivity call due to caching
    public static final int CALLBACK_DELAY_SECONDS_FOR_STABILIZATION = 60;
}
