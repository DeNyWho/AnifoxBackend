**Anifox** is a anime project where people can watch anime. **Open source project** made with ❤️.

## About
The **AniFox** is a project that was created with passion. It all started when a young person wanted to create their own version of an anime application, as every app with anime viewing capabilities had a large amount of advertising. It ended up evolving into a [diploma](https://github.com/DeNyWho/Anifox_Backend/blob/main/diplom/%D0%92%D0%9A%D0%A0.docx) project. This repository contains the second, completely rewritten version of the backend part of the project. All the features of the backend part can be seen on the [website](https://anifox.club/anime). 
## Features
- **Parser** - using scheduled tasks in Spring Boot, new anime data from Shikimori, Jikan, Kitsu, Haglund, Kodik are added to the PostgreSQL database every 12 hours✅
- **Update** - using scheduled tasks in Spring Boot, new anime episodes from Kodik, and episode information from Kitsu and Jikan are added to the database every 1️⃣ hour✅
- Default anime requests: main anime catalog request, details etc✅
- Oauth 2.0 authorization between Keycloak, which is installed on the Anifox server, and the Spring Boot application (works with cookies)✅
- Default user requests: adding a rating to an anime, adding an anime to favorite lists (watching, watched, on plan, postponed)✅
- Default admin requests for animes: adding an anime to the blocked list (users won't see these anime) ✅
- Add anime release schedules ✅
- Add notifications for anime series ❌
- Chat between users ❌
- Users ability to add friends ❌
- Users ability to watch animes with friends ❌
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
## License
This project uses the **Apache** license, the details are written [here](https://github.com/DeNyWho/Anifox_Backend/blob/main/LICENSE)

To contact me, use `denis.akhunov123@gmail.com`
