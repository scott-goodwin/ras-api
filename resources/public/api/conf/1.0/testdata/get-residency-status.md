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
            <td><p>Request with a valid UUID</p><p class ="code--block">uuid: 2800a7ab-fe20-42ca-98d7-c33f4133cfc2</p></td>
            <td>
                <p>N/A</p>
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
            <td><p>Request with an invalid UUID</p><p class ="code--block">uuid: 2800a7ab-fe20-42ca-98d7-c33f4133cfc</p></td>
            <td>
                <p>N/A</p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVALID_UUID",<br>
                                            "message": "The match has timed out and the UUID is no longer valid. 
                                                        The match (POST to /match) will need to be repeated."<br>
                                         }<br>
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid UUID, but the account has been locked</p><p class ="code--block">uuid: 76648d82-309e-484d-a310-d0ffd2997794</p></td>
            <td>
                <p>N/A</p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "ACCOUNT_LOCKED",<br>
                                            "message": "The account is locked, please ask your customer to get in touch with HMRC."<br>
                                         }<br>
                </p>
            </td>
        </tr>
        <tr>
        	<td><p>Request with a valid UUID that has timed out</p><p class ="code--block">uuid: 11548d82-309e-484d-a310-d0ffd2997795</p></td>
	        <td>
	            <p>N/A</p>
	        </td>
	        <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVALID_UUID",<br>
                                            "message": "The match has timed out and the UUID is no longer valid. 
                                                        The match (POST to /match) will need to be repeated."<br>
                                         }<br>
                </p>
            </td>
        </tr>
        <tr>
             <td><p>Request sent to incorrect endpoint</p><p class ="code--block">Endpoint: /customer/mtch/2800a7ab-fe20-42ca-98d7-c33f4133cfc2/get-residency-status</p></td>
            <td>
                <p>N/A</p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
                <p class ="code--block"> {<br>
                                              "code": "NOT_FOUND",<br>
                                              "message": "Resource Not Found"<br>
                                            }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid UUID and an invalid 'Accept' header</p><p class ="code--block">uuid: 2800a7ab-fe20-42ca-98d7-c33f4133cfc2<br><br>Accept: application/vnd.hmrc.1.0</p></td>
            <td>
                <p>N/A</p>
            </td>
            <td><p>HTTP status: <code class="code--slim">406 (Not Acceptable)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "ACCEPT_HEADER_INVALID",<br>
                                            "message": "The accept header is missing or invalid"<br>
                                          }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request which fails due to an unexpected error</p><p class ="code--block">uuid: 76648d82-309e-484d-a310-d0ffd2997795</p></td>
            <td>
                <p>N/A</p>
            </td>
            <td><p>HTTP status: <code class="code--slim">500 (Internal Server Error)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INTERNAL_SERVER_ERROR",<br>
                                            "message": "Internal server error"<br>
                                          }
                </p>
            </td>
        </tr>
	</tbody>
</table>