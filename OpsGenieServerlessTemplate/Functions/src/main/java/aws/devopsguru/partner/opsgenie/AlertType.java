package aws.devopsguru.partner.opsgenie;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.log4j.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.ifountain.opsgenie.client.OpsGenieClient;
import com.ifountain.opsgenie.client.swagger.ApiException;
import com.ifountain.opsgenie.client.swagger.api.AlertApi;
import com.ifountain.opsgenie.client.swagger.model.AddAlertDetailsRequest;
import com.ifountain.opsgenie.client.swagger.model.Alert;
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest;
import com.ifountain.opsgenie.client.swagger.model.DeleteAlertRequest;
import com.ifountain.opsgenie.client.swagger.model.GetAlertResponse;
import com.ifountain.opsgenie.client.swagger.model.Recipient;
import com.ifountain.opsgenie.client.swagger.model.TeamRecipient;

public class AlertType {
	
	public static void createAlert(JsonNode json, Logger LOGGER) {
		
		// Sets the API key from the environment variable passed to Lambda function
		AlertApi opsGenieClient = new OpsGenieClient().alertV2();
		
		// Null/Empty check for API_KEY and sets it to OpsGenieClient
		if (System.getenv("API_KEY") != null && !System.getenv("API_KEY").trim().isEmpty()) 
		{
			opsGenieClient.getApiClient().setApiKey(System.getenv("API_KEY"));
		} 
		else 
		{
			LOGGER.error("API_KEY has not been set properly check enviornment variable in Lambda");
		}
		
		// Based on the messageType of the event, route to different logic for the alert
		if (json.path("detail").path("messageType").asText().equals("NEW_INSIGHT")) 
		{
			AlertType.newInsight(json, LOGGER, opsGenieClient);
		} 
		else if (json.path("detail").path("messageType").asText().equals("CLOSED_INSIGHT")) 
		{
			AlertType.insightClosed(json, LOGGER, opsGenieClient);
		} 
		else if (json.path("detail").path("messageType").asText().equals("NEW_ASSOCIATION")) 
		{
			AlertType.newAssociation(json, LOGGER, opsGenieClient);
		} 
		else if (json.path("detail").path("messageType").asText().equals("NEW_RECOMMENDATION")) 
		{
			AlertType.newRecommendation(json, LOGGER, opsGenieClient);
		} 
		else if (json.path("detail").path("messageType").asText().equals("SEVERITY_UPGRADED")) 
		{
			AlertType.newSeverity(json, LOGGER, opsGenieClient);
		} 
		else 
		{
			LOGGER.error("Json not parsed properly or messageType is null/incorrect");
		}
	}

	public static void setOpsGenieSeverity(String severity, CreateAlertRequest opsGenieRequest) {
		
		// Decide the severity of the alert
		if (severity.equals("high")) 
		{
			opsGenieRequest.setPriority(CreateAlertRequest.PriorityEnum.P2);
		} 
		else if (severity.equals("medium")) 
		{
			opsGenieRequest.setPriority(CreateAlertRequest.PriorityEnum.P3);
		} 
		else 
		{
			opsGenieRequest.setPriority(CreateAlertRequest.PriorityEnum.P4);
		} 
	}
	
