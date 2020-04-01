package edu.gatech.SmartHub.VRDRService.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.hl7.fhir.dstu3.model.DateTimeType;

public class DateTimeParser {
	
	public static Date parseDateTimeObjectOnly(String dateString, String timeString) {
		String DEFAULT_PATTERN = "MM-dd-yyyy'T'HH:mm aa";
		String allTogether = dateString + "T" + timeString;
		DateFormat formatter = new SimpleDateFormat(DEFAULT_PATTERN);
		Date dateObject = null;
		try {
			dateObject = formatter.parse(allTogether);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dateObject;
	}
	public static DateTimeType parseDateTime(String dateString, String timeString) {
		Date dateObject = parseDateTimeObjectOnly(dateString,timeString);
		DateTimeType dtType = new DateTimeType(dateObject);
		return dtType;
	}
	
	public static Date parseDateObjectOnly(String dateString) {
		String DEFAULT_PATTERN = "MM-dd-yyyy";

		DateFormat formatter = new SimpleDateFormat(DEFAULT_PATTERN);
		Date dateObject = null;
		try {
			dateObject = formatter.parse(dateString);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dateObject;
	}
	
	public static DateTimeType parseDate(String dateString) {
		Date dateObject = parseDateObjectOnly(dateString);
		DateTimeType dtType = new DateTimeType(dateObject);
		return dtType;
	}
}
