package com.amazonaws.stepfunctions.cloudformation.activity;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.DeleteActivityRequest;
import com.amazonaws.services.stepfunctions.model.DescribeActivityRequest;
import com.google.common.util.concurrent.Uninterruptibles;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class DeleteHandler extends ResourceHandler {

    private static final int RETRY_COUNTER = 3;
    private static final int INITIAL_RETRY_DELAY_SECONDS = 10;
    private static final int MAX_RETRY_DELAY_SECONDS = 30;
    private static final int STABILIZATION_CALL_COUNT = 10;
    private static final Random random = new Random();

    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
            final AmazonWebServicesClientProxy proxy,
            final ResourceHandlerRequest<ResourceModel> request,
            final CallbackContext callbackContext,
            final Logger logger) {

        logger.log("INFO Activity DeleteHandler with clientRequestToken: " + request.getClientRequestToken());

        final ResourceModel model = request.getDesiredResourceState();

        try {
            if (callbackContext == null || !callbackContext.isActivityDeletionStarted()) {
                return initiateDeleteActivity(proxy, model, logger);
            } else {
                int currentRetryCount = callbackContext.getRetryCount();
                if (currentRetryCount <= RETRY_COUNTER) {
                    callbackContext.setRetryCount(currentRetryCount + 1);
                    return stabilizeDeleteActivity(proxy, model, callbackContext.getRetryCount(), logger);
                } else {
                    // Failsafe
                    // If somehow DescribeActivity is still able to describe the deleted activity after 3 retry
                    // attempts (>60s since delete), fallback to previous behaviour to not break backwards compatibility.
                    return ProgressEvent.<ResourceModel, CallbackContext>builder()
                            .status(OperationStatus.SUCCESS)
                            .build();
                }
            }
        } catch (Exception e) {
            logger.log("ERROR Deleting Activity, caused by " + e);
            ProgressEvent<ResourceModel, CallbackContext> failedProgressEvent = handleDefaultError(request, e);
            failedProgressEvent.setCallbackContext(CallbackContext.builder().build());

            return failedProgressEvent;
        }
    }

    private ProgressEvent<ResourceModel, CallbackContext> initiateDeleteActivity(
            final AmazonWebServicesClientProxy proxy,
            final ResourceModel model,
            final Logger logger) {
        verifyActivityArnIsPresent(model.getArn());

        AWSStepFunctions sfnClient = ClientBuilder.getClient();

        // Validate that the activity exists
        DescribeActivityRequest describeActivityRequest = new DescribeActivityRequest();
        describeActivityRequest.setActivityArn(model.getArn());
        proxy.injectCredentialsAndInvoke(describeActivityRequest, sfnClient::describeActivity);

        DeleteActivityRequest deleteActivityRequest = new DeleteActivityRequest();
        deleteActivityRequest.setActivityArn(model.getArn());

        proxy.injectCredentialsAndInvoke(deleteActivityRequest, sfnClient::deleteActivity);

        return stabilizeDeleteActivity(proxy, model, 0, logger);
    }

    private ProgressEvent<ResourceModel, CallbackContext> stabilizeDeleteActivity(
            final AmazonWebServicesClientProxy proxy,
            final ResourceModel model,
            final int currentRetryCount,
            final Logger logger) {

        ClientConfiguration clientConfiguration = new ClientConfiguration()
                .withRetryPolicy(PredefinedRetryPolicies.NO_RETRY_POLICY);

        DescribeActivityRequest describeActivityRequest = new DescribeActivityRequest();
        describeActivityRequest.setActivityArn(model.getArn());

        for (int i = 0; i < STABILIZATION_CALL_COUNT; i++) {
            try {
                AWSStepFunctions sfnClient = AWSStepFunctionsClientBuilder.standard()
                        .withClientConfiguration(clientConfiguration)
                        .build();

                proxy.injectCredentialsAndInvoke(describeActivityRequest, sfnClient::describeActivity);
                // If DescribeActivity succeeds, the activity still exists.
                logger.log("Activity still exists after DescribeActivity attempt #" + (i + 1));
                // If any DescribeActivity call succeeded, the activity still exists and the cache has not been cleared yet
                // Retry as DescribeActivity failure count has not reached the desired stabilization attempts
                return ProgressEvent.<ResourceModel, CallbackContext>builder()
                        .status(OperationStatus.IN_PROGRESS)
                        .callbackContext(CallbackContext.builder()
                                .retryCount(currentRetryCount)
                                .isActivityDeletionStarted(true)
                                .build())
                        .callbackDelaySeconds(calculateExponentialBackoffDelayWithJitter(currentRetryCount))
                        .build();
            } catch (Exception e) {
                // If DescribeActivity fails, wait for the next attempt.
                logger.log("DescribeActivity failed on attempt #" + (i + 1) + ": " + e.getMessage());
                Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
            }
        }

        // If all DescribeActivity attempts failed, assume the activity is deleted.
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
                .status(OperationStatus.SUCCESS)
                .callbackContext(CallbackContext.builder().build())
                .build();
    }

    private int calculateExponentialBackoffDelayWithJitter(int retryAttempt) {
        int exponentialDelay = Math.min((int) Math.pow(2, retryAttempt) * INITIAL_RETRY_DELAY_SECONDS, MAX_RETRY_DELAY_SECONDS);
        int jitter = (int) ((random.nextDouble() - 0.5) * exponentialDelay);
        int delayWithJitter = exponentialDelay + jitter;
        return Math.min(delayWithJitter, MAX_RETRY_DELAY_SECONDS);
    }
}
