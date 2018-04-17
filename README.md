# ras-api

[![Build Status](https://travis-ci.org/hmrc/ras-api.svg)](https://travis-ci.org/hmrc/ras-api) [ ![Download](https://api.bintray.com/packages/hmrc/releases/ras-api/images/download.svg) ](https://bintray.com/hmrc/releases/ras-api/_latestVersion)

Check if an pension scheme member is a resident in Scotland for tax purposes. 

If you’re a pension scheme administrator, you need to know this to make the correct relief at source claims.


Before You Start
----------------

You’ll need the pension scheme member’s name, date of birth and National Insurance number.


Description
-----------

Find out if a scheme member pays tax in Scotland or in the rest of the UK, to tell you the rate of tax to use for their relief at source contributions. 

From 1 January to 5 April each year you will get currentYearResidencyStatus and nextYearForecastResidencyStatus within the response json. 

From 6 April to 31 December each year you will only get currentYearResidencyStatus within the response json. 


Testing Approach
----------------

You can [use the HMRC Developer Sandbox to test the API](https://test-developer.service.hmrc.gov.uk/api-documentation/docs/sandbox/introduction). The Sandbox is an enhanced testing service that functions as a simulator of HMRC’s production environment.

The Sandbox for the RAS API does not currently support [stateful behaviour](https://test-developer.service.hmrc.gov.uk/api-documentation/docs/sandbox/stateful-behaviour), but you can use the payloads described in the resources to test specific scenarios.

For all the scenarios you will need you to create your own test user, which will be used to authorise the OAuth 2.0 token. This will not create a test user which will be returned when making a request to the endpoint. To create your own test user:
   
1. Use the [Create Test User API](https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/api-platform-test-user/1.0#_create-a-test-user-which-is-an-organisation_post_accordion) and use relief-at-source as the service name
2. Then use the userId and password in the response to get an OAuth 2.0 access token


Requirements
------------

All end points are User Restricted (see [authorisation](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation)). Versioning, data formats etc follow the API Platform standards (see [the reference guide](https://developer.service.hmrc.gov.uk/api-documentation/docs/reference-guide)).

The API makes use of HATEOAS/HAL resource links. Your application does not need to store a catalogue of all URLs.

You can dive deeper into the documentation in the [API Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/ras-api/1.0).

This version of the API is in development and is very likely to change.

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs a [JRE to](http://www.oracle.com/technetwork/java/javase/overview/index.html) run.


Schemas
-------

| Schema          | Location                                                                                                                   |
| --------------- | -------------------------------------------------------------------------------------------------------------------------- |
| Error Codes     | https://raw.githubusercontent.com/hmrc/ras-api/master/resources/public/api/conf/1.0/schemas/ErrorCodes.schema.json         |
| Residency Check | https://raw.githubusercontent.com/hmrc/ras-api/master/resources/public/api/conf/1.0/schemas/getResidencyStatus.schema.json |


Resources
----------

| Method | URL                                            | Description                                                                                                                                            |
| :----: | ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| POST   | /individuals/relief-at-source/residency-status | Find out if a scheme member pays tax in Scotland or in the rest of the UK, to tell you the rate of tax to use for their relief at source contributions.|
                                                          
For more information, visit the [API Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/ras-api/1.0).

Test Data
---------

You can find test data [here](https://github.com/hmrc/ras-api/blob/master/resources/public/api/conf/1.0/testdata/get-residency-status.md) 


Running Locally
---------------

Install [Service Manager](https://github.com/hmrc/service-manager), then start dependencies:

    sm --start RAS_ALL -f

Start the app:

    sbt "run 9669"


License
-------

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
