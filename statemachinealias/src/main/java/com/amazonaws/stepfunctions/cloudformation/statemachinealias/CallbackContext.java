package com.amazonaws.stepfunctions.cloudformation.statemachinealias;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

import java.time.Instant;

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
    private boolean isTrafficShifting = false;
    private String originVersionArn;
    private String targetVersionArn;
    private Integer originVersionWeight;
    private Integer targetVersionWeight;
    private Instant lastShiftedTime;
}
