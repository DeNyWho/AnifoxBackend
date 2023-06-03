FROM openjdk:18
COPY ./build/libs/* ./app.jar
COPY ./config/certs/abc.jks /app/config/certs/
COPY ./config/certs/truststore.jks /app/config/certs/
CMD ["java","-jar","app.jar"]
