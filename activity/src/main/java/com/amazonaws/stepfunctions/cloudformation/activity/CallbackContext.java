package com.amazonaws.stepfunctions.cloudformation.activity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@lombok.Getter
@lombok.Setter
@lombok.ToString
@lombok.EqualsAndHashCode(callSuper = true)
public class CallbackContext extends StdCallbackContext {
    @Builder.Default
    private boolean propagationDelayDone = false;
}
