# AWS::StepFunctions::StateMachineAlias RoutingConfigurationVersion

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#statemachineversionarn" title="StateMachineVersionArn">StateMachineVersionArn</a>" : <i>String</i>,
    "<a href="#weight" title="Weight">Weight</a>" : <i>Integer</i>
}
</pre>

### YAML

<pre>
<a href="#statemachineversionarn" title="StateMachineVersionArn">StateMachineVersionArn</a>: <i>String</i>
<a href="#weight" title="Weight">Weight</a>: <i>Integer</i>
</pre>

## Properties

#### StateMachineVersionArn

The Amazon Resource Name (ARN) that identifies one or two state machine versions defined in the routing configuration.

_Required_: Yes

_Type_: String

_Minimum Length_: <code>1</code>

_Maximum Length_: <code>2048</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Weight

The percentage of traffic you want to route to the state machine version. The sum of the weights in the routing configuration must be equal to 100.

_Required_: Yes

_Type_: Integer

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
