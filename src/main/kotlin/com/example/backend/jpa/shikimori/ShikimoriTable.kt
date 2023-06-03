package com.example.backend.jpa.shikimori

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "users", schema = "shikimori")
data class ShikimoriUsers(
    @Id
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val username: String = "",

    @Column(nullable = false)
    val image: String = ""

)