package edu.gatech.SmartHub.SmartHubVRDRExporter.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.dstu3.model.StringType;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Address.AddressUse;
import org.hl7.fhir.dstu3.model.BooleanType;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateTimeType;
import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.HumanName.NameUse;
import org.hl7.fhir.dstu3.model.ListResource.ListEntryComponent;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient.ContactComponent;
import org.hl7.fhir.dstu3.model.Practitioner.PractitionerQualificationComponent;
import org.hl7.fhir.dstu3.model.Procedure.ProcedurePerformerComponent;
import org.hl7.fhir.dstu3.model.Quantity;
import org.hl7.fhir.dstu3.model.Reference;

import edu.gatech.SmartHub.SmartHubAPI.json.Response.model.Response;
import edu.gatech.SmartHub.VRDRService.util.AddressParser;
import edu.gatech.SmartHub.VRDRService.util.DateTimeParser;
import edu.gatech.SmartHub.VRDRService.util.StateCodeParser;
import edu.gatech.VRDR.model.AutopsyPerformedIndicator;
import edu.gatech.VRDR.model.BirthRecordIdentifier;
import edu.gatech.VRDR.model.CauseOfDeathCondition;
import edu.gatech.VRDR.model.CauseOfDeathPathway;
import edu.gatech.VRDR.model.Certifier;
import edu.gatech.VRDR.model.ConditionContributingToDeath;
import edu.gatech.VRDR.model.DeathCertificate;
import edu.gatech.VRDR.model.DeathCertificateDocument;
import edu.gatech.VRDR.model.DeathCertification;
import edu.gatech.VRDR.model.DeathDate;
import edu.gatech.VRDR.model.DeathLocation;
import edu.gatech.VRDR.model.Decedent;
import edu.gatech.VRDR.model.DecedentAge;
import edu.gatech.VRDR.model.DecedentDispositionMethod;
import edu.gatech.VRDR.model.DecedentEducationLevel;
import edu.gatech.VRDR.model.DecedentEmploymentHistory;
import edu.gatech.VRDR.model.DecedentFather;
import edu.gatech.VRDR.model.DecedentMother;
import edu.gatech.VRDR.model.DecedentPregnancy;
import edu.gatech.VRDR.model.DecedentSpouse;
import edu.gatech.VRDR.model.DecedentTransportationRole;
import edu.gatech.VRDR.model.DispositionLocation;
import edu.gatech.VRDR.model.ExaminerContacted;
import edu.gatech.VRDR.model.FuneralHome;
import edu.gatech.VRDR.model.InjuryIncident;
import edu.gatech.VRDR.model.InjuryLocation;
import edu.gatech.VRDR.model.MannerOfDeath;
import edu.gatech.VRDR.model.TobaccoUseContributedToDeath;
import edu.gatech.VRDR.model.util.CommonUtil;
import edu.gatech.VRDR.model.util.DecedentPregnancyUtil;
import edu.gatech.VRDR.model.util.DecedentUtil;
import edu.gatech.VRDR.util.HumanNameParser;

@Service
public class SmartHubToVRDRService {
	public DeathCertificateDocument mapSmartHubToVRDR(Response smartHubResponse) {
		Map<String,String> answerMap = collectAnswersFromResponse(smartHubResponse);
		return createDCD(answerMap);
	}
	
	protected Map<String,String> collectAnswersFromResponse(Response smartHubResponse){
		return smartHubResponse.mapQuestionNamesToValue();
	}
	
