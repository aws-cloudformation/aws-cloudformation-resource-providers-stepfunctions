# AWS::StepFunctions::StateMachineAlias DeploymentPreference

The settings to enable gradual state machine deployments.

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#statemachineversionarn" title="StateMachineVersionArn">StateMachineVersionArn</a>" : <i>String</i>,
    "<a href="#type" title="Type">Type</a>" : <i>String</i>,
    "<a href="#percentage" title="Percentage">Percentage</a>" : <i>Integer</i>,
    "<a href="#interval" title="Interval">Interval</a>" : <i>Integer</i>,
    "<a href="#alarms" title="Alarms">Alarms</a>" : <i>[ String, ... ]</i>
}
</pre>

### YAML

<pre>
<a href="#statemachineversionarn" title="StateMachineVersionArn">StateMachineVersionArn</a>: <i>String</i>
<a href="#type" title="Type">Type</a>: <i>String</i>
<a href="#percentage" title="Percentage">Percentage</a>: <i>Integer</i>
<a href="#interval" title="Interval">Interval</a>: <i>Integer</i>
<a href="#alarms" title="Alarms">Alarms</a>: <i>
      - String</i>
</pre>

## Properties

#### StateMachineVersionArn

_Required_: Yes

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>2048</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Type

The type of deployment to perform.

_Required_: Yes

_Type_: String

_Allowed Values_: <code>LINEAR</code> | <code>ALL_AT_ONCE</code> | <code>CANARY</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Percentage

The percentage of traffic to shift to the new version in each increment.

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Interval

The time in minutes between each traffic shifting increment.

_Required_: No

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Alarms

A list of CloudWatch alarm names that will be monitored during the deployment. The deployment will fail and rollback if any alarms go into ALARM state.

_Required_: No

_Type_: List of String

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
