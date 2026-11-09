package com.ekenahmetfaruk

import kotlinx.serialization.Serializable

@Serializable
data class Task(val id: Int, val title: String, val done: Boolean)