package aws.devopsguru.partner.opsgenie;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.ifountain.opsgenie.client.swagger.ApiException;
import com.ifountain.opsgenie.client.swagger.model.AddAlertDetailsRequest;
import com.ifountain.opsgenie.client.swagger.model.Alert;
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest;
import com.ifountain.opsgenie.client.swagger.model.DeleteAlertRequest;
import com.ifountain.opsgenie.client.swagger.model.GetAlertResponse;
import com.ifountain.opsgenie.client.swagger.model.Recipient;
import com.ifountain.opsgenie.client.swagger.model.TeamRecipient;

public class AlertType {

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
	
	public static void newInsight(JsonNode input) {
		
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
		if (Constants.getTeamName() != null && !Constants.getTeamName().trim().isEmpty())
		{
			// If you would like to add more teams, simply add a comma inside the Arrays.asList after the 1st entry
			// and add another new TeamRecipient().name("anotherTeamName")
			opsGenieRequest.setTeams(Arrays.asList(new TeamRecipient().name(Constants.getTeamName())));
			
			// Same logic for setVisibleTo except type cast it like so (Recipient) new TeamRecipient().name()
			opsGenieRequest.setVisibleTo(Collections.singletonList((Recipient) new TeamRecipient().name("TEAM_NAME")));
		}
		else
		{
			Constants.getLogger().warn("TEAM_NAME has not been set properly check enviornment variable in Lambda");
		}
		
		// Null/Empty check for EMAIL
		if (Constants.getEmail() != null && !Constants.getEmail().trim().isEmpty())
		{
			opsGenieRequest.setUser(Constants.getEmail());
		}
		else
		{
			Constants.getLogger().warn("EMAIL has not been set properly check enviornment variable in Lambda");
		}
		
		setOpsGenieSeverity(input.path("detail").path("insightSeverity").asText(), opsGenieRequest);

		// Alert creation happens here after request fields filled out
		try {
			Constants.getOpsGenieClient().createAlert(opsGenieRequest);
		} catch (ApiException e) {
			Constants.getLogger().error("Alert creation failed due to: \n", e);
			e.printStackTrace();
		}
	}
	
	public static void insightClosed (JsonNode input) {
		
		// Identify the alert to delete
		DeleteAlertRequest request = new DeleteAlertRequest();
		request.setIdentifier(input.path("detail").path("insightId").asText());
		request.setIdentifierType(DeleteAlertRequest.IdentifierTypeEnum.ALIAS);

		// Alert deletion happens here
		try {
			Constants.getOpsGenieClient().deleteAlert(request);
		} catch (ApiException e) {
			Constants.getLogger().error("Insight failed to close due to: \n", e);
			e.printStackTrace();
		}
	}
	
	public static void newAssociation(JsonNode input) {
		
		// Retrieve existing anomaly details
		GetAlertResponse alert = null;
		try {
			alert = Constants.getOpsGenieClient().getAlert(input.path("detail").path("insightId").asText(), "alias");
		} catch (ApiException e1) {
			Constants.getLogger().error("Getting the existing alert failed due to: \n", e1);
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
			Constants.getOpsGenieClient().addDetails(input.path("detail").path("insightId").asText(), request, "alias");
		} catch (ApiException e) {
			Constants.getLogger().error("Anomaly update failed to process due to: \n", e);
			e.printStackTrace();
		}
	}
	
	public static void newRecommendation (JsonNode input) {
		
		// Retrieve existing anomaly details
		GetAlertResponse alert = null;
		try {
			alert = Constants.getOpsGenieClient().getAlert(input.path("detail").path("insightId").asText(), "alias");
		} catch (ApiException e1) {
			Constants.getLogger().error("Getting the existing alert failed due to: \n", e1);
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
			Constants.getOpsGenieClient().addDetails(input.path("detail").path("insightId").asText(), request, "alias");
		} catch (ApiException e) {
			Constants.getLogger().error("Recommendation update failed to process due to: \n", e);
			e.printStackTrace();
		}
	}

