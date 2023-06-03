package com.example.backend.util.common

import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

fun <T> List<T>.toPage(pageable: Pageable) =
    PageImpl(this.subList(
        pageable.offset.toInt(),
        (if (pageable.offset + pageable.pageSize > this.size) this.size else pageable.offset + pageable.pageSize).toInt()),
        pageable, this.size.toLong()
    )
