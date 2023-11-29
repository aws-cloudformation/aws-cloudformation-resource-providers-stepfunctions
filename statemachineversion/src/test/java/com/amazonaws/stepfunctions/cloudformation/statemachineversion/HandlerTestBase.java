package com.amazonaws.stepfunctions.cloudformation.statemachineversion;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.model.AWSStepFunctionsException;
import org.mockito.Mock;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;

public abstract class HandlerTestBase {

  protected final static AmazonServiceException exception500 = new AmazonServiceException("Server error");
  protected final static AmazonServiceException exception400 = new AmazonServiceException("Client error");
  protected final static AmazonServiceException stateMachineVersionDoesNotExist =
          new AmazonServiceException("State machine version does not exist");
  protected final static AmazonServiceException resourceNotFoundException =
          new AmazonServiceException("Resource does not exist");
  protected final static AmazonServiceException throttlingException =
          new AmazonServiceException("Your request has been throttled");
  protected final static AWSStepFunctionsException iamManagedRuleException = new AWSStepFunctionsException(
          "arn:aws:iam::000000000000:role/role' is not authorized to create managed-rule.");
  protected final static AmazonServiceException accessDeniedException = new AmazonServiceException("");
  protected final static String AWS_ACCOUNT_ID = "1234567890";
  protected final static String REGION = "us-east-1";
  protected final static String STATE_MACHINE_ARN = "arn:aws:states:us-east-1:1234567890:stateMachine:TestStateMachine";
  protected final static String STATE_MACHINE_VERSION_ARN = "arn:aws:states:us-east-1:1234567890:stateMachine:TestStateMachine:1";
  protected final static String STATE_MACHINE_REVISION_ID = "64bb58ee-63e7-4573-a877-5cb65cdd5f30";
  protected final static String DESCRIPTION = "TestStateMachine version description.";

  static {
    exception400.setStatusCode(400);
    exception500.setStatusCode(500);
    throttlingException.setStatusCode(400);
    stateMachineVersionDoesNotExist.setStatusCode(400);
    resourceNotFoundException.setStatusCode(400);
    throttlingException.setErrorCode("ThrottlingException");
    accessDeniedException.setErrorCode(Constants.ACCESS_DENIED_ERROR_CODE);
    iamManagedRuleException.setErrorCode("AccessDeniedException");
    iamManagedRuleException.setStatusCode(400);
    stateMachineVersionDoesNotExist.setErrorCode(Constants.STATE_MACHINE_DOES_NOT_EXIST_ERROR_CODE);
    resourceNotFoundException.setErrorCode(Constants.RESOURCE_NOT_FOUND_ERROR_CODE);
  }

  @Mock
  protected AmazonWebServicesClientProxy proxy;

  @Mock
  protected AWSStepFunctions client;

  @Mock
  protected Logger logger;

}
