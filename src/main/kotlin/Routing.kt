package com.ekenahmetfaruk

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.valuesOf
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

fun Application.configureRouting() {
    val jwtConfig = loadJwtConfig()
    routing {
        authRoutes(jwtConfig)
        get("/") {
            call.respondText("Hello, World!")
        }

        get("/health") {
            call.respondText("Yaver'in burada sapasağlam")
        }

        get("/hello/{name}") {
            val name = call.parameters["name"]
            call.respondText("Hello, $name!")
        }

        authenticate("auth-jwt") {
            get("/tasks") {
                val currentUserId = call.userId()
                val tasks = transaction {
                    Tasks.selectAll().where { Tasks.userId eq currentUserId }.map { it.toTask() }
                }
                call.respond(tasks)
            }

            post("/tasks") {
                val currentUserId = call.userId()
                val request = call.receive<NewTask>()

                if (request.title.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Başlık boş olamaz."))
                    return@post
                }

                val task = transaction {
                    val newId = Tasks.insert {
                        it[Tasks.userId] = currentUserId
                        it[Tasks.title] = request.title.trim()
                    } [Tasks.id]

                    Task(newId, request.title.trim(), false)
                }

                call.respond(task)
            }

            patch("/tasks/{id}") {
                val currentUserId = call.userId()
                val taskId = call.parameters["id"]?.toIntOrNull()

                if (taskId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Geçersiz görev numarası"))
                    return@patch
                }

                val request = call.receive<UpdateTask>()
                if (request.title == null && request.done == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Değiştirilecek bir alan gönderin"))
                    return@patch
                }

                val updated = transaction {
                    val count = Tasks.update({
                        (Tasks.id eq taskId) and (Tasks.userId eq currentUserId)
                    }) {
                        request.title?.let { newTitle -> it[Tasks.title] = newTitle }
                        request.done?.let { newDone -> it[Tasks.done] = newDone }
                    }

                    if (count == 0) {
                        null
                    } else {
                        Tasks.selectAll()
                            .where { Tasks.id eq taskId }
                            .map { it.toTask() }
                            .single()
                    }
                }

                if(updated == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Görev bulunamadı"))
                } else {
                    call.respond(updated)
                }
            }

            delete("/tasks/{id}") {
                val currentUserId = call.userId()
                val taskId = call.parameters["id"]?.toIntOrNull()
                if (taskId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Geçersiz görev numarası"))
                    return@delete
                }

                val deleted = transaction {
                    Tasks.deleteWhere {
                        (Tasks.id eq taskId) and (Tasks.userId eq currentUserId)
                    }
                }

                if (deleted == 0) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Görev bulunamadı"))
                } else {
                    call.respond(HttpStatusCode.OK)
                }
            }
        }

        get("/tasks") {
            val tasks = transaction {
                Tasks.selectAll().map { task ->
                    Task(
                        task[Tasks.id],
                        task[Tasks.title],
                        task[Tasks.done],
                    )
                }
            }

            call.respond(tasks)
        }

        post("/tasks") {
            val request = call.receive<NewTask>()
            val task = transaction {
                val newId = Tasks.insert {
                    it[Tasks.title] = request.title
                } get Tasks.id
                Task(newId, request.title, false)
            }
            call.respond(HttpStatusCode.Created, task)
        }

    }
}