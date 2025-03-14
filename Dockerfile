FROM maven:3.8-openjdk-11 AS build

WORKDIR /app
COPY . /app/
RUN mvn clean package

FROM openjdk:11-jre-slim

WORKDIR /app
COPY --from=build /app/target/master-slave-replication-1.0-SNAPSHOT.jar /app/

# Run the application
CMD ["java", "-jar", "master-slave-replication-1.0-SNAPSHOT.jar"]
