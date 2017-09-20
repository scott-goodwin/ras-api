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
            <td><p>Request with an invalid format UUID</p><p class ="code--block">uuid: 2800a7ab-fe20-42ca-98d7-c33f4133cfc</p></td>
            <td>
                <p>N/A</p>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVALID_FORMAT",<br>
                                            "message": "Invalid UUID format. Use the UUID provided."<br>
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
                                                        The match (POST to /customer/match) will need to be repeated."<br>
                                         }<br>
                </p>
            </td>
        </tr>
	</tbody>
</table>