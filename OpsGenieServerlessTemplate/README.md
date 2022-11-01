# OpsGenieServerlessTemplate

DESCRIPTION: 
  SAM app integration for creating OpsGenie Alerts when DevOpsGuru creates an Insight

DOCUMENTATION

Publishing a SAM app
https://docs.aws.amazon.com/serverlessrepo/latest/devguide/serverlessrepo-quick-start.html

Template Anatomy for .yaml file
https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-specification-template-anatomy.html

OpsGenie Java API
https://docs.opsgenie.com/docs/opsgenie-java-api
http://opsgeniedownloads.s3-website-us-west-2.amazonaws.com/java-sdk-doc/

SAM APP NAME: "DevOps-Guru-OpsGenie-Connector"

INSTRUCTIONS FOR DEPLOYING SAM APP

1. Make sure to fill out all 3 parameters. 

2. Press the deploy button and everything should be functioning properly. 

3. See the below instructions if you want to customize the alerts to your liking

INSTRUCTIONS FOR APPLYING A USE CASE:

1. Open to the "OpsGenie.java" file

2. At the top you should see an enum called "useCase"

3. Pick one of the enum values that describes the use case you would like and scroll down to the "caseChoice" variable and change its value to useCase.<the use case of your choice>

  Here is a list of the use cases. By default all AllEVENTS  is on. All use cases do insight open/close
    ALLEVENTS,
		ANOMALIES,
		RECOMMENDATIONS,
		SEVERITYUPGRADED,
		PROACTIVEANDHIGHONLY,
		PROACTIVEANDMEDIUMONLY,
		PROACTIVEANDLOWONLY,
		REACTIVEANDHIGHONLY,
		REACTIVEANDMEDIUMONLY,
		REACTIVEANDLOWONLY,

4. Save your code and do the instructions below in the AFTER YOU CUSTOMIZE YOUR CODE section.

INSTRUCTIONS FOR CUSTOMIZING CODE:

1. Go to the Functions/src/main/java/aws.devopsguru.opsgenie folder and you'll see a "OpsGenie" and "AlertType" file

CUSTOMIZING ALERT DETAILS
-IF you would like to customize the details of a specific alert, go to the "AlertType" file and look at the function corresponding to the event trigger you would like to change. There is 1 function for each trigger should be clear to pick out. There are some comments in each section to help generally guide you but depending on what you need please view the following:

    -To add more OpsGenie specific details, please view the OpsGenie Java API for the function calls that are available in addition to the comments in the "newInsight" function which give you examples of additional fields you can populate if you so choose. 

    -To access specific details of an EventBridge event, go to the Eventbridge https://us-east-1.console.aws.amazon.com/events/home?region=us-east-1#/explore and go down to "Sample Event". Choose the event type such as "devops guru new insight open" to see the json format and the details available for you to access. From there, follow the existing examples using the jsonNode variable "input" to grab the information you desire for your alert. 

AFTER YOU CUSTOMIZE YOUR CODE

-Run a "Maven build" in your IDE such as Eclipse to make a new .jar file out of your code and upload it to your Lambda function

DEPLOYING THIS CODE AS A NEW SAM APP

1. Go to the template.yaml file and add the Metadata section. Below is an example you can follow. See the documentation for publishing a SAM app for more details on the steps needed for deploying if you haven't done it before. 

Metadata:
  AWS::ServerlessRepo::Application:
    Name: DevOps-Guru-OpsGenie-Connector
    Description: Creates an Lambda function template to create an OpsGenie alert with an Eventbridge rule attached
    Author: Amazon DevOps Guru
    SpdxLicenseId: Apache-2.0
    LicenseUrl: // Local path of LICENSE.txt file
    ReadmeUrl: // Local path of README.md file
    Labels: ['OpsGenie']
    HomePageUrl: https://github.com/aws-samples/amazon-devops-guru-connector-opsgenie
    SemanticVersion: 0.0.1
    SourceCodeUrl: https://github.com/aws-samples/amazon-devops-guru-connector-opsgenie


