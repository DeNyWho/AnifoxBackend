package club.anifox.backend.service.account

import club.anifox.backend.domain.repository.account.AccountRepository
import club.anifox.backend.service.account.component.AccountInteractionComponent
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

@Service
class AccountService(
    private val accountInteractionComponent: AccountInteractionComponent,
) : AccountRepository {

    override fun changeAvatar(token: String, image: MultipartFile, response: HttpServletResponse) {
        return accountInteractionComponent.changeAvatar(token = token, image = image, response = response)
    }

    override fun changeBirthday(token: String, newBirthday: LocalDate, response: HttpServletResponse) {
        return accountInteractionComponent.changeBirthday(token = token, newBirthday = newBirthday, response = response)
    }

    override fun changeNickName(token: String, newNickName: String, response: HttpServletResponse) {
        return accountInteractionComponent.changeNickName(token = token, newNickName = newNickName, response = response)
    }
}
