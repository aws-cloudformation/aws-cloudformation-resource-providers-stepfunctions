package com.amazonaws.stepfunctions.cloudformation.statemachine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    @Builder.Default
    private boolean deletionStarted = false;
}
