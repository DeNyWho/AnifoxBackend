package club.anifox.backend.controller.account

import club.anifox.backend.service.account.AccountService
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

@RestController
@CrossOrigin("*")
@Tag(name = "AccountApi", description = "All about account")
@PreAuthorize("hasRole('ROLE_USER')")
@RequestMapping("/api/account/")
class AccountController(
    private val accountService: AccountService,
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], name = "avatar")
    fun changeAvatar(
        @RequestHeader(value = "Authorization") token: String,
        @RequestBody image: MultipartFile,
        response: HttpServletResponse,
    ) {
        accountService.changeAvatar(token, image, response)
    }

    @PostMapping("birthday")
    fun changeBirthday(
        @RequestHeader(value = "Authorization") token: String,
        @RequestParam(name = "birthday") newBirthday: LocalDate,
        response: HttpServletResponse,
    ) {
        accountService.changeBirthday(token, newBirthday, response)
    }

    @PostMapping("nickname")
    fun changeNickName(
        @RequestHeader(value = "Authorization") token: String,
        @RequestParam(name = "nickname") newNickName: String,
        response: HttpServletResponse,
    ) {
        accountService.changeNickName(token, newNickName, response)
    }
}
