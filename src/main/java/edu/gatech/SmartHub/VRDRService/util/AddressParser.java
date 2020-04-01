package edu.gatech.SmartHub.VRDRService.util;

import org.hl7.fhir.dstu3.model.Address;

public class AddressParser {
	
	public static Address parseAddressFromString(String addressString) {
		String[] commaSeperated = addressString.split(",");
		String line = commaSeperated[0].trim();
		String cityState = "";
		String city = "";
		String state = "";
		String zipCode = "";
		if(commaSeperated.length > 1) {
			cityState = commaSeperated[1];
		}
		if(commaSeperated.length > 2) {
			zipCode = commaSeperated[2];
		}
		if(cityState != "") {
			state = cityState.substring(cityState.length() - 2);
			city = cityState.substring(0, cityState.length() - 2);
		}
		Address address = new Address();
		address.addLine(line);
		address.setCity(city);
		address.setState(state);
		address.setPostalCode(zipCode);
		address.setCountry("United States");
		return address;
	}
}