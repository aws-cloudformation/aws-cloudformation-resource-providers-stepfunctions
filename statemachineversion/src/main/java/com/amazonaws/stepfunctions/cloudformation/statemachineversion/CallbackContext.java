package com.amazonaws.stepfunctions.cloudformation.statemachineversion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallbackContext extends StdCallbackContext {
    @Builder.Default
    private boolean deletionStarted = false;
}
