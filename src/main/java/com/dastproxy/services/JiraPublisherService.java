package com.dastproxy.services;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.dastproxy.common.constants.AppScanConstants;
import com.dastproxy.common.utils.AppScanUtils;
import com.dastproxy.configuration.RootConfiguration;
import com.dastproxy.model.Issue;
import com.dastproxy.model.IssueVariant;
import com.dastproxy.model.jira.CustomField1;
import com.dastproxy.model.jira.CustomField10;
import com.dastproxy.model.jira.CustomField11;
import com.dastproxy.model.jira.CustomField2;
import com.dastproxy.model.jira.CustomField3;
import com.dastproxy.model.jira.CustomField4;
import com.dastproxy.model.jira.CustomField5;
import com.dastproxy.model.jira.CustomField6;
import com.dastproxy.model.jira.CustomField7;
import com.dastproxy.model.jira.CustomField8;
import com.dastproxy.model.jira.CustomField9;
import com.dastproxy.model.jira.Fields;
import com.dastproxy.model.jira.IssueType;
import com.dastproxy.model.jira.JiraIssueRequest;
import com.dastproxy.model.jira.JiraIssueResponse;
import com.dastproxy.model.jira.Priority;
import com.dastproxy.model.jira.Project;

@Service
public class JiraPublisherService {

	private static final Logger LOGGER = LogManager
			.getLogger(JiraPublisherService.class.getName());

	public JiraIssueResponse publishToJIRAProject(final String projectKey,
			final Issue issueToBePushedToJira, final String userName,
			final String password) throws IOException {

		String description = "Details of the bug are:\n \n" + "Type: {Type}\n"
				+ "Severity: {Severirty}\n" + "Test URL: {Test_URL}\n";

		description = description
				.replace("{Type}", issueToBePushedToJira.getIssueType())
				.replace("{Severirty}", issueToBePushedToJira.getSeverity())
				.replace("{Test_URL}", issueToBePushedToJira.getTestUrl());

		LOGGER.debug(
				"Inside JiraPublisherService.publishToJIRAProject. Using the following username",
				userName);

		final RestTemplate authorisedRestTemplate = new RestTemplate();

		// Create a list for the message converters
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();

		// Add the Jackson Message converter
		
		messageConverters.add(new MappingJackson2HttpMessageConverter());

		authorisedRestTemplate.setMessageConverters(messageConverters);
		authorisedRestTemplate.getMessageConverters().add(new FormHttpMessageConverter());

		final Fields fields = new Fields();

		final Project project = new Project();
		project.setKey(projectKey);
		fields.setProject(project);

		// By default the Issue Id is Bug
		IssueType issueType = new IssueType();
		issueType.setName("Security");
		fields.setIssueType(issueType);

		fields.setDescription(description);
		fields.setSummary(issueToBePushedToJira.getIssueType());

		String[] labels = { "SecToolsAppScan" };
		fields.setLabels(labels);

		// final Reporter reporter = new Reporter();
		// reporter.setName(userName);
		// fields.setReporter(reporter);

		// fields.setDedicatedTeam("Secure Engineering and Network Assessment");
		// fields.setDomain("Information Security");

		Priority priority = new Priority();

		switch (issueToBePushedToJira.getSeverity()) {

		case "High":
			priority.setName("P2");
			break;

		case "Medium":
			priority.setName("P3");
			break;

		case "Low":
			priority.setName("P4");
			break;

		default:
			priority.setName("P4");
			break;
		}

		fields.setPriority(priority);

		CustomField1 customField1 = new CustomField1();

		switch (issueToBePushedToJira.getSeverity()) {

		case "High":
			customField1.setValue("High Risk");
			break;

		case "Medium":
			customField1.setValue("Risk");
			break;

		case "Low":
			customField1.setValue("Low Risk");
			break;

		default:
			customField1.setValue("Low Risk");
			break;
		}
		fields.setCustomField1(customField1);

		CustomField2 customField2 = new CustomField2();
		customField2.setValue("Other");
		fields.setCustomField2(customField2);

		CustomField3 customField3 = new CustomField3();
		customField3.setValue("QA");
		fields.setCustomField3(customField3);

		CustomField4 customField4 = new CustomField4();
		customField4.setValue("Testing");
		fields.setCustomField4(customField4);

		CustomField5 customField5 = new CustomField5();
		customField5.setValue("Web Application");
		fields.setCustomField5(customField5);

		CustomField6 customField6 = new CustomField6();
		customField6.setValue("Internal: Security");
		fields.setCustomField6(customField6);

		CustomField7 customField7 = new CustomField7();
		customField7.setValue("Easy");
		fields.setCustomField7(customField7);

		CustomField8 customField8 = new CustomField8();
		customField8.setValue("Moderate");
		fields.setCustomField8(customField8);

		CustomField9 customField9 = new CustomField9();
		customField9.setValue("Don't Know");
		fields.setCustomField9(customField9);

		CustomField10 customField10 = new CustomField10();
		customField10.setValue("Vulnerability");
		fields.setCustomField10(customField10);

		CustomField11 customField11 = new CustomField11();
		customField11.setValue("Internal: Security Testing");
		fields.setCustomField11(customField11);

		final JiraIssueRequest jiraIssueRequest = new JiraIssueRequest();
		jiraIssueRequest.setFields(fields);

		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION,
				returnAuthHeader(userName, password));

