package com.jetbrains.handson.httpapi

import com.jetbrains.handson.httpapi.models.ItemInfo
import io.ktor.application.Application
import java.util.concurrent.ConcurrentHashMap
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import kotlin.random.Random


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

object ItemStorage {
    val items = ConcurrentHashMap(mapOf(
        "100101" to ItemInfo("lol", "something"),
        "191101" to ItemInfo("other", "aaaaaaa")
    ))
}

fun Application.module(testing: Boolean = false) {

    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/v1/items") {
            if (ItemStorage.items.isEmpty()) {
                throw EmptyItemStorageException()
            }
            call.respond(ItemStorage.items)
        }

        get("/v1/items/{id}") {
            val id = call.parameters["id"] ?: throw EmptyIdException()
            if (!ItemStorage.items.containsKey(id)) {
                throw ItemDoesNotExistException()
            }
            call.respond(mapOf(id to ItemStorage.items[id]))
        }

        post("/v1/items") {
            val info = call.receive<ItemInfo>()
            var id = ""
            do {
                id = Random.nextInt().toString()
            } while (ItemStorage.items.containsKey(id))
            ItemStorage.items[id] = info
            call.respond(mapOf("status" to "ok"))
        }

        put("/v1/items/{id}") {
            val id = call.parameters["id"] ?: throw EmptyIdException()
            val info = call.receive<ItemInfo>()
            ItemStorage.items[id] = info
            call.respond(mapOf("status" to "ok"))
        }

        delete("/v1/items/{id}") {
            val id = call.parameters["id"] ?: throw EmptyIdException()
            if (ItemStorage.items.containsKey(id)) {
                ItemStorage.items.remove(id)
            }
            call.respond(mapOf("status" to "ok"))
        }
    }
}

class ItemDoesNotExistException : RuntimeException("Item doesn't exist")
class EmptyIdException : RuntimeException("Empty id")
class EmptyItemStorageException : RuntimeException("No items found")