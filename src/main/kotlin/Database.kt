package com.ekenahmetfaruk

import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

object Tasks : Table("tasks") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val title = varchar("title",255)
    val done = bool("done").default(false)

    override val primaryKey = PrimaryKey(id)
}

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val email = varchar("email",255).uniqueIndex()
    val passwordHash = varchar("passwordHash", 100)

    override val primaryKey = PrimaryKey(id)
}

fun Application.configureDatabase() {
    val config = environment.config

    Database.connect(
        url = config.property("database.url").getString(),
        driver = "org.postgresql.Driver",
        user = config.property("database.user").getString(),
        password = config.property("database.password").getString()
    )

    transaction {
        SchemaUtils.create(Users, Tasks)
    }
}

fun ResultRow.toTask() = Task(this[Tasks.id], this[Tasks.title], this[Tasks.done])