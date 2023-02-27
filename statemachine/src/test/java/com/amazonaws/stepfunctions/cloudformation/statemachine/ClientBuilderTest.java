package com.amazonaws.stepfunctions.cloudformation.statemachine;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.stepfunctions.model.CreateStateMachineRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientBuilderTest {

    @Test
    public void testRetryOnThrottling() {
        RetryPolicy retryPolicy = ClientBuilder.getRetryPolicy();

        AmazonWebServiceRequest request = new CreateStateMachineRequest();
        AmazonServiceException e = new AmazonServiceException("Throttling");
        e.setStatusCode(429);

        assertThat(retryPolicy.getRetryCondition().shouldRetry(request, e, 0)).isTrue();
    }

    @Test
    public void testRetryOnManagedRule() {
        RetryPolicy retryPolicy = ClientBuilder.getRetryPolicy();

        AmazonWebServiceRequest request = new CreateStateMachineRequest();
        AmazonServiceException e = new AmazonServiceException("Throttling");
        e.setErrorCode("AccessDeniedException");

        assertThat(retryPolicy.getRetryCondition().shouldRetry(request, e, 0)).isFalse();

        e.setErrorMessage(Constants.MANAGED_RULE_EXCEPTION_MESSAGE_SUBSTRING);
        assertThat(retryPolicy.getRetryCondition().shouldRetry(request, e, 0)).isTrue();

        e.setErrorMessage(Constants.STS_AUTHORIZED_TO_ASSUME_MESSAGE_SUBSTRING);
        assertThat(retryPolicy.getRetryCondition().shouldRetry(request, e, 0)).isTrue();

        e.setErrorMessage(Constants.LOGGING_ACCESS_DENIED_SUBSTRING);
        assertThat(retryPolicy.getRetryCondition().shouldRetry(request, e, 0)).isTrue();
    }

}
