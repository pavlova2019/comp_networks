package com.jetbrains.handson.httpapi.models

import kotlinx.serialization.Serializable

@Serializable
data class ItemInfo(var name: String, var description: String)
