package com.ekenahmetfaruk

import kotlinx.serialization.Serializable

@Serializable
data class NewTask(val title: String)

@Serializable
data class UpdateTask(val title: String? = null, val done: Boolean? = null)