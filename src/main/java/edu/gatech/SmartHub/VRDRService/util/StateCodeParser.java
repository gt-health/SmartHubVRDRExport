package edu.gatech.SmartHub.VRDRService.util;

import java.util.HashMap;
import java.util.Map;

import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;

public class StateCodeParser {
	public static Map<String, String> LOINCcodes = new HashMap<String, String>();
	static {
		LOINCcodes.put("US", "000");
		LOINCcodes.put("NewEng", "001");
		LOINCcodes.put("ME", "002");
		LOINCcodes.put("NH", "003");
		LOINCcodes.put("VT", "004");
		LOINCcodes.put("MA", "005");
		LOINCcodes.put("RI", "006");
		LOINCcodes.put("CT", "007");
		LOINCcodes.put("NJ", "008");
		LOINCcodes.put("NY", "011");
		LOINCcodes.put("PA", "014");
		LOINCcodes.put("DE", "017");
		LOINCcodes.put("MD", "021");
		LOINCcodes.put("DC", "022");
		LOINCcodes.put("VA", "023");
		LOINCcodes.put("WV", "024");
		LOINCcodes.put("NC", "025");
		LOINCcodes.put("SC", "026");
		LOINCcodes.put("TN", "031");
		LOINCcodes.put("GA", "033");
		LOINCcodes.put("FL", "035");
		LOINCcodes.put("AL", "037");
		LOINCcodes.put("MS", "039");
		LOINCcodes.put("MI", "041");
		LOINCcodes.put("OH", "043");
		LOINCcodes.put("IN", "045");
		LOINCcodes.put("KY", "050");
		LOINCcodes.put("WI", "051");
		LOINCcodes.put("IA", "053");
		LOINCcodes.put("ND", "054");
		LOINCcodes.put("SD", "055");
		LOINCcodes.put("MT", "056");
		LOINCcodes.put("IL", "061");
		LOINCcodes.put("MO", "063");
		LOINCcodes.put("KS", "065");
		LOINCcodes.put("NE", "067");
		LOINCcodes.put("AR", "071");
		LOINCcodes.put("LA", "073");
		LOINCcodes.put("OK", "075");
		LOINCcodes.put("TX", "077");
		LOINCcodes.put("ID", "081");
		LOINCcodes.put("WY", "082");
		LOINCcodes.put("CO", "083");
		LOINCcodes.put("UT", "084");
		LOINCcodes.put("NV", "085");
		LOINCcodes.put("NM", "086");
		LOINCcodes.put("AZ", "087");
		LOINCcodes.put("AK", "091");
		LOINCcodes.put("WA", "093");
		LOINCcodes.put("OR", "095");
		LOINCcodes.put("CA", "097");
		LOINCcodes.put("HI", "099");
	}
	public static CodeableConcept getStateCodeFromString(String stateCodeString) {
		String stateStringAlone = stateCodeString.split(",")[1].trim();
		return new CodeableConcept(new Coding("https://loinc.org/21842-0/", LOINCcodes.get(stateStringAlone), stateStringAlone));
	}
}
