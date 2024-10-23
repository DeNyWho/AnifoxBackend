package club.anifox.backend.domain.repository.account

import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

interface AccountRepository {
    fun changeAvatar(
        token: String,
        image: MultipartFile,
        response: HttpServletResponse,
    )

    fun changeNickName(
        token: String,
        newNickName: String,
        response: HttpServletResponse,
    )

    fun changeBirthday(
        token: String,
        newBirthday: LocalDate,
        response: HttpServletResponse,
    )
}
