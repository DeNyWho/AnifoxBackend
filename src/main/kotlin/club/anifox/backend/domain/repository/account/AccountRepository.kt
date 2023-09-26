package club.anifox.backend.domain.repository.account

import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.multipart.MultipartFile

interface AccountRepository {
    fun changeAvatar(token: String, image: MultipartFile, response: HttpServletResponse)
    fun changeNickName(token: String, newNickName: String, response: HttpServletResponse)
}