	public static void newSeverity(JsonNode input) {
		
		// Map needed as argument parameter for AddAlertDetailsRequest.setDetails()
		Map<String, String> details = new HashMap<String, String>();
		details.put("Severity Upgraded", input.path("detail").path("insightSeverity").asText());
		
		AddAlertDetailsRequest request = new AddAlertDetailsRequest();
		request.setDetails(details);
		
		try {
			Constants.getOpsGenieClient().addDetails(input.path("detail").path("insightId").asText(), request, "alias");
		} catch (ApiException e) {
			Constants.getLogger().error("Severity update failed to process due to: \n", e);
			e.printStackTrace();
		}
	}
	
	public static void allFeatures(JsonNode input) {
		
		// All 5 triggers
		if (input.path("detail").path("messageType").asText().equals("NEW_INSIGHT")) {
			AlertType.newInsight(input);
		} else if (input.path("detail").path("messageType").asText().equals("CLOSED_INSIGHT")) {
			AlertType.insightClosed(input);
		} else if (input.path("detail").path("messageType").asText().equals("NEW_ASSOCIATION")) {
			AlertType.newAssociation(input);
		} else if (input.path("detail").path("messageType").asText().equals("NEW_RECOMMENDATION")) {
			AlertType.newRecommendation(input);
		} else if (input.path("detail").path("messageType").asText().equals("SEVERITY_UPGRADED")) {
			AlertType.newSeverity(input);
		} else {
			Constants.getLogger().error("Json not parsed properly or messageType is null/incorrect");
		}
	}

	public static void allFeaturesReactiveInsights(JsonNode input) {
		
		// All 5 triggers but only for reactive insights
		if (input.path("detail").path("messageType").asText().equals("NEW_INSIGHT")) 
		{
			if (input.path("detail").path("insightType").asText().equals("REACTIVE"))
			{
				AlertType.newInsight(input);
			}
		} 
		else if (input.path("detail").path("messageType").asText().equals("CLOSED_INSIGHT")) 
		{
			if (input.path("detail").path("insightType").asText().equals("REACTIVE"))
			{
				AlertType.insightClosed(input);
			}
		} 
		else if (input.path("detail").path("messageType").asText().equals("NEW_ASSOCIATION")) 
		{
			AlertType.newAssociation(input);
		} 
		else if (input.path("detail").path("messageType").asText().equals("NEW_RECOMMENDATION")) 
		{
			AlertType.newRecommendation(input);
		} 
		else if (input.path("detail").path("messageType").asText().equals("SEVERITY_UPGRADED")) 
		{
			if (input.path("detail").path("insightType").asText().equals("REACTIVE"))
			{
				AlertType.newSeverity(input);
			}
		} 
		else 
		{
			Constants.getLogger().error("Json not parsed properly or messageType is null/incorrect");
		}
	}

	public static void allFeaturesProactiveInsights(JsonNode input) {
		
		// All 5 triggers but only for reactive insights
		if (input.path("detail").path("messageType").asText().equals("NEW_INSIGHT")) 
		{
			if (input.path("detail").path("insightType").asText().equals("PROACTIVE"))
			{
				AlertType.newInsight(input);
			}
		} 
		else if (input.path("detail").path("messageType").asText().equals("CLOSED_INSIGHT")) 
		{
			if (input.path("detail").path("insightType").asText().equals("PROACTIVE"))
			{
				AlertType.insightClosed(input);
			}
		} 
		else if (input.path("detail").path("messageType").asText().equals("NEW_ASSOCIATION")) 
		{
			AlertType.newAssociation(input);
		} 
		else if (input.path("detail").path("messageType").asText().equals("NEW_RECOMMENDATION")) 
		{
			AlertType.newRecommendation(input);
		} 
		else if (input.path("detail").path("messageType").asText().equals("SEVERITY_UPGRADED")) 
		{
			if (input.path("detail").path("insightType").asText().equals("PROACTIVE"))
			{
				AlertType.newSeverity(input);
			}
		} 
		else 
		{
			Constants.getLogger().error("Json not parsed properly or messageType is null/incorrect");
		}
	}

