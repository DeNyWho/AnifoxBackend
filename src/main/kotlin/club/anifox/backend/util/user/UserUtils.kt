package club.anifox.backend.util.user

import club.anifox.backend.domain.enums.user.UserIdentifierType
import club.anifox.backend.domain.exception.common.NotFoundException
import club.anifox.backend.jpa.entity.user.UserTable
import club.anifox.backend.jpa.repository.user.UserRepository
import club.anifox.backend.util.TokenHelper
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

    fun checkUserIdentifier(userIdentifier: String): UserIdentifierType {
        return if (isValidEmail(userIdentifier)) UserIdentifierType.EMAIL else UserIdentifierType.LOGIN
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^\\w+([\\.-]?\\w+)*@\\w+([\\.-]?\\w+)*(\\.\\w{2,})+$")
        return emailRegex.matches(email)
    }
}