		headers.setContentType(MediaType.APPLICATION_JSON);

		if(LOGGER.isDebugEnabled()){
			List interceptor = new ArrayList();
			interceptor.add(new LoggingRequestInterceptor());

			authorisedRestTemplate.setInterceptors(interceptor);
		}
		
		HttpEntity httpEntity = new HttpEntity(jiraIssueRequest, headers);

		ResponseEntity<JiraIssueResponse> responseEntity = authorisedRestTemplate
				.exchange(
						RootConfiguration.getProperties().getProperty(
								AppScanConstants.PROPERTIES_JIRA_BASE_URL)
								+ "/rest/api/2/issue/", HttpMethod.POST,
						httpEntity, JiraIssueResponse.class);

		final JiraIssueResponse jiraIssueResponse = responseEntity.getBody();

		String payloadVariantTrafficPrefix = jiraIssueResponse.getKey();
		int counter = 1;

		for (IssueVariant issueVariant : issueToBePushedToJira
				.getIssueVariants()) {

			if (AppScanUtils.isNotNull(issueVariant.getTraffic())
					&& AppScanUtils.isNotNull(issueVariant.getTraffic()
							.getOriginalHttpTraffic())
					&& AppScanUtils.isNotNull(issueVariant.getTraffic()
							.getTestHttpTraffic())) {

				File tempTestTrafficFile = File.createTempFile(
						payloadVariantTrafficPrefix
								+ "-testTrafficDataUsingVariant-" + counter,
						".txt");
				byte[] testTrafficData = issueVariant.getTraffic()
						.getTestHttpTraffic().getBytes();
				/*FileOutputStream outputStream = new FileOutputStream(
						tempTestTrafficFile);
				outputStream.write(testTrafficData);
				// Always close files.
				outputStream.close();

				System.out.println(tempTestTrafficFile.getAbsolutePath());
				tempTestTrafficFile.delete();*/
				
				
				
				//HttpHeaders fileUploadHeaders = new HttpHeaders();
				headers.setContentType(MediaType.MULTIPART_FORM_DATA);
				//headers.setContentDispositionFormData(name, filename);
				headers.set("X-Atlassian-Token", "nocheck");
				
				MultiValueMap<String, Object> parts = 
				          new LinkedMultiValueMap<String, Object>();
				  parts.add("file", new ByteArrayResource(testTrafficData));
				  parts.add("name", payloadVariantTrafficPrefix
							+ "-testTrafficDataUsingVariant-" + counter+".txt");
				HttpEntity<MultiValueMap<String, Object>> requestEntity =
				          new HttpEntity<MultiValueMap<String, Object>>(parts, headers);
				
				/*try{
				ResponseEntity<String> response =
						authorisedRestTemplate.exchange(RootConfiguration.getProperties().getProperty(
									AppScanConstants.PROPERTIES_JIRA_BASE_URL)
									+ "/rest/api/2/issue/" +jiraIssueResponse.getKey()+"/attachments", 
				                  HttpMethod.POST, requestEntity, String.class);
				}
				catch(Exception e){
					LOGGER.error(e);
				}*/
				
				counter++;	
			}
		}

		return jiraIssueResponse;
	}

	private String returnAuthHeader(final String username, final String password) {

		String auth = username + ":" + password;
		byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset
				.forName("US-ASCII")));
		String authHeader = "Basic " + new String(encodedAuth);
		return authHeader;

	}
}
