FROM openjdk:18
COPY ./build/libs/* ./app.jar
COPY ./ConfigBackend/config/certs/keystore.pkcs12 /app/config/certs/
COPY ./ConfigBackend/config/certs/truststore.pkcs12 /app/config/certs/
CMD ["java","-jar","app.jar"]
