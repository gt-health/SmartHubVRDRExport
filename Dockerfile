FROM maven:3.6.0-jdk-8 AS builder
WORKDIR /usr/src/vrdrexport
ADD . .
RUN mvn clean install -DskipTests -f /usr/src/vrdrexport/SmartHubJavaAPI/
RUN mvn clean install -DskipTests -f /usr/src/vrdrexport/VRDR_javalib/
RUN mvn clean install -DskipTests -f /usr/src/vrdrexport/

FROM tomcat:latest
#move the WAR for contesa to the webapps directory
COPY --from=builder /usr/src/vrdrexport/target/SmarthubVRDRExporter-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/SmarthubVRDRExport.war
COPY --from=builder /usr/src/vrdrexport/src/main/resources/* /usr/local/tomcat/src/main/resources/