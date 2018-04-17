# ras-api

[![Build Status](https://travis-ci.org/hmrc/ras-api.svg)](https://travis-ci.org/hmrc/ras-api) [ ![Download](https://api.bintray.com/packages/hmrc/releases/ras-api/images/download.svg) ](https://bintray.com/hmrc/releases/ras-api/_latestVersion)

Use this API to check HMRC records to see if an individual is a Scottish resident for tax purposes.

All end points are User Restricted (see [authorisation](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation)). Versioning, data formats etc follow the API Platform standards (see [the reference guide](https://developer.service.hmrc.gov.uk/api-documentation/docs/reference-guide)).

The API makes use of HATEOAS/HAL resource links. Your application does not need to store a catalogue of all URLs.

You can dive deeper into the documentation in the [API Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/api#self-assessment-api).

This version of the API is in development and is very likely to change.

## Requirements

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs a [JRE to](http://www.oracle.com/technetwork/java/javase/overview/index.html) run.

### Running Locally

Install [Service Manager](https://github.com/hmrc/service-manager), then start dependencies:

    sm --start RAS_ALL -f

Start the app:

    sbt "run 9669"

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
