package com.example.backend.service.anime

import com.example.backend.repository.anime.async.AnimeServiceAsyncTaskInterface
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.AsyncConfigurerSupport
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service


@Service
@EnableAsync
class AnimeServiceAsyncTask : AsyncConfigurerSupport(), AnimeServiceAsyncTaskInterface {

    @Autowired
    private lateinit var animeService: AnimeService

    @Scheduled(fixedRate = 10000) // Запускать метод каждые 10 секунд
    @Async
    override fun longRunningTask() {
        animeService.addTranslationsToDB(listOf(610, 609, 735, 643, 559, 739, 767, 825, 933, 557, 794, 1002))
        animeService.addDataToDB("610, 609, 735, 643, 559, 739, 767, 825, 933, 557, 794, 1002")
    }

    override fun getAsyncExecutor() = ThreadPoolTaskExecutor().apply {
        corePoolSize = 5
        maxPoolSize = 10
        queueCapacity = 25
        initialize()
    }
}