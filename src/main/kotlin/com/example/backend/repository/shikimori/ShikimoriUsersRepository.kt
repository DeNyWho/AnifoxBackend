package com.example.backend.repository.shikimori

import com.example.backend.jpa.shikimori.ShikimoriUsers
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ShikimoriUsersRepository : JpaRepository<ShikimoriUsers, Long>