	public static void reactiveHighSeverityOnly(JsonNode input) {
		
		// FILTER: Insight Open + Insight Closed
		if (input.path("detail").path("messageType").asText().equals("NEW_INSIGHT")) 
		{
			// Filter only reactive high severity insights
			if (input.path("detail").path("insightSeverity").asText().equals("high") && 
					input.path("detail").path("insightType").asText().equals("REACTIVE"))
			{
				AlertType.newInsight(input);
			}
		} 
		else if (input.path("detail").path("messageType").asText().equals("CLOSED_INSIGHT")) 
		{
			if (input.path("detail").path("insightSeverity").asText().equals("high") && 
					input.path("detail").path("insightType").asText().equals("REACTIVE"))
			{
				AlertType.insightClosed(input);
			}
		}
		
	}
	
	public static void reactiveMediumSeverityOnly(JsonNode input) {
		
		// FILTER: Insight Open + Insight Closed
		if (input.path("detail").path("messageType").asText().equals("NEW_INSIGHT")) 
		{
			// Filter only reactive medium severity insights
			if (input.path("detail").path("insightSeverity").asText().equals("medium") && 
					input.path("detail").path("insightType").asText().equals("REACTIVE"))
			{
				AlertType.newInsight(input);
			}
		} 
		else if (input.path("detail").path("messageType").asText().equals("CLOSED_INSIGHT")) 
		{
			if (input.path("detail").path("insightSeverity").asText().equals("medium") && 
					input.path("detail").path("insightType").asText().equals("REACTIVE"))
			{
				AlertType.insightClosed(input);
			}
		}
		
	}
	
	public static void reactiveLowSeverityOnly(JsonNode input) {
		
		// FILTER: Insight Open + Insight Closed
		if (input.path("detail").path("messageType").asText().equals("NEW_INSIGHT")) 
		{
			// Filter only reactive low severity insights
			if (input.path("detail").path("insightSeverity").asText().equals("low") && 
					input.path("detail").path("insightType").asText().equals("REACTIVE"))
			{
				AlertType.newInsight(input);
			}
		} 
		else if (input.path("detail").path("messageType").asText().equals("CLOSED_INSIGHT")) 
		{
			if (input.path("detail").path("insightSeverity").asText().equals("low") && 
					input.path("detail").path("insightType").asText().equals("REACTIVE"))
			{
				AlertType.insightClosed(input);
			}
		}
		
	}
	
	public static void proactiveHighSeverityOnly(JsonNode input) {
		
		// FILTER: Insight Open + Insight Closed
		if (input.path("detail").path("messageType").asText().equals("NEW_INSIGHT")) 
		{
			// Filter only proactive high severity insights
			if (input.path("detail").path("insightSeverity").asText().equals("high") && 
					input.path("detail").path("insightType").asText().equals("PROACTIVE"))
			{
				AlertType.newInsight(input);
			}
		} 
		else if (input.path("detail").path("messageType").asText().equals("CLOSED_INSIGHT")) 
		{
			if (input.path("detail").path("insightSeverity").asText().equals("high") && 
					input.path("detail").path("insightType").asText().equals("PROACTIVE"))
			{
				AlertType.insightClosed(input);
			}
		}
		
	}
	public static void proactiveMediumSeverityOnly(JsonNode input) {
		
		// FILTER: Insight Open + Insight Closed
		if (input.path("detail").path("messageType").asText().equals("NEW_INSIGHT")) 
		{
			// Filter only proactive medium severity insights
			if (input.path("detail").path("insightSeverity").asText().equals("medium") && 
					input.path("detail").path("insightType").asText().equals("PROACTIVE"))
			{
				AlertType.newInsight(input);
			}
		} 
		else if (input.path("detail").path("messageType").asText().equals("CLOSED_INSIGHT")) 
		{
			if (input.path("detail").path("insightSeverity").asText().equals("medium") && 
					input.path("detail").path("insightType").asText().equals("PROACTIVE"))
			{
				AlertType.insightClosed(input);
			}
		}
		
	}

