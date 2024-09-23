package club.anifox.backend.service.account.component

import club.anifox.backend.domain.enums.anime.parser.CompressAnimeImageType
import club.anifox.backend.jpa.repository.user.UserRepository
import club.anifox.backend.service.image.ImageService
import club.anifox.backend.util.mdFive
import club.anifox.backend.util.user.UserUtils
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
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

        user.image = runBlocking {
            imageService.saveFileInSThird(
                filePath = "images/user/${mdFive(user.id)}/${CompressAnimeImageType.Avatar.path}/${mdFive(user.login)}.${CompressAnimeImageType.Avatar.imageType.textFormat()}",
                data = image.bytes,
                compress = true,
                width = 400,
                height = 400,
                newImage = true,
                type = CompressAnimeImageType.Avatar,
            )
        }

        userRepository.save(user)
    }

    fun changeNickName(token: String, newNickName: String, response: HttpServletResponse) {
        val user = userUtils.checkUser(token)

        user.nickName = newNickName

        userRepository.save(user)
    }
}
