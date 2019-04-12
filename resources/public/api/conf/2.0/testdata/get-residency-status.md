<table>
    <col width="25%">
    <col width="35%">
    <col width="40%">
    <thead>
        <tr>
            <th>Scenario</th>
            <th>Example Response</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>
                <p>Request with a valid payload where previousYearRTIEmployedEarnings is the only field contained in the response</p>
                 <p class="code--block">
                   <strong>nino:</strong><br>
                   Create test data in NTC-STUBS
                 </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (Ok)</code></p>
                <p class="code--block">
                    {<br>
                      "previousYearRTIEmployedEarnings" : test data<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with a valid payload where previousYearRTIEmployedEarningsPartner is the only field contained in the response</p>
                 <p class="code--block">
                   <strong>nino:</strong><br>
                   Create test data in NTC-STUBS
                 </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (Ok)</code></p>
                <p class="code--block">
                    {<br>
                      "previousYearRTIEmployedEarningsPartner" : test data<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with a valid payload where previousYearRTIEmployedEarningsPartner is the only field contained in the response</p>
                 <p class="code--block">
                   <strong>nino:</strong><br>
                   Create test data in NTC-STUBS
                 </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (Ok)</code></p>
                <p class="code--block">
                    {<br>
                      "previousYearRTIEmployedEarnings" : test data,<br>
                      "previousYearRTIEmployedEarningsPartner" : test data<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with a valid payload containing an invalid National Insurance Number</p>
                 <p class="code--block">
                   <strong>nino:</strong><br>
                   Create test data in NTC-STUBS
                 </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (BadRequest)</code></p>
                <p class="code--block">
                    {<br>
                      "code": "BAD_REQUEST",<br>
                      "message": "National Insurance Number in the URL is in the wrong format."<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with a valid payload where the individual doesn't have RTI data</p>
                 <p class="code--block">
                   <strong>nino:</strong><br>
                   Create test data in NTC-STUBS
                 </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (NotFound)</code></p>
                <p class="code--block">
                    {<br>
                      "code": "RTI_NOT_FOUND",<br>
                      "message": "Cannot provide employment earnings for this individual."<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with a valid payload but there's an internal server error</p>
                 <p class="code--block">
                   <strong>nino:</strong><br>
                   Create test data in NTC-STUBS
                 </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">500 (InternalServerError)</code></p>
                <p class="code--block">
                    {<br>
                      "code": "INTERNAL_SERVER_ERROR",<br>
                      "message": "Internal Server Error."<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with a valid payload but the service is unavailable</p>
                 <p class="code--block">
                   <strong>nino:</strong><br>
                   Create test data in NTC-STUBS
                 </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">503 (ServiceUnavailable)</code></p>
                <p class="code--block">
                    {<br>
                      "code": "SERVICE_UNAVAILABLE",<br>
                      "message": "Service Unavailable."<br>
                    }
                </p>
            </td>
        </tr>
	</tbody>
</table>