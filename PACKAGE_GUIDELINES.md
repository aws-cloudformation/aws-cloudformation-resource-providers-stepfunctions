# AWSCloudFormationResourceProvidersStepFunctions

##Package Information
This package contains the code for the State Machine and Activity Uluru resources.

Most of the contents of this package have been open sourced and are maintained in our [public GitHub repository for the Uluru resource providers](https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-stepfunctions). Changes to any files located in both this Brazil package and in the GitHub repository should be made in GitHub, as changes can be pulled from GitHub to Brazil, but not vice versa. 

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

##Package Setup

###Commands to run from local workplace:
```
brazil ws create --root AWSCloudFormationResourceProvidersStepFunctions --versionset GrapheneCloudFormationResourceProvider/release
cd AWSCloudFormationResourceProvidersStepFunctions && brazil ws use --package AWSCloudFormationResourceProvidersStepFunctions
brazil setup platform-support
y
brazil ws use --platform AL2012
cd src/AWSCloudFormationResourceProvidersStepFunctions
git remote add upstream "git@github.com:aws-cloudformation/aws-cloudformation-resource-providers-stepfunctions.git"
git fetch upstream
brazil-build
```
###Description of commands:
- Create a new workspace with the specified version set. 
- Add the resource providers package to the workspace. 
- Set up platform support.
- Respond to the prompt `Proceed with the setup? [y/N]` during platform support setup.
- Change the package's platform to `AL2012`.
- Set the public resource provider GitHub repository as a remote upstream repository.
- Fetch upstream changes from the public resource provider GitHub repository.
- Build the package.

###Opening the package in Intellij:
- Open the workspace in Intellij. Ensure that when opening the project, the **workspace root** is selected.
- Click `File` -> `Project Structure`. Configure the Project SDK to be `Java 8`. Set the project language level to `15 - Text blocks`. Click `Apply`, then `OK`. 
- Click the `Brazil` dropdown from the menu, then select `Sync from Workspace`. When prompted to configure the Gradle module setup, select `Classic`.

##Pulling in changes from GitHub:
Most of the contents of this package have been open sourced and are maintained in our [public GitHub repository for the Uluru resource providers](https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-stepfunctions). Use the following steps to sync changes from GitHub to this Brazil package. 
- `git branch` - Ensure you are on your local `mainline` branch.
- `git fetch --all` - Fetch the latest information from upstream and origin.
- `git status` - Ensure you should have a clean `mainline` branch that is tracking `origin/mainline`.
- `git merge upstream/main` - Merge in the latest changes from GitHub.
- `cr --parent origin/mainline` - Create a code review for the merged in changes. 

##Testing New Changes
Before merging new changes to the handler code into mainline, it is important to test the new functionality. In addition to writing unit tests, contributors can create a custom CloudFormation resource, and test that operations are correctly performed on the underlying resource.

###Sample testing workflow:
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

###Running Unit Tests:
Unit tests can be run for a given resource by:
1. Execute the following command from the root directory of the resource to test (eg. `./statemachine`):
    - Run `mvn test`

###Running Contract Tests:
Uluru contract tests can be run for a given resource by:
1. Execute the following commands from the root directory of the resource to test (eg. `./statemachine`) to generate the output files:
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
- While running `git fetch upstream`, I see errors like `ERROR: Repository not found. 
  fatal: Could not read from remote repository. Please make sure you have the correct access rights and the repository exists.`
    - If the repository is private, ensure you are a member of the `resource-provider-owner-stepfunctions` GitHub team: https://github.com/orgs/aws-cloudformation/teams/resource-provider-owner-stepfunctions/members
- While performing Brazil or Maven commands, I observe an error saying the `.rpdk-config file is invalid`.
  - Open the config file at `statemachine/.rpdk-config` or `activity/.rpdk-config`.
  - Remove "artifact_type" and "executableEntrypoint" keys from the config.
- While running Brazil or Maven commands, I observe an error saying `Process 'command <path-to-brazil-build-temp-dir>/[AWSCloudFormationRPDKJavaPluginTool-2.0]run.runtimefarm/bin/cfn' finished with non-zero exit value 127`.
  - Run `brazil-build clean` to remove extra build artifacts.
- Executing the contract tests using `cfn submit` does not result in any output logs being added to the corresponding S3 bucket.
  - Navigate to your IAM roles and find the role named `CloudFormationManagedUplo-LogAndMetricsDeliveryRol-XXXXXXX`.
  - Modify the role's policy and trust relationships to match the snippets in https://w.amazon.com/bin/view/AWS21/Design/Uluru/ContractTests/Executing/#HTestingcontracttestswithcfnsubmit

##Relevant Links

####Resource Providers GitHub repository: https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-stepfunctions

####CloudFormation CLI: https://github.com/aws-cloudformation/cloudformation-cli

####Canaries:
- Onboarding guide: https://w.amazon.com/bin/view/AWS21/Design/Uluru/Onboarding_Guide/Uluru_Operations_Guide/#HOnboardingCanaries
- Canary runner code: https://code.amazon.com/packages/CFNManagedCanaryRunner/blobs/4b4fa420287aa88110719d130a99eec9083cebf0/--/src/com/amazonaws/cloudformation/managedcanary/CanaryRunner.java#L61

####Contract Tests:
- Base documentation: https://w.amazon.com/bin/view/AWS21/Design/Uluru/ContractTests/
- Contract test code: https://github.com/aws-cloudformation/cloudformation-cli/tree/e874a82755da1511bbbc1f447a276badc2c92928/src/rpdk/core/contract/suite
- Test walkthrough: https://w.amazon.com/bin/view/AWS21/Design/Uluru/ContractTests/TestWalkThrough/
- Debugging documentation: https://w.amazon.com/bin/view/AWS21/Design/Uluru/ContractTests/Debugging/
- Local testing documentation:
    - https://docs.aws.amazon.com/cloudformation-cli/latest/userguide/resource-type-test.html
    - https://w.amazon.com/bin/view/AWS21/Design/Uluru/ContractTests/Executing/


    