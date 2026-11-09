package com.ekenahmetfaruk

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import java.util.Date

class JwtConfig(
    secret: String,
    val issuer: String,
    val audience: String,
    val realm: String
) {
    private val algorithm = Algorithm.HMAC256(secret)

    val verifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun createToken(userId: Int): String = JWT.create()
        .withIssuer(issuer)
        .withAudience(audience)
        .withClaim("userId", userId)
        .withExpiresAt(Date(System.currentTimeMillis() + 3600 * 24 * 1000))
        .sign(algorithm)
}

fun Application.loadJwtConfig(): JwtConfig {
    val config = environment.config

    return  JwtConfig(
        secret = config.property("jwt.secret").getString(),
        issuer = config.property("jwt.issuer").getString(),
        audience = config.property("jwt.audience").getString(),
        realm = config.property("jwt.realm").getString()
    )
}

fun Application.configureSecurity() {
    val jwtConfig = loadJwtConfig()

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtConfig.realm
            verifier(jwtConfig.verifier)
            validate { credential ->
                if (credential.payload.getClaim("userId").asInt() != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                call.respond (
                    HttpStatusCode.Unauthorized,
                    ErrorResponse("Geçersiz veya süresi dolmuş token")
                )
            }
        }
    }
}

fun ApplicationCall.userId(): Int = principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asInt() ?: 0