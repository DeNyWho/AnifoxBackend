import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.20"
    id("org.springframework.boot") version "2.7.11"
    id("io.spring.dependency-management") version Dependencies.Versions.springDep
    kotlin("jvm") version "1.7.20"
    kotlin("plugin.spring") version Dependencies.Versions.kotlin
}

springBoot {
    mainClass.set("com.example.backend.ApplicationKt")
}
tasks.getByName<Jar>("jar") {
    enabled = false
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven(Dependencies.MultiPlatform.composeMaven)
    maven(Dependencies.MultiPlatform.gradleMaven)
    maven(Dependencies.MultiPlatform.jitpack)
}

dependencies {
    implementation("org.bitbucket.b_c:jose4j:0.9.3")
    implementation("org.springframework.security:spring-security-jwt:1.1.1.RELEASE")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.keycloak:keycloak-core:20.0.0")
    with(Dependencies.Spring.KeyCloak){
        implementation(keycloakAdminClient)
        implementation(keycloakSpring)
    }
    with(Dependencies.Spring.Cache){
        implementation(ehcache)
        implementation(javaxCache)
    }
    with(Dependencies.Spring.Oauth){
        implementation(securityConf)
        implementation(securityOauthCore)
        implementation(securityOauthClient)
        implementation(securityOauthAutoConfigure)
        implementation(jjwtApi)
        runtimeOnly(jjwtImpl)
        runtimeOnly(jjwtJackson)
        implementation(web)
    }
    with(Dependencies.Ktor){
        implementation(clientCore)
        implementation(clientJava)
        implementation(clientLogging)
        implementation(clientJson)
        implementation(json)
        implementation(contentNegotiation)
    }
    with(Dependencies.MultiPlatform){
        implementation(kotlinxSerializationJson)
    }
    with(Dependencies.Spring.Defaults){
        implementation(actuator)
        implementation(web)
        implementation(dataJpa)
        implementation(mail)
        implementation(thymeleaf)
        implementation(migration)
        implementation(starterValidation)
        implementation(security)
        implementation(cache)
        implementation(jwt)
        implementation(mail)
        implementation(webSpr)
        implementation(webMVC)
        implementation(validation)
        implementation(springCore)
        implementation(springСontext)
        runtimeOnly(postgreSQLRun)
    }
    with(Dependencies.Spring.swagger){
        implementation(swaggerUi)
        implementation(swaggerOpenApi)
        implementation(swaggerDataRest)
    }
    with(Dependencies.Spring.ImageIO){
        implementation(bmp)
        implementation(tiff)
        implementation(jpeg)
        implementation(psd)
        implementation(pdf)
        implementation(hdr)
        implementation(servlet)
    }
    with(Dependencies.Spring){
        implementation(logging)
        implementation(skrapeIT)
        implementation(jackson)
        implementation(gson)
        implementation(tomcat)
        implementation(guava)
        implementation(uniRest)
        implementation(commonsIO)
        implementation(commonsText)
        implementation(javax)
        implementation(jakarta)
        implementation(hibernate)
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "18"
    }
    kotlinOptions.jvmTarget = "18"
}
tasks.withType<Test> {
    useJUnitPlatform()
}