	public static void proactiveLowSeverityOnly(JsonNode input) {
		
		// FILTER: New Insight + Insight Closed
		if (input.path("detail").path("messageType").asText().equals("NEW_INSIGHT")) 
		{
			// Filter only proactive low severity insights
			if (input.path("detail").path("insightSeverity").asText().equals("low") && 
					input.path("detail").path("insightType").asText().equals("PROACTIVE"))
			{
				AlertType.newInsight(input);
			}
		} 
		else if (input.path("detail").path("messageType").asText().equals("CLOSED_INSIGHT")) 
		{
			if (input.path("detail").path("insightSeverity").asText().equals("low") && 
					input.path("detail").path("insightType").asText().equals("PROACTIVE"))
			{
				AlertType.insightClosed(input);
			}
		}
		
	}
	
	public static void insightOpenAndRecommendations(JsonNode input) {
		
		// FILTER: New Insight + Insight Closed + New Recommendation
		if (input.path("detail").path("messageType").asText().equals("NEW_INSIGHT")) 
		{
			AlertType.newInsight(input);
		} 
		else if (input.path("detail").path("messageType").asText().equals("CLOSED_INSIGHT")) 
		{
			AlertType.insightClosed(input);
		}
		else if (input.path("detail").path("messageType").asText().equals("NEW_RECOMMENDATION")) 
		{
			AlertType.newRecommendation(input);
		}
	}
	
	public static void insightOpenAndAnomalies(JsonNode input) {
	
		// FILTER: New Insight + Insight Closed + New Recommendation
		if (input.path("detail").path("messageType").asText().equals("NEW_INSIGHT")) 
		{
			AlertType.newInsight(input);
		} 
		else if (input.path("detail").path("messageType").asText().equals("CLOSED_INSIGHT")) 
		{
			AlertType.insightClosed(input);
		}
		else if (input.path("detail").path("messageType").asText().equals("NEW_ASSOCIATION")) 
		{
			AlertType.newAssociation(input);
		}
	}
	
	public static void insightOpenAndSeverityUpgraded(JsonNode input) {
		
		// FILTER: New Insight + Insight Closed + New Recommendation
		if (input.path("detail").path("messageType").asText().equals("NEW_INSIGHT")) 
		{
			AlertType.newInsight(input);
		} 
		else if (input.path("detail").path("messageType").asText().equals("CLOSED_INSIGHT")) 
		{
			AlertType.insightClosed(input);
		}
		else if (input.path("detail").path("messageType").asText().equals("SEVERITY_UPGRADED")) 
		{
			AlertType.newSeverity(input);
		}
	}

	public static void insightOpenAnomaliesAndRecommendations(JsonNode input) {
	
		// FILTER: New Insight + Insight Closed + New Recommendation + New Anomalies
		if (input.path("detail").path("messageType").asText().equals("NEW_INSIGHT")) 
		{
			AlertType.newInsight(input);
		} 
		else if (input.path("detail").path("messageType").asText().equals("CLOSED_INSIGHT")) 
		{
			AlertType.insightClosed(input);
		}
		else if (input.path("detail").path("messageType").asText().equals("NEW_ASSOCIATION")) 
		{
			AlertType.newAssociation(input);
		}
		else if (input.path("detail").path("messageType").asText().equals("NEW_RECOMMENDATION")) 
		{
			AlertType.newRecommendation(input);
		}
	}
}