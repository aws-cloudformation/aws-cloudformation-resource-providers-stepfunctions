package com.amazonaws.stepfunctions.cloudformation.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallbackContext extends StdCallbackContext {
    private boolean isActivityDeletionStarted;
    private int retryCount;
}
