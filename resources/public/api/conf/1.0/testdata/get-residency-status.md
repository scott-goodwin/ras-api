<table>
    <col width="25%">
    <col width="35%">
    <col width="40%">
    <thead>
        <tr>
            <th>Scenario</th>
            <th>Request Payload</th>
            <th>Example Response</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><p>Request with a valid payload where currentYearResidencyStatus is otherUKResident and nextYearForecastResidencyStatus is otherUKResident</p></td>
            <td>
                <p class ="code--block"> {<br>
                                           "nino" : "PC243122B",<br>
                                           "firstName" : "Peter",<br>
                                           "lastName" : "Armstrong",<br>
                                           "dateOfBirth" : "1969-01-01"<br>
                                         }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (Ok)</code></p>
                <p class="code--block">
                    {<br>
                      "currentYearResidencyStatus" : "otherUKResident",<br>
                      "nextYearForecastResidencyStatus" : "otherUKResident"<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload where currentYearResidencyStatus is otherUKResident and nextYearForecastResidencyStatus is scotResident</p></td>
            <td>
                <p class ="code--block"> {<br>
                                           "nino" : "BB123456B",<br>
                                           "firstName" : "John",<br>
                                           "lastName" : "Smith",<br>
                                           "dateOfBirth" : "1975-05-25"<br>
                                         }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (Ok)</code></p>
                <p class="code--block">
                    {<br>
                      "currentYearResidencyStatus" : "otherUKResident",<br>
                      "nextYearForecastResidencyStatus" : "scotResident"<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload where currentYearResidencyStatus is scotResident and nextYearForecastResidencyStatus is otherUKResident</p></td>
            <td>
                <p class ="code--block"> {<br>
                                           "nino" : "LR325154D",<br>
                                           "firstName" : "Jane",<br>
                                           "lastName" : "Doe",<br>
                                           "dateOfBirth" : "1969-06-09"<br>
                                         }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (Ok)</code></p>
                <p class="code--block">
                    {<br>
                      "currentYearResidencyStatus" : "scotResident",<br>
                      "nextYearForecastResidencyStatus" : "otherUKResident"<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload where currentYearResidencyStatus is scotResident and nextYearForecastResidencyStatus is scotResident</p></td>
            <td>
                <p class ="code--block"> {<br>
                                           "nino" : "CC123456C",<br>
                                           "firstName" : "Joe",<br>
                                           "lastName" : "Bloggs",<br>
                                           "dateOfBirth" : "1982-02-17"<br>
                                         }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (Ok)</code></p>
                <p class="code--block">
                    {<br>
                      "currentYearResidencyStatus" : "scotResident",<br>
                      "nextYearForecastResidencyStatus" : "scotResident"<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload where currentYearResidencyStatus is otherUKResident</p></td>
            <td>
                <p class ="code--block"> {<br>
                                           "nino" : "AA315445B",<br>
                                           "firstName" : "Victoria",<br>
                                           "lastName" : "Clark",<br>
                                           "dateOfBirth" : "1981-12-12"<br>
                                         }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (Ok)</code></p>
                <p class="code--block">
                    {<br>
                      "currentYearResidencyStatus" : "otherUKResident",<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload where currentYearResidencyStatus is scotResident</p></td>
            <td>
                <p class ="code--block"> {<br>
                                           "nino" : "BA315445B",<br>
                                           "firstName" : "Charlie",<br>
                                           "lastName" : "Thompson",<br>
                                           "dateOfBirth" : "1941-10-12"<br>
                                         }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (Ok)</code></p>
                <p class="code--block">
                    {<br>
                      "currentYearResidencyStatus" : "scotResident",<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
             <td><p>Request with an invalid nino, no first name, invalid data type for last name and a date of birth which does not exist</p></td>
             <td>
                 <p class ="code--block"> {<br>
                                            "nino" : "LE241131E",<br>
                                             "lastName" : true,<br>
                                             "dateOfBirth" : "1989-02-30"<br>
                                          }
                 </p>
             </td>
             <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                 <p class ="code--block"> {<br>
                                             "code": "BAD_REQUEST",<br>
                                             "message": "Bad Request"<br>
                                             "errors": [<br>
                                             {<br>
                                                   "code": "INVALID_FORMAT",<br>
                                                   "message": "Invalid format has been used",<br>
                                                   "path": "/nino"<br>
                                                 },<br>
                                                 {<br>
                                                   "code": "INVALID_DATA_TYPE",<br>
                                                   "message": "Invalid data type has been used",<br>
                                                   "path": "/lastName"<br>
                                                 },<br>
                                                 {<br>
                                                   "code": "INVALID_DATE",<br>
                                                   "message": "Date is invalid",<br>
                                                   "path": "/dateOfBirth"<br>
                                                 },<br>
                                                 {<br>
                                                   "code": "MISSING_FIELD",<br>
                                                   "message": "This field is required",<br>
                                                   "path": "/firstName"<br>
                                                 }<br>
                                             ]<br>
                                          }
                 </p>
             </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload, but the customer could not be found</p></td>
            <td>
                <p class ="code--block"> {<br>
                                                 "nino" : "SE235112A",<br>
                                                 "firstName" : "Raj",<br>
                                                 "lastName" : "Patel",<br>
                                                 "dateOfBirth" : "1984-10-30"<br>
                                              }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                               "code": "MATCHING_FAILED",<br>
                                               "message": "The individual's details provided did not match with HMRC’s records."<br>
                                             }
                </p>
            </td>
        </tr>
	</tbody>
</table>