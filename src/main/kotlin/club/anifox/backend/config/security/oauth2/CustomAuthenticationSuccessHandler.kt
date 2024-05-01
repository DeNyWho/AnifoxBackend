package club.anifox.backend.config.security.oauth2

import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.security.web.savedrequest.DefaultSavedRequest
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class CustomAuthenticationSuccessHandler : SimpleUrlAuthenticationSuccessHandler() {
    @Throws(IOException::class, ServletException::class)
    override fun onAuthenticationSuccess(request: HttpServletRequest, response: HttpServletResponse?, authentication: Authentication?) {
        val defaultSavedRequest = request.session.getAttribute("SPRING_SECURITY_SAVED_REQUEST") as DefaultSavedRequest
        redirectStrategy.sendRedirect(request, response, defaultSavedRequest.redirectUrl)
    }
}
