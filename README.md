## About
Anifox is an open-source anime watching platform built with ❤️, designed to provide an ad-free experience for anime enthusiasts. This repository contains the backend of the second, completely rewritten version of the Anifox project, initially developed as a diploma project.
Explore the live application on [anifox.club](https://anifox.club)!

## Features
- **Anime Parsing**: Scheduled tasks in Spring Boot fetch new anime data from Shikimori, Jikan, Kitsu, Haglund, and Kodik every 12 hours, stored in a PostgreSQL database. ✅
- **Episode Updates**: Hourly scheduled tasks retrieve new anime episodes and metadata from Kodik, Kitsu, and Jikan. ✅
- **Anime Catalog**: Supports main catalog requests, anime details, and more. ✅
- **Anime characters**: Find out all about your favorite anime character. ✅
- **Anime voice acting**: Watch anime with any of the popular dubbings (JAM, KANSAI Studio, AniDub, AniLibria, Studio Band, Animedia, SHIZA Project, HDrezka Studio, AniMaunt, Amber, Dream Cast). ✅
- **OAuth 2.0 Authentication**: Secure user authentication via Keycloak, integrated with the Spring Boot backend (cookie-based). ✅
- **User features**: Rate anime and add to personal lists (Watching, Watched, Planned, Postponed), saving the point where playback stopped ✅
- **Admin features**: Block specific anime from appearing in user searches. ✅
- **Release Schedules**: Display upcoming anime release schedules. ✅

## Configuration
The AniFox backend requires environment variables to connect to external services, databases, and authentication systems.
````
anime.ko.token=<Kodik API token>;
bucket_name_s3=<Amazon S3 bucket name>;
secret_key_s3=<Amazon S3 secret access key>
domain_s3=<Amazon S3 CDN URL (e.g., https://cdn.domain)>;
spring.datasource.url=<JDBC URL to PostgreSQL (e.g., jdbc:postgresql://localhost:5432/anifox)>;
spring.datasource.username=<PostgreSQL username>;
spring.datasource.password=<PostgreSQL password>;
keycloak.auth-server-url=<Keycloak authentication server URL (e.g., https://keycloak)>;
keycloak.realm=<Keycloak realm name>;
keycloak.resource=<Keycloak client ID>;
keycloak.credentials.secret=<Keycloak client secret>;
keycloak.issuer-uri=<Keycloak realm issuer URL (e.g., https://keycloak/realms/realm-name)>;
key-store=<Path to keystore file (e.g., /path/to/keystore.jks)>;
key-store-password=<Keystore password>;
trust-store=<Path to truststore file (e.g., /path/to/truststore.jks)>;
trust-store-password=<Truststore password>;
spring.profiles.active=<Active profile (e.g., parser, prod, dev)>;
````

## Built With 🛠
- [Kotlin](https://kotlinlang.org/) - is a modern but already mature programming language designed to make developers happier.
- [Spring boot](https://spring.io/projects/spring-boot) - is a Java framework designed to simplify the configuration and deployment of applications, providing a streamlined development experience.
- [Ktor Client](https://ktor.io/docs/welcome.html) - is a library for writing the client-side of HTTP requests in the Ktor framework.
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) - simplify asynchronous programming in Kotlin, making it easier to write non-blocking code for improved application responsiveness.
- [Kotlin serialization](https://kotlinlang.org/docs/serialization.html) - is the process of converting data used by an application to a format that can be transferred over a network or stored in a database or a file.
- [Springdoc](https://springdoc.org/) - java library helps to automate the generation of API documentation using spring boot projects.
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa) - part of the larger Spring Data family, makes it easy to easily implement JPA-based (Java Persistence API) repositories.
- [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/) - is a framework that provides seamless integration of OAuth 2.0 authentication and authorization mechanisms into Spring Boot applications, facilitating secure and standardized user authentication processes.
- [PostgreSQL](https://www.postgresql.org/) - is a powerful, open source object-relational database system with over 10 years of active development that has earned it a strong reputation for reliability, feature robustness, and performance.
- [Keycloak](https://www.keycloak.org/) - open source identity and access management.
- [Amazon S3](https://docs.amazonaws.cn/en_us/AmazonS3/latest/userguide/Welcome.html) - is an object storage service that offers industry-leading scalability, data availability, security, and performance.
- [Shikimori](https://shikimori.one/) - is an online platform dedicated to anime and manga enthusiasts, providing a community-driven database for information, reviews, and discussions about anime series and manga titles.
- [Jikan REST API v4](https://docs.api.jikan.moe/) - is an **Unofficial** MyAnimeList API. It scrapes the website to satisfy the need for a complete API - which MyAnimeList lacks.
- [Kitsu](https://kitsu.io/) -  is an online platform catering to anime and manga enthusiasts, offering a user-friendly database for discovering, tracking, and discussing anime series and manga titles.
- [Haglund](https://arm.haglund.dev/docs) -  a service for mapping Anime IDs.
- [Kodik](https://kodik.online/) -  is a convenient database for searching, tracking and discussing anime episodes, as well as information about anime.
- [Docker](https://www.docker.com/) -  is a platform designed to help developers build, share, and run container applications.

## Contributing

I welcome contributions to Anifox! To contribute, please follow these steps:

1. Fork this repository.
2. Create a feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

## License
This project uses the **Apache** license, the details are written [here](https://github.com/DeNyWho/Anifox_Backend/blob/main/LICENSE)

To contact me, use `denis.akhunov123@gmail.com`
