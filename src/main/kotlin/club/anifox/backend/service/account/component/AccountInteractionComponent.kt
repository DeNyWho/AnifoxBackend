package club.anifox.backend.service.account.component

import club.anifox.backend.jpa.repository.user.UserRepository
import club.anifox.backend.service.image.ImageService
import club.anifox.backend.util.mdFive
import club.anifox.backend.util.user.UserUtils
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

@Component
class AccountInteractionComponent {

    @Autowired
    private lateinit var imageService: ImageService

    @Autowired
    private lateinit var userUtils: UserUtils

    @Autowired
    private lateinit var userRepository: UserRepository

    fun changeAvatar(token: String, image: MultipartFile, response: HttpServletResponse) {
        val user = userUtils.checkUser(token)

        user.image = imageService.saveFileInSThird(
            filePath = "images/user/${mdFive(user.login)}/avatar/${mdFive(user.id)}.png",
            data = image.bytes,
            compress = true,
            width = 400,
            height = 400,
            newImage = true,
        )

        userRepository.save(user)
    }

    fun changeNickName(token: String, newNickName: String, response: HttpServletResponse) {
        val user = userUtils.checkUser(token)

        user.nickName = newNickName

        userRepository.save(user)
    }
}
