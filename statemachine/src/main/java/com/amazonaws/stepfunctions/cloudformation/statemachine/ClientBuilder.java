package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;

public class ClientBuilder {

    public static AmazonS3 getS3Client() {
        return AmazonS3ClientBuilder.standard().build();
    }

    public static AWSStepFunctions getClient() {
        ClientConfiguration clientConfiguration = new ClientConfiguration()
                .withRetryPolicy(getRetryPolicy());

        return AWSStepFunctionsClientBuilder.standard()
                .withClientConfiguration(clientConfiguration)
                .build();
    }

    public static RetryPolicy getRetryPolicy() {
        return new RetryPolicy(CUSTOM_RETRY_CONDITION, PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY,
                Constants.MAX_ERROR_RETRIES, false
        );
    }

    public static final RetryPolicy.RetryCondition CUSTOM_RETRY_CONDITION = new CustomRetryCondition();

    public static class CustomRetryCondition implements RetryPolicy.RetryCondition {

        private RetryPolicy.RetryCondition defaultRetryCondition = PredefinedRetryPolicies.DEFAULT_RETRY_CONDITION;

        @Override
        public boolean shouldRetry(AmazonWebServiceRequest amazonWebServiceRequest, AmazonClientException e, int i) {
            if (e instanceof AmazonServiceException && isRetriableException((AmazonServiceException) e)) {
                return true;
            }
            return defaultRetryCondition.shouldRetry(amazonWebServiceRequest, e, i);
        }

        private boolean isRetriableException(AmazonServiceException exception) {
            return Constants.ACCESS_DENIED_ERROR_CODE.equals(exception.getErrorCode())
                    && (exception.getErrorMessage().contains(Constants.MANAGED_RULE_EXCEPTION_MESSAGE_SUBSTRING) ||
                    exception.getErrorMessage().contains(Constants.STS_AUTHORIZED_TO_ASSUME_MESSAGE_SUBSTRING) ||
                    exception.getErrorMessage().contains(Constants.LOGGING_ACCESS_DENIED_MESSAGE_SUBSTRING));
        }

    }

}
