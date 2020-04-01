package edu.gatech.SmartHub.SmartHubVRDRExporter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan("edu.gatech.SmartHub.SmartHubVRDRExporter.Controller")
@ComponentScan("edu.gatech.SmartHub.SmartHubVRDRExporter.Service")
@ComponentScan("edu.gatech.SmartHub.nightingale.Service")
@SpringBootApplication
public class SmartHubVRDRExporterApplication extends SpringBootServletInitializer{
	public static void main(String[] args) {
        new SpringApplicationBuilder(SmartHubVRDRExporterApplication.class)
        .run(args);
	}
}