# ---------------------------------------------------------------------------
# build image
FROM maven:3.9.3-eclipse-temurin-11-focal AS builder

WORKDIR /work

COPY pom.xml /work/
RUN mvn -ntp dependency:resolve-plugins
RUN mvn -ntp dependency:resolve

COPY src /work/src

RUN mvn -ntp clean package

# ---------------------------------------------------------------------------
# runtime image (slim, without all those build dependencies)
# see https://hub.docker.com/_/jetty for documentation
FROM jetty:10-jdk11-eclipse-temurin AS jetty

RUN java -jar $JETTY_HOME/start.jar --add-modules=plus

# NOTE: "/work/target/webservices.fcs.fcs_noske_endpoint-1.0-SNAPSHOT.war" might break on version updates,
#       change to "/work/target/webservices.fcs.fcs_noske_endpoint-*.war" or "/work/target/*.war"
COPY --from=builder /work/target/webservices.fcs.fcs_noske_endpoint-1.0-SNAPSHOT.war /var/lib/jetty/webapps/ROOT.war

# ---------------------------------------------------------------------------