	public static void newInsight(JsonNode input, Logger LOGGER, AlertApi opsGenieClient) {
		
		// Set all the details of the alert 
		CreateAlertRequest opsGenieRequest = new CreateAlertRequest();
		opsGenieRequest.setMessage(input.path("detail").path("insightDescription").asText());
		opsGenieRequest.setAlias(input.path("detail").path("insightId").asText());
		opsGenieRequest.setDescription(String.format("Source: %s\n"
				+ "AlertType: %s\n"
				+ "Creation Time: %s\n"
				+ "Region: %s\n"
				+ "Insight Type: %s\n"
				+ "InsightURL: %s", 
				input.path("source").asText(), 
				input.path("detail").path("messageType").asText(), 
				input.path("time").asText(), 
				input.path("region").asText(), 
				input.path("detail").path("insightType").asText(), 
				input.path("detail").path("insightUrl").asText()));
		
		// String for holding all anomaly details
		String anomalyDetails = "";

		// Add the new anomalies into 1 string
		for (Iterator<JsonNode> anomalies = input.path("detail").path("anomalies").iterator(); anomalies.hasNext();) {

			JsonNode anomaly = anomalies.next();
			for (Iterator<JsonNode> sourceDetails = anomaly.path("sourceDetails").iterator(); sourceDetails
					.hasNext();) {
				JsonNode sourceDetail = sourceDetails.next();
				anomalyDetails += String.format("Data Source: %s\n" + "Name: %s\n" + "Stat: %s\n\n",
						sourceDetail.path("dataSource").asText(),
						sourceDetail.path("dataIdentifiers").path("name").asText(),
						sourceDetail.path("dataIdentifiers").path("stat").asText());
			}
		}
		
		// Map needed as argument parameter for .setDetails()
		Map<String, String> details = new HashMap<String, String>();
		details.put("Anomalies", anomalyDetails);
		opsGenieRequest.setDetails(details);
		
		// Null/Empty check for TEAM_NAME
		if (System.getenv("TEAM_NAME") != null && !System.getenv("TEAM_NAME").trim().isEmpty())
		{
			// If you would like to add more teams, simply add a comma inside the Arrays.asList after the 1st entry
			// and add another new TeamRecipient().name("anotherTeamName")
			opsGenieRequest.setTeams(Arrays.asList(new TeamRecipient().name(System.getenv("TEAM_NAME"))));
			
			// Same logic for setVisibleTo except type cast it like so (Recipient) new TeamRecipient().name()
			opsGenieRequest.setVisibleTo(Collections.singletonList((Recipient) new TeamRecipient().name("TEAM_NAME")));
		}
		else
		{
			LOGGER.warn("TEAM_NAME has not been set properly check enviornment variable in Lambda");
		}
		
		// Null/Empty check for EMAIL
		if (System.getenv("EMAIL") != null && !System.getenv("EMAIL").trim().isEmpty())
		{
			opsGenieRequest.setUser(System.getenv("EMAIL"));
		}
		else
		{
			LOGGER.warn("EMAIL has not been set properly check enviornment variable in Lambda");
		}
		
		setOpsGenieSeverity(input.path("detail").path("insightSeverity").asText(), opsGenieRequest);

		// Alert creation happens here after request fields filled out
		try {
			opsGenieClient.createAlert(opsGenieRequest);
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Alert creation failed due to: \n", e);
			e.printStackTrace();
		}
	}
	
	public static void insightClosed (JsonNode input, Logger LOGGER, AlertApi opsGenieClient) {
		
		// Identify the alert to delete
		DeleteAlertRequest request = new DeleteAlertRequest();
		request.setIdentifier(input.path("detail").path("insightId").asText());
		request.setIdentifierType(DeleteAlertRequest.IdentifierTypeEnum.ALIAS);

		// Alert deletion happens here
		try {
			opsGenieClient.deleteAlert(request);
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Insight failed to close due to: \n", e);
			e.printStackTrace();
		}
	}
	
