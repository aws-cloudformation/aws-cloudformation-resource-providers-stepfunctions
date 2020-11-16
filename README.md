# AWSCloudFormationResourceProvidersStepFunctions

This package contains the code for the State Machine and Activity Uluru resources.

##Building this Package on MacOS
- Create a new workspace. `brazil ws create --root AWSCloudFormationResourceProvidersStepFunctions`
- Add the package to the workspace. `cd AWSCloudFormationResourceProvidersStepFunctions && brazil ws use --package AWSCloudFormationResourceProvidersStepFunctions`
- Set up platform support. `brazil setup platform-support`
- Change the package's platform. `brazil ws use --platform AL2012`
- Build the package. `brazil-build wrapper`
- Open the workspace in Intellij. Ensure that when opening the project, the **workspace root** is selected.
- Configure the Project SDK to be `Java 8`.
- Click the `Brazil` dropdown from the menu, then select `Sync from Workspace`.

##Project Structure
```
.
├── activity
│   ├── canary-bundle
│   │   ├── canary
│   │   │   └── <CanaryTemplateName>_001.yaml
│   │   └── canary_settings.yaml 
│   ├── src
│   │   ├── main 
│   │   └── test 
│   ├── aws-stepfunctions-activity.json
│   ├── build.gradle
│   ├── pom.xml
│   ├── resource-role.yaml
│   └── template.yaml
├── statemachine
│   ├── canary-bundle
│   │   ├── canary
│   │   │   └── <CanaryTemplateName>_001.yaml
│   │   ├── bootstrap.yaml
│   │   └── canary_settings.yaml 
│   ├── src
│   │   ├── main 
│   │   └── test 
│   ├── aws-stepfunctions-statemachine.json
│   ├── build.gradle
│   ├── pom.xml
│   ├── resource-role.yaml
│   └── template.yaml
├── build.gradle
└── settings.internal.json
```

##Testing New Changes
Before merging new changes to the handler code into mainline, it is important to test the new functionality. In addition to writing unit tests, contributors can create a custom CloudFormation resource, and test that operations are correctly performed on the underlying resource.

####Sample testing workflow:
1. Execute the following commands from the root directory of that resource (eg. `./statemachine`) to generate the output files:
    - `mvn clean`
    - `cfn generate`
    - `mvn package`
2. Register the extension with CloudFormation and set it as the default:
    - `cfn submit --region <REGION_CODE_LOWER> --no-role --set-default`
3. Create a CloudFormation stack in the same region to test the handlers work as intended.
4. Deregister the customer type by executing the following command, substituting the region, resource name, and version:
    - `aws cloudformation deregister-type --region <REGION_CODE_LOWER> --type RESOURCE --type-name My::Resource::Example --version-id 00000003`
    - If this command fails saying the version cannot be deregistered due to it being the default version, repeat the command to delete all other versions until only the default version remains. Now run the same command as above, but without the `--version-id` flag.

##Running Unit Tests
Unit tests can be run for a given resource by executing the following command from the root directory of that resource (eg. `./statemachine`): `mvn test`

##Running Contract Tests
Uluru contract tests can be run for a given resource by:
1. Execute the following commands from the root directory of that resource (eg. `./statemachine`) to generate the output files:
    - `mvn clean`
    - `cfn generate`
    - `mvn package`
2. In a separate terminal window, navigate to the  root directory of the resource (eg. `./statemachine`) and execute the following command:
    - `sam local start-lambda`
3. In the original terminal window, run the tests by executing the following command:
    - `cfn test`
    - Note: to run a single contract test, append the following to the end of the above command: `-- -k contract_create_delete`

##Canaries
Canaries for each resource are defined in their respective `canary-bundle` directory (eg. `./statemachine/canary-bundle`). More details on how the canaries are defined and executed, as well as the metrics they emit, can be found here: https://w.amazon.com/bin/view/AWS21/Design/Uluru/Onboarding_Guide/Uluru_Operations_Guide/#HOnboardingCanaries

##Troubleshooting
- While performing Brazil or Maven commands, I observe an error saying the `.rpdk-config file is invalid`.
  - Open the config file at `statemachine/.rpdk-config` or `activity/.rpdk-config`.
  - Remove "artifact_type" and "executableEntrypoint" keys from the config.
- While running Brazil or Maven commands, I observe an error saying `Process 'command <path-to-brazil-build-temp-dir>/[AWSCloudFormationRPDKJavaPluginTool-2.0]run.runtimefarm/bin/cfn' finished with non-zero exit value 127`.
  - Run `brazil-build clean` to remove extra build artifacts.
- Executing the contract tests using `cfn submit` does not result in any output logs being added to the corresponding S3 bucket.
  - Navigate to your IAM roles and find the role named `CloudFormationManagedUplo-LogAndMetricsDeliveryRol-XXXXXXX`.
  - Modify the role's policy and trust relationships to match the snippets in https://w.amazon.com/bin/view/AWS21/Design/Uluru/ContractTests/Executing/#HTestingcontracttestswithcfnsubmit

##Relevant Links

####CloudFormation CLI:
- GitHub documentation here: https://github.com/aws-cloudformation/cloudformation-cli

####Canaries
- Onboarding guide: https://w.amazon.com/bin/view/AWS21/Design/Uluru/Onboarding_Guide/Uluru_Operations_Guide/#HOnboardingCanaries
- Canary runner code: https://code.amazon.com/packages/CFNManagedCanaryRunner/blobs/4b4fa420287aa88110719d130a99eec9083cebf0/--/src/com/amazonaws/cloudformation/managedcanary/CanaryRunner.java#L61

####Contract Tests
- Base documentation: https://w.amazon.com/bin/view/AWS21/Design/Uluru/ContractTests/
- Contract test code: https://github.com/aws-cloudformation/cloudformation-cli/tree/e874a82755da1511bbbc1f447a276badc2c92928/src/rpdk/core/contract/suite
- Test walkthrough: https://w.amazon.com/bin/view/AWS21/Design/Uluru/ContractTests/TestWalkThrough/
- Debugging documentation: https://w.amazon.com/bin/view/AWS21/Design/Uluru/ContractTests/Debugging/
- Local testing documentation:
    - https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test.html
    - https://w.amazon.com/bin/view/AWS21/Design/Uluru/ContractTests/Executing/


    