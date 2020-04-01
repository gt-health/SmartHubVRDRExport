package edu.gatech.SmartHub.SmartHubVRDRExporter.Controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import edu.gatech.SmartHub.SmartHubAPI.json.Response.model.Response;
import edu.gatech.SmartHub.SmartHubVRDRExporter.Service.SmartHubToVRDRService;
import edu.gatech.SmartHub.nightingale.Service.NightingaleValidationService;
import edu.gatech.VRDR.model.DeathCertificateDocument;

@CrossOrigin
@RestController
public class VRDRExportController {
	private Logger log = LoggerFactory.getLogger(VRDRExportController.class);
	private SmartHubToVRDRService smartHubToVRDRService;
	private NightingaleValidationService nightingaleValidationService;
	private IParser jsonParser;
	
	@Autowired
	public VRDRExportController(SmartHubToVRDRService smartHubToVRDRService,NightingaleValidationService nightingaleValidationService) {
		super();
		this.smartHubToVRDRService = smartHubToVRDRService;
		this.nightingaleValidationService = nightingaleValidationService;
		this.jsonParser = FhirContext.forDstu3().newJsonParser();
	}
	
	@RequestMapping(value = "VRDR", method = RequestMethod.POST)
	public ResponseEntity<String> convertToVRDR(@RequestBody Response smartHubActivity) throws JsonProcessingException, IOException{
		DeathCertificateDocument vrdrDoc = smartHubToVRDRService.mapSmartHubToVRDR(smartHubActivity);
		String jsonString = jsonParser.encodeResourceToString(vrdrDoc);
		JsonNode jsonVRDR = new ObjectMapper().readTree(jsonString);
		JsonNode issues = nightingaleValidationService.validateVRDRAgainstNightingale(jsonString);
		ObjectNode returnJson = JsonNodeFactory.instance.objectNode();
		returnJson.set("VRDR", jsonVRDR);
		returnJson.set("issues", issues);
		HttpHeaders responseHeaders = new HttpHeaders();
	    responseHeaders.set("Content-Type", 
	      "application/json");
	    return ResponseEntity.ok().headers(responseHeaders).body(new ObjectMapper().writeValueAsString(returnJson));
	}
}