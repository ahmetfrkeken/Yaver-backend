package com.ekenahmetfaruk

import at.favre.lib.crypto.bcrypt.BCrypt
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.util.StringValues
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Serializable
data class RegisterRequest(val email: String, val password: String)

@Serializable
data class UserResponse(val id: Int, val email: String)

@Serializable
data class ErrorResponse(val error: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val token: String, val user: UserResponse)

fun Route.authRoutes(jwtConfig: JwtConfig) {
    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val email = request.email.trim().lowercase()

            if (!email.contains("@") || request.password.length < 8) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Geçerli E-posta ve en az 8 karakterli şifre giriniz")
                )
                return@post
            }

            val alreadyExists = transaction {
                Users.selectAll().where { Users.email eq email }.any()
            }

            if (alreadyExists) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("Bu e-posta zaten kayıtlı"))
                return@post
            }

            val hash = BCrypt.withDefaults().hashToString(12, request.password.toCharArray())

            val user = transaction {
                val newId = Users.insert {
                    it[Users.email] = email
                    it[Users.passwordHash] = hash
                } get Users.id
                UserResponse(newId, email)
            }
            call.respond(HttpStatusCode.Created, user)
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val email = request.email.trim().lowercase()

            val found = transaction {
                Users.selectAll().where { Users.email eq email }.map {
                    Triple(it[Users.id], it[Users.email], it[Users.passwordHash])
                }.singleOrNull()
            }

            val passwordOk = found != null && BCrypt.verifyer().verify(
                request.password.toCharArray(), found.third
            ).verified

            if (found == null || !passwordOk) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("E-posta veya şifre yanlış"))
                return@post
            }

            val token = jwtConfig.createToken(found.first)

            call.respond(LoginResponse(token, UserResponse(found.first, found.second)))
        }

        authenticate("auth-jwt") {
            get("/me") {
                val userId = call.principal<JWTPrincipal>()?.payload?.claims?.get("userId")?.asInt()

                val me = transaction {
                    Users.selectAll().where { Users.id eq (userId ?: 0) }.map {
                        UserResponse(it[Users.id], it[Users.email])
                    }.singleOrNull()
                }

                if(me == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Kullanıcı bulunamadı"))
                } else {
                    call.respond(me)
                }
            }
        }
    }
}