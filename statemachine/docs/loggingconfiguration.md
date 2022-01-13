# AWS::StepFunctions::StateMachine LoggingConfiguration

## Syntax

To declare this entity in your AWS CloudFormation template, use the following syntax:

### JSON

<pre>
{
    "<a href="#level" title="Level">Level</a>" : <i>String</i>,
    "<a href="#includeexecutiondata" title="IncludeExecutionData">IncludeExecutionData</a>" : <i>Boolean</i>,
    "<a href="#destinations" title="Destinations">Destinations</a>" : <i>[ <a href="logdestination.md">LogDestination</a>, ... ]</i>
}
</pre>

### YAML

<pre>
<a href="#level" title="Level">Level</a>: <i>String</i>
<a href="#includeexecutiondata" title="IncludeExecutionData">IncludeExecutionData</a>: <i>Boolean</i>
<a href="#destinations" title="Destinations">Destinations</a>: <i>
      - <a href="logdestination.md">LogDestination</a></i>
</pre>

## Properties

#### Level

_Required_: No

_Type_: String

_Allowed Values_: <code>ALL</code> | <code>ERROR</code> | <code>FATAL</code> | <code>OFF</code>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### IncludeExecutionData

_Required_: No

_Type_: Boolean

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)

#### Destinations

_Required_: No

_Type_: List of <a href="logdestination.md">LogDestination</a>

_Update requires_: [No interruption](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/using-cfn-updating-stacks-update-behaviors.html#update-no-interrupt)
