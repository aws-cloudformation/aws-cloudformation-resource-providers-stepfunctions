package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.AWSStepFunctionsException;
import org.mockito.Mock;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;

import static com.amazonaws.stepfunctions.cloudformation.activity.Constants.ACCESS_DENIED_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.activity.Constants.ACTIVITY_DOES_NOT_EXIST_ERROR_CODE;
import static com.amazonaws.stepfunctions.cloudformation.activity.Constants.THROTTLING_ERROR_CODE;

public abstract class HandlerTestBase {

    protected final static AmazonServiceException exception500 = new AmazonServiceException("Server error");
    protected final static AmazonServiceException exception400 = new AmazonServiceException("Client error");
    protected final static RuntimeException unknownException = new RuntimeException("Runtime error");
    protected final static AmazonServiceException throttlingException = new AmazonServiceException("Your request has been throttled");
    protected final static AWSStepFunctionsException iamManagedRuleException = new AWSStepFunctionsException(
            "arn:aws:iam::000000000000:role/role' is not authorized to create managed-rule.");

    protected final static String AWS_ACCOUNT_ID = "1234567890";
    protected final static String REGION = "us-east-1";
    protected final static String ACTIVITY_NAME = "TestActivity";
    protected final static String ACTIVITY_ARN = "arn:aws:states:us-east-1:1234567890:activity:TestActivity";

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