	protected DeathCertificateDocument createDCD(Map<String,String> answerMap) {
		DeathCertificateDocument document = new DeathCertificateDocument();
		DeathCertificate deathCertificate = new DeathCertificate();
		List<Resource> contents = new ArrayList<Resource>();
    	contents.add(deathCertificate);
    	Decedent decedent = new Decedent();
    	Reference decedentReference = new Reference(decedent.getId());
    	Certifier certifier = new Certifier();
    	Reference certifierReference = new Reference(certifier.getId());
    	contents.add(decedent);
    	contents.add(certifier);
    	//Funeral Director group
    	if(answerMap.keySet().contains("DECEDENT'S LEGAL NAME")) {
    		HumanName name = HumanNameParser.createHumanName(answerMap.get("DECEDENT'S LEGAL NAME"));
    		name.setUse(NameUse.OFFICIAL);
    		decedent.addName(name);
    	}
    	if(answerMap.keySet().contains("SEX")) {
    		if(answerMap.get("SEX").equalsIgnoreCase("Male")) {
    			decedent.setGender(AdministrativeGender.MALE);
    			decedent.setBirthSex("M", "Male");	
    		}
    		else if(answerMap.get("SEX").equalsIgnoreCase("Female")) {
    			decedent.setGender(AdministrativeGender.FEMALE);
    			decedent.setBirthSex("F", "Female");
    		}
    		else {
    			decedent.setGender(AdministrativeGender.OTHER);
    			decedent.setBirthSex("O", "Other");
    		}
    	}
    	else {
    		decedent.setGender(AdministrativeGender.UNKNOWN);
    		decedent.setBirthSex("UNK", "Unknown");
    	}
    	if(answerMap.keySet().contains("SOCIAL SECURITY NUMBER")) {
    		Identifier identifier = new Identifier();
    		identifier.setSystem("http://hl7.org/fhir/sid/us-ssn");
    		identifier.setValue(answerMap.get("SOCIAL SECURITY NUMBER"));
    		CodeableConcept typeCC = new CodeableConcept();
    		typeCC.addCoding(new Coding("http://hl7.org/fhir/v2/0203","SB", ""));
    		identifier.setType(typeCC);
    		decedent.addIdentifier(identifier);
    	}
    	if(answerMap.keySet().contains("AGE")) {
    		DecedentAge decedentAge = new DecedentAge();
    		decedentAge.setValue(new Quantity(Double.parseDouble(answerMap.get("AGE"))).setUnit("a"));
    		contents.add(decedentAge);
    	}
    	if(answerMap.keySet().contains("DATE OF BIRTH") && answerMap.keySet().contains("BIRTHPLACE")) {
    		DateTimeType birthYear = DateTimeParser.parseDate(answerMap.get("DATE OF BIRTH"));
    		CodeableConcept stateCode = StateCodeParser.getStateCodeFromString(answerMap.get("BIRTHPLACE"));
    		BirthRecordIdentifier birthRecordIdentifier = new BirthRecordIdentifier(answerMap.get("DATE OF BIRTH"), stateCode, birthYear);
        	birthRecordIdentifier.setSubject(decedentReference);
        	contents.add(birthRecordIdentifier);
    	}
    	Set<String> residentAddressKeys = new HashSet(Arrays.asList("RESIDENT STREET AND NUMBER","RESIDENT COUNTY","RESIDENT STATE","RESIDENT APT NO","RESIDENT CITY OR TOWN","RESIDENT ZIP CODE"));
    	if(!Sets.intersection(answerMap.keySet(), residentAddressKeys).isEmpty()) {
    		Address residentAddress = new Address();
    		if(answerMap.keySet().contains("RESIDENT STREET AND NUMBER")) {
    			residentAddress.addLine(answerMap.get("RESIDENT STREET AND NUMBER"));
    		}
    		if(answerMap.keySet().contains("RESIDENT APT NO")) {
    			residentAddress.addLine(answerMap.get("RESIDENT APT NO"));
    		}
    		if(answerMap.keySet().contains("RESIDENT COUNTY")) {
    			residentAddress.setDistrict(answerMap.get("RESIDENT COUNTY"));
    		}
    		if(answerMap.keySet().contains("RESIDENT CITY OR TOWN")) {
    			residentAddress.setCity(answerMap.get("RESIDENT CITY OR TOWN"));
    		}
    		if(answerMap.keySet().contains("RESIDENT STATE")) {
    			residentAddress.setState(answerMap.get("RESIDENT STATE"));
    		}
    		if(answerMap.keySet().contains("RESIDENT ZIP CODE")) {
    			residentAddress.setPostalCode(answerMap.get("RESIDENT ZIP CODE"));
    		}
    		if(answerMap.keySet().contains("INSIDE CITY LIMITS")){
    			Extension withinCityLimits = new Extension();
    			withinCityLimits.setUrl(DecedentUtil.addressWithinCityLimitsIndicatorExtensionURL);
    			if(answerMap.get("INSIDE CITY LIMITS").equalsIgnoreCase("True") || answerMap.get("INSIDE CITY LIMITS").equalsIgnoreCase("Yes")) {
    				withinCityLimits.setValue(new BooleanType(true));
    			}
    			else {
    				withinCityLimits.setValue(new BooleanType(false));
    			}
    			residentAddress.addExtension(withinCityLimits);
        	}
    		decedent.addAddress(residentAddress);
    	}
    	DecedentEmploymentHistory decedentEmploymentHistory = new DecedentEmploymentHistory();
    	boolean decedentEmploymentHistoryUsed = false;
    	if(answerMap.keySet().contains("EVER IN US ARMED FORCES?")){
    		if(answerMap.get("EVER IN US ARMED FORCES?").equalsIgnoreCase("Yes")) {
    			decedentEmploymentHistory.addMilitaryService(CommonUtil.yesCode);
    			decedentEmploymentHistoryUsed = true;
    		}
    		else if(answerMap.get("EVER IN US ARMED FORCES?").equalsIgnoreCase("No")) {
    			decedentEmploymentHistory.addMilitaryService(CommonUtil.noCode);
    			decedentEmploymentHistoryUsed = true;
    		}
    	}
    	if(answerMap.keySet().contains("MARITAL STATUS AT TIME OF DEATH")) {
    		CodeableConcept maritalStatusConcept = new CodeableConcept();
    		Coding maritalStatusCoding = new Coding();
    		maritalStatusConcept.addCoding(maritalStatusCoding);
    		maritalStatusCoding.setSystem("http://terminology.hl7.org/CodeSystem/v3-MaritalStatus");
    		if(answerMap.get("MARITAL STATUS AT TIME OF DEATH").equalsIgnoreCase("Married")) {
    			maritalStatusCoding.setCode("M").setDisplay("Married");
    		}
    		else if(answerMap.get("MARITAL STATUS AT TIME OF DEATH").equalsIgnoreCase("Widowed")) {
    			maritalStatusCoding.setCode("W").setDisplay("Widowed");
    		}
    		else if(answerMap.get("MARITAL STATUS AT TIME OF DEATH").equalsIgnoreCase("Divorced")) {
    			maritalStatusCoding.setCode("D").setDisplay("Divorced");
    		}
    		else if(answerMap.get("MARITAL STATUS AT TIME OF DEATH").equalsIgnoreCase("Never_Married")) {
    			maritalStatusCoding.setCode("S").setDisplay("Never Married");
    		}
    		else if(answerMap.get("MARITAL STATUS AT TIME OF DEATH").equalsIgnoreCase("Married_But_Seperated")) {
    			maritalStatusCoding.setCode("M").setDisplay("Married");
    		}
    	}
    	if(answerMap.keySet().contains("DECEDENT'S EDUCATION")) {
    		DecedentEducationLevel decedentEducationLevel = new DecedentEducationLevel();
    		CodeableConcept eduCode = new CodeableConcept();
    		Coding eduCoding = new Coding();
    		eduCoding.setSystem("http://www.hl7.org/fhir/ValueSet/v3-EducationLevel");
    		if(answerMap.get("DECEDENT'S EDUCATION").equalsIgnoreCase("8th grade or less")) {
    			eduCoding.setCode("ELEM").setDisplay("Elementary School");
    		}
    		else if(answerMap.get("DECEDENT'S EDUCATION").equalsIgnoreCase("9-12th grade (no diploma)")) {
    			eduCoding.setCode("SEC").setDisplay("Some secondary or high school education");
    		}
    		else if(answerMap.get("DECEDENT'S EDUCATION").equalsIgnoreCase("High school graduate or GED")) {
    			eduCoding.setCode("HS").setDisplay("High School or secondary school degree complete");
    		}
    		else if(answerMap.get("DECEDENT'S EDUCATION").equalsIgnoreCase("Some college credit but no degree")) {
    			eduCoding.setCode("SCOL").setDisplay("Some College education");
    		}
			else if(answerMap.get("DECEDENT'S EDUCATION").equalsIgnoreCase("Associate's degree")) {
				eduCoding.setCode("ASSOC").setDisplay("Associate's or technical degree complete");
			}
			else if(answerMap.get("DECEDENT'S EDUCATION").equalsIgnoreCase("Bachelor's degree")) {
				eduCoding.setCode("BD").setDisplay("College or baccalaureate degree complete");
			}
			else if(answerMap.get("DECEDENT'S EDUCATION").equalsIgnoreCase("Master's degree")) {
				eduCoding.setCode("GD").setDisplay("Graduate or professional degree complete");
			}
			else if(answerMap.get("DECEDENT'S EDUCATION").equalsIgnoreCase("Doctorate")) {
				eduCoding.setCode("POSTG").setDisplay("Doctoral or post graduate education");
			}
    		eduCode.addCoding(eduCoding);
    		decedentEducationLevel.setCode(eduCode);
    	}
    	if(answerMap.keySet().contains("DECEDENT OF HISPANIC ORIGIN?")) {
    		if(answerMap.get("DECEDENT OF HISPANIC ORIGIN?").equalsIgnoreCase("No")) {
    			decedent.addEthnicity("2186-5", "", "Not Hispanic or latino");
    		}
    		else if(answerMap.get("DECEDENT OF HISPANIC ORIGIN?").equalsIgnoreCase("Yes (Mexican American, Chicano)")) {
    			decedent.addEthnicity("2135-2", "2149-3", "Mexican American");
    		}
    		else if(answerMap.get("DECEDENT OF HISPANIC ORIGIN?").equalsIgnoreCase("Yes (Puerto Rican)")) {
    			decedent.addEthnicity("2135-2", "2180-8	", "Puerto Rican");
    		}
    		else if(answerMap.get("DECEDENT OF HISPANIC ORIGIN?").equalsIgnoreCase("Yes (Cuban)")) {
    			decedent.addEthnicity("2135-2", "2180-8	", "Puerto Rican");
    		}
    		else if(answerMap.get("DECEDENT OF HISPANIC ORIGIN?").equalsIgnoreCase("Yes (other Spanish/Hispanic/Latino)")) {
    			decedent.addEthnicity("2135-2", "", "Hispanic or latino");
    		}
    	}
    	if(answerMap.keySet().contains("DECEDENT'S RACE")) {
    		
    		if(answerMap.get("DECEDENT'S RACE").equalsIgnoreCase("White")) {
    			decedent.addRace("2106-3", "21206-3", "White");
    		}
    		else if(answerMap.get("DECEDENT'S RACE").equalsIgnoreCase("Black or African American")) {
    			decedent.addRace("2054-5", "2054-5", "Black or African American");
    		}
    		else if(answerMap.get("DECEDENT'S RACE").equalsIgnoreCase("American Indian or Alaska Native")) {
    			decedent.addRace("2076-8", "2076-8	", "American Indian or Alaska Native");
    		}
    		else if(answerMap.get("DECEDENT'S RACE").equalsIgnoreCase("Asian Indian")) {
    			decedent.addRace("2028-9", "2029-7", "Asian Indian");
    		}
    		else if(answerMap.get("DECEDENT'S RACE").equalsIgnoreCase("Chinese")) {
    			decedent.addRace("2028-9", "2034-7", "Chinese");
    		}
    		else if(answerMap.get("DECEDENT'S RACE").equalsIgnoreCase("Filipino")) {
    			decedent.addRace("2028-9", "2036-2", "Filipino");
    		}
    		else if(answerMap.get("DECEDENT'S RACE").equalsIgnoreCase("Japanese")) {
    			decedent.addRace("2028-9", "2039-6", "Japanese");
    		}
    		else if(answerMap.get("DECEDENT'S RACE").equalsIgnoreCase("Korean")) {
    			decedent.addRace("2028-9", "2040-4", "Korean");
    		}
    		else if(answerMap.get("DECEDENT'S RACE").equalsIgnoreCase("Other Asian")) {
    			decedent.addRace("2028-9", "", "Other Asian");
    		}
    		else if(answerMap.get("DECEDENT'S RACE").equalsIgnoreCase("Native Hawaiian")) {
    			decedent.addRace("2076-8", "2079-2", "Native Hawaiian");
    		}
    		else if(answerMap.get("DECEDENT'S RACE").equalsIgnoreCase("Guamanian or Chemorro")) {
    			decedent.addRace("2076-8", "2086-7", "Guamanian or Chemorro");
    		}
    		else if(answerMap.get("DECEDENT'S RACE").equalsIgnoreCase("Samoan")) {
    			decedent.addRace("2076-8", "2080-0", "Samoan");
    		}
    		else if(answerMap.get("DECEDENT'S RACE").equalsIgnoreCase("Other Pacific Islander")) {
    			decedent.addRace("2076-8", "2500-7", "Other Pacific Islander");
    		}
    		else if(answerMap.get("DECEDENT'S RACE").equalsIgnoreCase("Other")) {
    			decedent.addRace("2135-2", "2313-1", "Other");
    		}
    	}
    	if(answerMap.keySet().contains("DECEDENT'S USUAL OCCUPATION")) {
    		decedentEmploymentHistoryUsed = true;
    		decedentEmploymentHistory.addUsualOccupation(new CodeableConcept().setText(answerMap.get("DECEDENT'S USUAL OCCUPATION")));
    	}
    	if(answerMap.keySet().contains("KIND OF BUSINESS/INDUSTRY")) {
    		decedentEmploymentHistoryUsed = true;
    		decedentEmploymentHistory.addUsualIndustry(new CodeableConcept().setText(answerMap.get("KIND OF BUSINESS/INDUSTRY")));
    	}
    	
    	if(answerMap.keySet().contains("SURVIVING SPOUSE'S NAME")) {
    		DecedentSpouse decedentSpouse = new DecedentSpouse();
    		decedentSpouse.addName(HumanNameParser.createHumanName(answerMap.get("SURVIVING SPOUSE'S NAME")));
    		decedentSpouse.setPatient(decedentReference);
    		contents.add(decedentSpouse);
    	}
    	if(answerMap.keySet().contains("FATHER'S NAME")) {
    		DecedentFather decedentFather = new DecedentFather();
    		decedentFather.addName(HumanNameParser.createHumanName(answerMap.get("FATHER'S NAME")));
    		decedentFather.setPatient(decedentReference);
    		contents.add(decedentFather);
    	}
    	if(answerMap.keySet().contains("MOTHER'S NAME")) {
    		DecedentMother decedentMother = new DecedentMother();
    		decedentMother.addName(HumanNameParser.createHumanName(answerMap.get("MOTHER'S NAME")));
    		decedentMother.setPatient(decedentReference);
    		contents.add(decedentMother);
    	}
    	if(answerMap.keySet().contains("INFORMANT'S NAME")) {
    		ContactComponent informant = new ContactComponent();
    		informant.setName(HumanNameParser.createHumanName(answerMap.get("INFORMANT'S NAME")));
    		CodeableConcept relationshipType = new CodeableConcept();
			Coding relationshipCoding = new Coding();
			relationshipCoding.setSystem("http://hl7.org/fhir/v2/0131");
    		if(answerMap.keySet().contains("INFORMANT'S RELATION TO DECEDENT")) {
    			if(answerMap.get("INFORMANT'S RELATION TO DECEDENT").equalsIgnoreCase("Emergency Contact")) {
    				relationshipCoding.setCode("C");
    				relationshipCoding.setDisplay("Emergency Contact");
    			}
    			else if(answerMap.get("INFORMANT'S RELATION TO DECEDENT").equalsIgnoreCase("Next-of-Kin")) {
    				relationshipCoding.setCode("N");
    				relationshipCoding.setDisplay("Next-of-Kin");
    			}
    			else if(answerMap.get("INFORMANT'S RELATION TO DECEDENT").equalsIgnoreCase("Employer")) {
    				relationshipCoding.setCode("E");
    				relationshipCoding.setDisplay("Employer");
    			}
    			else if(answerMap.get("INFORMANT'S RELATION TO DECEDENT").equalsIgnoreCase("State Agency")) {
    				relationshipCoding.setCode("S");
    				relationshipCoding.setDisplay("State Agency");
    			}
    			else if(answerMap.get("INFORMANT'S RELATION TO DECEDENT").equalsIgnoreCase("Federal Agency")) {
    				relationshipCoding.setCode("F");
    				relationshipCoding.setDisplay("Federal Agency");
    			}
    			else if(answerMap.get("INFORMANT'S RELATION TO DECEDENT").equalsIgnoreCase("Insurance Company")) {
    				relationshipCoding.setCode("I");
    				relationshipCoding.setDisplay("Insurance Company");
    			}
    			else if(answerMap.get("INFORMANT'S RELATION TO DECEDENT").equalsIgnoreCase("Other")) {
    				relationshipCoding.setCode("O");
    				relationshipCoding.setDisplay("Other");
    			}
    		}
    		else {
    			relationshipCoding.setCode("U");
				relationshipCoding.setDisplay("Unknown");
    		}
    		relationshipType.addCoding(relationshipCoding);
    		informant.addRelationship(relationshipType);
    		decedent.addContact(informant);
    	}
    	
    	DeathDate deathDate = new DeathDate();
    	DateTimeType deathDateDT = new DateTimeType();
    	boolean deathDateSet = false;
    	if(answerMap.keySet().contains("DATE PRONOUNCED DEAD") || answerMap.keySet().contains("TIME PRONOUNCED DEAD")) {
    		deathDateSet = true;
    		String datePronouncedDead = answerMap.keySet().contains("DATE PRONOUNCED DEAD") ? answerMap.get("DATE PRONOUNCED DEAD") : "";
    		String timePronouncedDead = answerMap.keySet().contains("TIME PRONOUNCED DEAD") ? answerMap.get("TIME PRONOUNCED DEAD") : "";
    		deathDateDT = DateTimeParser.parseDateTime(datePronouncedDead, timePronouncedDead);
    		decedent.setDeceased(deathDateDT);
    		deathDate.addDatePronouncedDead(deathDateDT);
    		contents.add(deathDate);
    	}
    	//TODO: Actual and presumed death date doesn't seem to be captured in VRDR....
    	if(answerMap.keySet().contains("CERTIFIER NAME")) {
    		certifier.addName(HumanNameParser.createHumanName(answerMap.get("CERTIFIER NAME")).setUse(NameUse.OFFICIAL));
    	}
    	if(answerMap.keySet().contains("TITLE OF CERTIFIER") && answerMap.keySet().contains("LICENSE NUMBER")) {
    		certifier.addQualification(answerMap.get("LICENSE NUMBER"), "National provider identifier", answerMap.get("TITLE OF CERTIFIER"));
    	}
    	Set<String> certifierAddressKeys = new HashSet(Arrays.asList("CERTIFIER ADDRESS STREET AND NUMBER","CERTIFIER ADDRESS COUNTY","CERTIFIER ADDRESS STATE","CERTIFIER ADDRESS CITY OR TOWN","CERTIFIER ADDRESS ZIP CODE"));
    	if(!Sets.intersection(answerMap.keySet(), certifierAddressKeys).isEmpty()) {
    		Address certifierAddress = new Address();
    		if(answerMap.keySet().contains("CERTIFIER ADDRESS STREET AND NUMBER")) {
    			certifierAddress.addLine(answerMap.get("RESIDENT STREET AND NUMBER"));
    		}
    		if(answerMap.keySet().contains("CERTIFIER ADDRESS COUNTY")) {
    			certifierAddress.setDistrict(answerMap.get("CERTIFIER ADDRESS COUNTY"));
    		}
    		if(answerMap.keySet().contains("CERTIFIER ADDRESS CITY OR TOWN")) {
    			certifierAddress.setCity(answerMap.get("CERTIFIER ADDRESS CITY OR TOWN"));
    		}
    		if(answerMap.keySet().contains("CERTIFIER ADDRESS STATE")) {
    			certifierAddress.setState(answerMap.get("CERTIFIER ADDRESS STATE"));
    		}
    		if(answerMap.keySet().contains("CERTIFIER ADDRESS ZIP CODE")) {
    			certifierAddress.setPostalCode(answerMap.get("CERTIFIER ADDRESS ZIP CODE"));
    		}
    		certifier.addAddress(certifierAddress);
    	}
    	
    	if(answerMap.keySet().contains("CERTIFIER TYPE")) {
    		DeathCertification deathCertification = new DeathCertification();
    		CodeableConcept certifierTypeCode = new CodeableConcept();
    		Coding certifierTypeCoding = new Coding();
    		certifierTypeCoding.setSystem("http://snomed.info/sct");
    		if(answerMap.get("CERTIFIER TYPE").equalsIgnoreCase("Certifying Physician")) {
    			certifierTypeCoding.setCode("434651000124107");
    			certifierTypeCoding.setDisplay("Physician certified death certificate");
    		}
    		else if(answerMap.get("CERTIFIER TYPE").equalsIgnoreCase("Pronouncing & Certifying physician")) {
    			certifierTypeCoding.setCode("434641000124105");
    			certifierTypeCoding.setDisplay("Physician certified and pronounced death certificate");
    		}
    		else if(answerMap.get("CERTIFIER TYPE").equalsIgnoreCase("Medical Examiner/Coroner")) {
    			certifierTypeCoding.setCode("440051000124108");
    			certifierTypeCoding.setDisplay("Medical Examiner");
    		}
    		certifierTypeCode.addCoding(certifierTypeCoding);
    		ProcedurePerformerComponent procedurePerformerComponent = new ProcedurePerformerComponent();
    		procedurePerformerComponent.setRole(certifierTypeCode);
    		procedurePerformerComponent.setActor(certifierReference);
    		deathCertification.addPerformer(procedurePerformerComponent);
    		if(deathDateSet) {
    			deathCertification.setPerformed(deathDateDT);
    		}
    		deathCertificate.addEvent(deathCertification);
    		contents.add(deathCertification);
    	}
    	
    	DeathLocation deathLocation = new DeathLocation();
    	boolean deathLocationUsed = false;
    	if(answerMap.keySet().contains("PLACE OF DEATH")) {
    		deathLocationUsed = true;
    		CodeableConcept locationType = new CodeableConcept();
    		CodeableConcept physicalType = new CodeableConcept();
    		Coding locationTypeCoding = new Coding();
    		locationTypeCoding.setSystem("http://www.hl7.org/fhir/ValueSet/v3-ServiceDeliveryLocationRoleType");
    		Coding physicalTypeCoding = new Coding();
    		physicalTypeCoding.setSystem("http://www.hl7.org/fhir/ValueSet/location-physical-type");
    		locationType.addCoding(locationTypeCoding);
    		physicalType.addCoding(physicalTypeCoding);
    		if(answerMap.get("PLACE OF DEATH").equalsIgnoreCase("Inpatient")) {
    			locationTypeCoding.setCode("INLAB");
    			locationTypeCoding.setDisplay("Inpatient laboratoty");
    			physicalTypeCoding.setCode("Wa");
    			physicalTypeCoding.setDisplay("Ward");
    		}
    		else if(answerMap.get("PLACE OF DEATH").equalsIgnoreCase("Emergency Room/Outpatient")) {
    			locationTypeCoding.setCode("ER");
    			locationTypeCoding.setDisplay("Emergency Room");
    			physicalTypeCoding.setCode("Wa");
    			physicalTypeCoding.setDisplay("Ward");
    		}
    		else if(answerMap.get("PLACE OF DEATH").equalsIgnoreCase("Dead on Arrival at Hospital")) {
    			locationTypeCoding.setCode("HOSP");
    			locationTypeCoding.setDisplay("hospital");
    			physicalTypeCoding.setCode("Wa");
    			physicalTypeCoding.setDisplay("Ward");
    		}
    		else if(answerMap.get("PLACE OF DEATH").equalsIgnoreCase("Hospice Facility")) {
    			locationTypeCoding.setCode("COMM");
    			locationTypeCoding.setDisplay("Community Location");
    			physicalTypeCoding.setCode("Bu");
    			physicalTypeCoding.setDisplay("Building");
    		}
    		else if(answerMap.get("PLACE OF DEATH").equalsIgnoreCase("Nursing Home/LTC Facility")) {
    			locationTypeCoding.setCode("NCCF");
    			locationTypeCoding.setDisplay("	Nursing or custodial care facility");
    			physicalTypeCoding.setCode("Wa");
    			physicalTypeCoding.setDisplay("Ward");
    		}
    		else if(answerMap.get("PLACE OF DEATH").equalsIgnoreCase("Decendent's Home")) {
    			locationTypeCoding.setCode("PTRES");
    			locationTypeCoding.setDisplay("Patient's Residence");
    			physicalTypeCoding.setCode("Bu");
    			physicalTypeCoding.setDisplay("Building");
    		}
    		deathLocation.setType(locationType);
    		deathLocation.setPhysicalType(physicalType);
    	}
    	Set<String> facilityAddressKeys = new HashSet(Arrays.asList("FACILITY NAME", "FACILITY OF DEATH STREET AND NUMBER","FACILITY COUNTY","FACILITY STATE","FACILITY CITY OR TOWN","FACILITY ZIP CODE"));
    	if(!Sets.intersection(answerMap.keySet(), facilityAddressKeys).isEmpty()) {
    		deathLocationUsed = true;
    		Address facilityAddress = new Address();
    		if(answerMap.keySet().contains("FACILITY OF DEATH STREET AND NUMBER")) {
    			facilityAddress.addLine(answerMap.get("RESIDENT STREET AND NUMBER"));
    		}
    		if(answerMap.keySet().contains("FACILITY COUNTY")) {
    			facilityAddress.setDistrict(answerMap.get("FACILITY COUNTY"));
    		}
    		if(answerMap.keySet().contains("FACILITY CITY OR TOWN")) {
    			facilityAddress.setCity(answerMap.get("FACILITY CITY OR TOWN"));
    		}
    		if(answerMap.keySet().contains("FACILITY STATE")) {
    			facilityAddress.setState(answerMap.get("FACILITY STATE"));
    		}
    		if(answerMap.keySet().contains("FACILITY ZIP CODE")) {
    			facilityAddress.setPostalCode(answerMap.get("FACILITY ZIP CODE"));
    		}
    		deathLocation.setAddress(facilityAddress);
    	}
    	if(answerMap.keySet().contains("METHOD OF DISPOSITION")) {
    		DecedentDispositionMethod decedentDispositionMethod = new DecedentDispositionMethod();
    		CodeableConcept dispositionCode = new CodeableConcept();
    		Coding dispositionCoding = new Coding();
    		dispositionCoding.setSystem("http://snomed.info/sct");
    		if(answerMap.get("METHOD OF DISPOSITION").equalsIgnoreCase("Burial")) {
    			dispositionCoding.setCode("449971000124106");
    			dispositionCoding.setDisplay("Patient status determination, deceased and buried (finding)");
    		}
    		else if(answerMap.get("METHOD OF DISPOSITION").equalsIgnoreCase("Cremation")) {
    			dispositionCoding.setCode("449961000124104");
    			dispositionCoding.setDisplay("Patient status determination, deceased and body cremated (finding)");
    		}
    		else if(answerMap.get("METHOD OF DISPOSITION").equalsIgnoreCase("Donation")) {
    			dispositionCoding.setCode("449951000124101");
    			dispositionCoding.setDisplay("Patient status determination, deceased and body donated (finding)");
    		}
    		else if(answerMap.get("METHOD OF DISPOSITION").equalsIgnoreCase("Entombment")) {
    			dispositionCoding.setCode("449931000124108");
    			dispositionCoding.setDisplay("Patient status determination, deceased and entombed (finding)");
    		}
    		else if(answerMap.get("METHOD OF DISPOSITION").equalsIgnoreCase("Removal_From_State")) {
    			dispositionCoding.setCode("449941000124103");
    			dispositionCoding.setDisplay("Patient status determination, deceased and removed from state (finding)");
    		}
    		else{
    			dispositionCoding.setCode("None");
    			dispositionCoding.setDisplay("Other");
    		}
    		dispositionCode.addCoding(dispositionCoding);
    		decedentDispositionMethod.setCode(dispositionCode);
    		decedentDispositionMethod.setSubject(decedentReference);
    		contents.add(decedentDispositionMethod);
    	}
    	
    	Set<String> dispositionAddressKeys = new HashSet(Arrays.asList("NAME OF PLACE OF DISPOSITION", "PLACE OF DISPOSITION STREET AND NUMBER","PLACE OF DISPOSITION COUNTY","PLACE OF DISPOSITION CITY OR TOWN","PLACE OF DISPOSITION STATE","PLACE OF DISPOSITION ZIP CODE"));
    	if(!Sets.intersection(answerMap.keySet(), dispositionAddressKeys).isEmpty()) {
    		DispositionLocation dispositionLocation = new DispositionLocation();
    		if(answerMap.keySet().contains("NAME OF PLACE OF DISPOSITION")) {
        		dispositionLocation.setName(answerMap.get("NAME OF PLACE OF DISPOSITION"));
        	}
    		Address dispositionAddress = new Address();
    		if(answerMap.keySet().contains("PLACE OF DISPOSITION STREET AND NUMBER")) {
    			dispositionAddress.addLine(answerMap.get("PLACE OF DISPOSITION AND NUMBER"));
    		}
    		if(answerMap.keySet().contains("PLACE OF DISPOSITION COUNTY")) {
    			dispositionAddress.setDistrict(answerMap.get("PLACE OF DISPOSITION COUNTY"));
    		}
    		if(answerMap.keySet().contains("PLACE OF DISPOSITION CITY OR TOWN")) {
    			dispositionAddress.setCity(answerMap.get("PLACE OF DISPOSITION CITY OR TOWN"));
    		}
    		if(answerMap.keySet().contains("PLACE OF DISPOSITION STATE")) {
    			dispositionAddress.setState(answerMap.get("PLACE OF DISPOSITION STATE"));
    		}
    		if(answerMap.keySet().contains("PLACE OF DISPOSITION ZIP CODE")) {
    			dispositionAddress.setPostalCode(answerMap.get("PLACE OF DISPOSITION ZIP CODE"));
    		}
    		dispositionLocation.setAddress(dispositionAddress);
    		contents.add(dispositionLocation);
    	}
    	
    	Set<String> funeralAddressKeys = new HashSet(Arrays.asList("NAME OF FUNERAL FACILITY", "FUNERAL FACILITY STREET AND NUMBER","FUNERAL FACILITY COUNTY","FUNERAL FACILITY CITY OR TOWN","FUNERAL FACILITY STATE","FUNERAL FACILITY ZIP CODE"));
    	if(!Sets.intersection(answerMap.keySet(), funeralAddressKeys).isEmpty()) {
    		FuneralHome funeralHome = new FuneralHome();
    		Address funeralAddress = new Address();
    		if(answerMap.keySet().contains("NAME OF FUNERAL FACILITY")) {
    			funeralHome.setName(answerMap.get("NAME OF FUNERAL FACILITY"));
        	}
    		Address dispositionAddress = new Address();
    		if(answerMap.keySet().contains("FUNERAL FACILITY STREET AND NUMBER")) {
    			funeralAddress.addLine(answerMap.get("RESIDENT STREET AND NUMBER"));
    		}
    		if(answerMap.keySet().contains("FUNERAL FACILITY COUNTY")) {
    			funeralAddress.setDistrict(answerMap.get("FUNERAL FACILITY COUNTY"));
    		}
    		if(answerMap.keySet().contains("FUNERAL FACILITY CITY OR TOWN")) {
    			funeralAddress.setCity(answerMap.get("FUNERAL FACILITY CITY OR TOWN"));
    		}
    		if(answerMap.keySet().contains("FUNERAL FACILITY STATE")) {
    			funeralAddress.setState(answerMap.get("FUNERAL FACILITY STATE"));
    		}
    		if(answerMap.keySet().contains("FUNERAL FACILITY ZIP CODE")) {
    			funeralAddress.setPostalCode(answerMap.get("FUNERAL FACILITY ZIP CODE"));
    		}
    		funeralHome.addAddress(dispositionAddress);
    		contents.add(funeralHome);
    	}
    	
    	if(answerMap.keySet().contains("WAS MEDICAL EXAMINER OR CORONER CONTACTED?")) {
    		ExaminerContacted examinerContacted = new ExaminerContacted();
    		if(answerMap.get("WAS MEDICAL EXAMINER OR CORONER CONTACTED?").equalsIgnoreCase("Yes")) {
    			examinerContacted.setValue(new BooleanType(true));
    		}
    		else {
    			examinerContacted.setValue(new BooleanType(false));
    		}
    		contents.add(examinerContacted);
    	}
    	
    	if(answerMap.keySet().contains("MANNER OF DEATH")) {
    		MannerOfDeath mannerOfDeath = new MannerOfDeath();
    		CodeableConcept modCode = new CodeableConcept();
    		Coding modCoding = new Coding();
    		modCoding.setSystem("http://www.hl7.org/fhir/stu3/valueset-MannerTypeVS");
    		if(answerMap.get("MANNER OF DEATH").equalsIgnoreCase("Natural")) {
    			modCoding.setCode("38605008");
    			modCoding.setDisplay("Natural");
    		}
    		else if(answerMap.get("MANNER OF DEATH").equalsIgnoreCase("Homicide")) {
    			modCoding.setCode("27935005");
    			modCoding.setDisplay("Homicide");
    		}
    		else if(answerMap.get("MANNER OF DEATH").equalsIgnoreCase("Accident")) {
    			modCoding.setCode("7878000");
    			modCoding.setDisplay("Accident");
    		}
    		else if(answerMap.get("MANNER OF DEATH").equalsIgnoreCase("Accident")) {
    			modCoding.setCode("7878000");
    			modCoding.setDisplay("Accident");
    		}
    		else if(answerMap.get("MANNER OF DEATH").equalsIgnoreCase("Suicide")) {
    			modCoding.setCode("44301001");
    			modCoding.setDisplay("Suicide");
    		}
    		else if(answerMap.get("MANNER OF DEATH").equalsIgnoreCase("Pending Investigation")) {
    			modCoding.setCode("185973002");
    			modCoding.setDisplay("Pending Investigation");
    		}
    		else {
    			modCoding.setCode("65037004");
    			modCoding.setDisplay("Could not be determined");
    		}
    		modCode.addCoding(modCoding);
    		mannerOfDeath.setCode(modCode);
    		contents.add(mannerOfDeath);
    	}
    	CauseOfDeathPathway causeOfDeathPathway = new CauseOfDeathPathway();
    	boolean causeOfDeathPathwayUsed = false;
    	//TODO: Find a way to map plainstext into cause of death coded
    	if(answerMap.keySet().contains("CAUSE OF DEATH")) {
    		causeOfDeathPathwayUsed = true;
    		CauseOfDeathCondition causeOfDeath = new CauseOfDeathCondition();
    		causeOfDeath.setCode(new CodeableConcept().addCoding(new Coding("","",answerMap.get("CAUSE OF DEATH"))));
    		causeOfDeath.setSubject(decedentReference);
    		causeOfDeath.setAsserter(certifierReference);
    		causeOfDeathPathway.addEntry(new ListEntryComponent().setItem(new Reference(causeOfDeath.getId())));
    		contents.add(causeOfDeath);
    	}
    	List<String> contribCauseOfDeathKeys = Arrays.asList("CONTRIBUTING CAUSE OF DEATH 1","CONTRIBUTING CAUSE OF DEATH 2","CONTRIBUTING CAUSE OF DEATH 3","CONTRIBUTING CAUSE OF DEATH 4","CONTRIBUTING CAUSE OF DEATH 5");
    	for(String contribCauseOfDeathKey : contribCauseOfDeathKeys) {
    		if(answerMap.keySet().contains(contribCauseOfDeathKey)) {
    			causeOfDeathPathwayUsed = true;
	    		ConditionContributingToDeath causeOfDeath = new ConditionContributingToDeath();
	    		causeOfDeath.setCode(new CodeableConcept().addCoding(new Coding("","",answerMap.get(contribCauseOfDeathKey))));
	    		causeOfDeath.setSubject(decedentReference);
	    		causeOfDeath.setAsserter(certifierReference);
	    		causeOfDeathPathway.addEntry(new ListEntryComponent().setItem(new Reference(causeOfDeath.getId())));
	    		contents.add(causeOfDeath);
	    	}
    	}
    	if(answerMap.keySet().contains("WAS AN AUTOPSY PERFORMED?")) {
    		AutopsyPerformedIndicator autopsyPerformedIndicator = new AutopsyPerformedIndicator();
    		if(answerMap.get("WAS AN AUTOPSY PERFORMED?").equalsIgnoreCase("Yes")) {
    			autopsyPerformedIndicator.addAutopsyResultsAvailableComponent(new CodeableConcept().addCoding(new Coding("http://terminology.www.hl7.org/CodeSystem/v2-0136","Y","Yes")));
    		}
    		else{
    			autopsyPerformedIndicator.addAutopsyResultsAvailableComponent(new CodeableConcept().addCoding(new Coding("http://terminology.www.hl7.org/CodeSystem/v2-0136","N","No")));
    		}
    		autopsyPerformedIndicator.setSubject(decedentReference);
    		contents.add(autopsyPerformedIndicator);
    	}
    	if(answerMap.keySet().contains("DID TOBACCO USE CONTRIBUTE TO DEATH?")) {
    		TobaccoUseContributedToDeath tobaccoUseContributedToDeath = new TobaccoUseContributedToDeath();
    		if(answerMap.get("DID TOBACCO USE CONTRIBUTE TO DEATH?").equalsIgnoreCase("Yes")) {
    			tobaccoUseContributedToDeath.setValue("Yes");
    		}
    		else{
    			tobaccoUseContributedToDeath.setValue("No");
    		}
    		tobaccoUseContributedToDeath.setSubject(decedentReference);
    		contents.add(tobaccoUseContributedToDeath);
    	}
    	if(answerMap.keySet().contains("IF FEMALE:")) {
    		DecedentPregnancy decedentPregnancy = new DecedentPregnancy();
    		if(answerMap.get("IF FEMALE:").equalsIgnoreCase("Not pregnant within past year")) {
    			decedentPregnancy.setCode(DecedentPregnancyUtil.VALUE_NOCODE);
    		}
    		else if(answerMap.get("IF FEMALE:").equalsIgnoreCase("Pregnant at time of death")) {
    			decedentPregnancy.setCode(DecedentPregnancyUtil.VALUE_YESCODE);
    		}
    		else if(answerMap.get("IF FEMALE:").equalsIgnoreCase("Not pregnant but within 42 days of death")) {
    			decedentPregnancy.setCode(DecedentPregnancyUtil.VALUE_42DAYSCODE);
    		}
    		else if(answerMap.get("IF FEMALE:").equalsIgnoreCase("Not pregnant but within 43 days to 1 year of death")) {
    			decedentPregnancy.setCode(DecedentPregnancyUtil.VALUE_1YEARCODE);
    		}
    		else if(answerMap.get("IF FEMALE:").equalsIgnoreCase("Unknown if pregnant within the past year")) {
    			decedentPregnancy.setCode(DecedentPregnancyUtil.VALUE_UNKNOWNCODE);
    		}
    		else if(answerMap.get("IF FEMALE:").equalsIgnoreCase("Not Applicable")) {
    			decedentPregnancy.setCode(DecedentPregnancyUtil.VALUE_NACODE);
    		}
    		decedentPregnancy.setSubject(decedentReference);
    		contents.add(decedentPregnancy);
    	}
    	
    	InjuryIncident injuryIncident = new InjuryIncident();
    	DateTimeType injuryDT = new DateTimeType();
    	boolean injuryIncidentUsed = false;
    	if(answerMap.keySet().contains("DATE OF INJURY") || answerMap.keySet().contains("TIME OF INJURY")) {
    		injuryIncidentUsed = true;
    		String datePronouncedDead = answerMap.keySet().contains("DATE OF INJURY") ? answerMap.get("DATE OF INJURY") : "";
    		String timePronouncedDead = answerMap.keySet().contains("TIME OF INJURY") ? answerMap.get("TIME OF INJURY") : "";
    		deathDateDT = DateTimeParser.parseDateTime(datePronouncedDead, timePronouncedDead);
    		injuryIncident.setEffective(injuryDT);
    	}
    	if(answerMap.keySet().contains("INJURY AT WORK?")) {
    		injuryIncidentUsed = true;
    		if(answerMap.get("INJURY AT WORK?").equalsIgnoreCase("Yes")) {
    			injuryIncident.addInjuredAtWorkBooleanComponent(CommonUtil.yesCode);
    		}
    		else if(answerMap.get("INJURY AT WORK?").equalsIgnoreCase("No")) {
    			injuryIncident.addInjuredAtWorkBooleanComponent(CommonUtil.noCode);
    		}
    	}
    	Set<String> injuryLocationKeys = new HashSet(Arrays.asList("PLACE OF INJURY (e.g., Decedent's home; construction site; restaurant; wooded area)", "PLACE OF INJURY STREET AND NUMBER","PLACE OF INJURY COUNTY","PLACE OF INJURY CITY OR TOWN","PLACE OF INJURY STATE","PLACE OF INJURY ZIP CODE"));
    	if(!Sets.intersection(answerMap.keySet(), funeralAddressKeys).isEmpty()) {
    		injuryIncidentUsed = true;
    		InjuryLocation injuryLocation = new InjuryLocation();
    		Address injuryAddress = new Address();
    		if(answerMap.keySet().contains("PLACE OF INJURY (e.g., Decedent's home; construction site; restaurant; wooded area)")) {
    			injuryLocation.setName(answerMap.get("NAME OF PLACE OF INJURY"));
        	}
    		Address dispositionAddress = new Address();
    		if(answerMap.keySet().contains("PLACE OF INJURY STREET AND NUMBER")) {
    			injuryAddress.addLine(answerMap.get("RESIDENT STREET AND NUMBER"));
    		}
    		if(answerMap.keySet().contains("PLACE OF INJURY COUNTY")) {
    			injuryAddress.setDistrict(answerMap.get("PLACE OF INJURY COUNTY"));
    		}
    		if(answerMap.keySet().contains("PLACE OF INJURY CITY OR TOWN")) {
    			injuryAddress.setCity(answerMap.get("PLACE OF INJURY CITY OR TOWN"));
    		}
    		if(answerMap.keySet().contains("PLACE OF INJURY STATE")) {
    			injuryAddress.setState(answerMap.get("PLACE OF INJURY STATE"));
    		}
    		if(answerMap.keySet().contains("PLACE OF INJURY ZIP CODE")) {
    			injuryAddress.setPostalCode(answerMap.get("PLACE OF INJURY ZIP CODE"));
    		}
    		injuryLocation.setAddress(dispositionAddress);
    		contents.add(injuryLocation);
    	}
    	if(answerMap.keySet().contains("DESCRIBE HOW INJURY OCCURRED:")) {
    		injuryIncidentUsed = true;
    		injuryIncident.setValue(new StringType(answerMap.get("DESCRIBE HOW INJURY OCCURRED:")));
    	}
    	if(answerMap.keySet().contains("IF TRANSPORTATION INJURY, SPECIFY:")) {
    		injuryIncidentUsed = true;
    		DecedentTransportationRole decedentTransportationRole = new DecedentTransportationRole();
    		CodeableConcept trCode = new CodeableConcept();
    		Coding trCoding = new Coding();
    		trCode.addCoding(trCoding);
    		trCoding.setSystem("http://snomed.info/sct");
    		if(answerMap.get("IF TRANSPORTATION INJURY, SPECIFY:?").equalsIgnoreCase("Driver/Operator")) {
    			trCoding.setCode("236320001").setDisplay("Vehicle driver");
    		}
    		else if(answerMap.get("IF TRANSPORTATION INJURY, SPECIFY:?").equalsIgnoreCase("Passenger")) {
    			trCoding.setCode("257500003").setDisplay("Passenger");
    		}
    		else if(answerMap.get("IF TRANSPORTATION INJURY, SPECIFY:?").equalsIgnoreCase("Pedestrian")) {
    			trCoding.setCode("257518000").setDisplay("Pedestrian");
    		}
    		else if(answerMap.get("IF TRANSPORTATION INJURY, SPECIFY:?").equalsIgnoreCase("Other")) {
    			trCoding.setCode("394841004").setDisplay("Other");
    		}
    		injuryIncident.addtransportationRelationshipComponent(trCode);
    		decedentTransportationRole.setCode(trCode);
    		decedentTransportationRole.setSubject(decedentReference);
    		contents.add(decedentTransportationRole);
    	}
    	if(answerMap.keySet().contains("FOR REGISTRAR ONLY- DATE FILED (Mo/Day/Yr)")) {
    		deathCertificate.setDate(DateTimeParser.parseDateObjectOnly(answerMap.get("FOR REGISTRAR ONLY- DATE FILED (Mo/Day/Yr)")));
    	}
    	if(causeOfDeathPathwayUsed) {
    		contents.add(causeOfDeathPathway);
    	}
    	if(decedentEmploymentHistoryUsed) {
    		contents.add(decedentEmploymentHistory);
    	}
    	if(deathLocationUsed) {
    		contents.add(deathLocation);
    	}
    	if(injuryIncidentUsed) {
    		contents.add(injuryIncident);
    	}
    	
    	deathCertificate.setSubject(decedentReference);
    	deathCertificate.addAttester(certifier, new Date());
    	for(Resource resource:contents) {
    		CommonUtil.addSectionEntry(deathCertificate, resource);
    		CommonUtil.addBundleEntry(document,resource);
    	}
		return document;
	}
}