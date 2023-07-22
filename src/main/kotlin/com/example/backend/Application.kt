package com.example.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.transaction.annotation.EnableTransactionManagement


@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableTransactionManagement
@EnableAsync
class Application

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}