	public static void newAssociation(JsonNode input, Logger LOGGER, AlertApi opsGenieClient) {
		
		// Retrieve existing anomaly details
		GetAlertResponse alert = null;
		try {
			alert = opsGenieClient.getAlert(input.path("detail").path("insightId").asText(), "alias");
		} catch (ApiException e1) {
			// TODO Auto-generated catch block
			LOGGER.error("Getting the existing alert failed due to: \n", e1);
			e1.printStackTrace();
		}

		// String for holding all anomaly details
		String anomalyDetails = "";

		// Check if there are previous anomalies to add
		if (alert != null) 
		{
			Alert alertData = alert.getData();
			Map<String, String> tempMap = alertData.getDetails();
			// Check if the key already exists
			// OpsGenie overwrites existing if the key exists so need to combine what's new
			// and what exists
			if (tempMap.containsKey("Anomalies")) 
			{
				anomalyDetails += tempMap.get("Anomalies") + "\n\n";
			}
		}

		// Add the new anomalies to the string
		for (Iterator<JsonNode> anomalies = input.path("detail").path("anomalies").iterator(); anomalies.hasNext(); ) {
			
			JsonNode anomaly = anomalies.next();
			for (Iterator<JsonNode> sourceDetails = anomaly.path("sourceDetails").iterator(); sourceDetails.hasNext(); ) {
				JsonNode sourceDetail = sourceDetails.next();
				anomalyDetails += String.format(
						"Data Source: %s\n" + 
						"Name: %s\n" + 
						"Stat: %s\n\n",
						sourceDetail.path("dataSource").asText(),
						sourceDetail.path("dataIdentifiers").path("name").asText(),
						sourceDetail.path("dataIdentifiers").path("stat").asText());
			}
		}
		
		// Map needed as argument parameter for .setDetails()
		Map<String, String> details = new HashMap<String, String>();
		details.put("Anomalies", anomalyDetails);
		AddAlertDetailsRequest request = new AddAlertDetailsRequest();
		request.setDetails(details);
		
		// Details are added here
		try {
			opsGenieClient.addDetails(input.path("detail").path("insightId").asText(), request, "alias");
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Anomaly update failed to process due to: \n", e);
			e.printStackTrace();
		}
	}
	
	public static void newRecommendation (JsonNode input, Logger LOGGER, AlertApi opsGenieClient) {
		
		// Retrieve existing anomaly details
		GetAlertResponse alert = null;
		try {
			alert = opsGenieClient.getAlert(input.path("detail").path("insightId").asText(), "alias");
		} catch (ApiException e1) {
			// TODO Auto-generated catch block
			LOGGER.error("Getting the existing alert failed due to: \n", e1);
			e1.printStackTrace();
		}
		
		// String for holding all recommendation details
		String recommendationDetails = "";

		// Check if there are previous recommendations to add
		if (alert != null) 
		{
			Alert alertData = alert.getData();
			Map<String, String> tempMap = alertData.getDetails();
			// Check if the key already exists
			// OpsGenie overwrites existing if the key exists so need to combine what's new
			// and what exists
			if (tempMap.containsKey("Recommendations")) {
				recommendationDetails += tempMap.get("Recommendations") + "\n\n";
			}
		}
		
		// Add the new recommendations
		for (Iterator<JsonNode> it = input.path("detail").path("recommendations").iterator(); it.hasNext(); ) {
			JsonNode recommendation = it.next();
			recommendationDetails += String.format(
					"Name: %s\n" + 
					"Reason: %s\n\n", 
					recommendation.path("name").asText(), 
					recommendation.path("reason").asText());
		}
			
		// Map needed as argument parameter for AddAlertDetailsRequest.setDetails()
		Map<String, String> details = new HashMap<String, String>();
		details.put("Recommendations", recommendationDetails);
		AddAlertDetailsRequest request = new AddAlertDetailsRequest();
		request.setDetails(details);
		
		// Details are added here
		try {
			opsGenieClient.addDetails(input.path("detail").path("insightId").asText(), request, "alias");
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Recommendation update failed to process due to: \n", e);
			e.printStackTrace();
		}
	}

	public static void newSeverity(JsonNode input, Logger LOGGER, AlertApi opsGenieClient) {
		
		// Map needed as argument parameter for AddAlertDetailsRequest.setDetails()
		Map<String, String> details = new HashMap<String, String>();
		details.put("Severity Upgraded", input.path("detail").path("insightSeverity").asText());
		
		AddAlertDetailsRequest request = new AddAlertDetailsRequest();
		request.setDetails(details);
		
		try {
			opsGenieClient.addDetails(input.path("detail").path("insightId").asText(), request, "alias");
		} catch (ApiException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Severity update failed to process due to: \n", e);
			e.printStackTrace();
		}
	}

}