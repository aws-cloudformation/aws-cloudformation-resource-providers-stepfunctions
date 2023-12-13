package com.amazonaws.stepfunctions.cloudformation.statemachineversion;

public class Constants {
	public static final String THROTTLING_ERROR_CODE = "ThrottlingException";
	public static final String CONFLICT_EXCEPTION_ERROR_CODE = "ConflictException";
	public static final String ACCESS_DENIED_ERROR_CODE = "AccessDeniedException";
	public static final String INTERNAL_FAILURE_MESSAGE = "Internal Failure";
	public static final int MAX_ERROR_RETRIES = 10;
	public static final String MANAGED_RULE_EXCEPTION_MESSAGE_SUBSTRING = "managed-rule";
	public static final String STS_AUTHORIZED_TO_ASSUME_MESSAGE_SUBSTRING =
			"Neither the global service principal states.amazonaws.com, nor the regional one is authorized to assume the provided role";
	public static final String INVALID_TOKEN = "InvalidToken";
	public static final String RESOURCE_NOT_FOUND_ERROR_CODE = "ResourceNotFound";
	public static final String STATE_MACHINE_DOES_NOT_EXIST_ERROR_CODE = "StateMachineDoesNotExist";
	public static final String STATE_MACHINE_ALREADY_EXISTS = "StateMachineAlreadyExists";
    public static final String STATE_MACHINE_INITIAL_REVISION_ID = "INITIAL";
}
