FROM openjdk:18
COPY ./build/libs/* ./app.jar
COPY keycloak.json ./keycloak.json
COPY ./ConfigBackend/config/certs/abc.jks /app/config/certs/
COPY ./ConfigBackend/config/certs/truststore.jks /app/config/certs/
CMD ["java","-jar","app.jar"]
