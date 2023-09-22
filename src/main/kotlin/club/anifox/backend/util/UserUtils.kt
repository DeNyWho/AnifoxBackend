package club.anifox.backend.util

import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.jpa.entity.user.UserTable
import club.anifox.backend.jpa.repository.user.UserRepository
import org.springframework.stereotype.Component

@Component
class UserUtils(
    private val tokenHelper: TokenHelper,
    private val userRepository: UserRepository,
) {

    fun checkUser(token: String): UserTable {
        return userRepository.findByLogin(tokenHelper.getTokenInfo(token).preferredUsername!!)
            .orElseThrow { throw NotFoundException("User not found") }
    }
}
