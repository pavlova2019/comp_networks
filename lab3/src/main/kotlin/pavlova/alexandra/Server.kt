package pavlova.alexandra

import java.io.File
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread

fun main() {
    val port = 8080
    val server = ServerSocket(port)
    println("The server is running on http://0.0.0.0:${server.localPort}")

    while(true) {
        val client = server.accept()

        thread { ClientHandler(client).run() }
    }
}

class ClientHandler(private val client: Socket) {
    private val writer = client.getOutputStream()
    private val reader = Scanner(client.getInputStream())

    fun run() {
        try {
            val request = reader.nextLine()
            val command = request.split(' ')[1]
            println(request)
            val response = when(command) {
                "/" -> getHealth()
                else -> getFile(command)
            }
            writer.write(response)
            writer.close()
            client.close()
        } catch (e: Exception) {
            println("Error! ${e.message}")
            client.close()
        }
    }

    private fun getFile(fileName: String): ByteArray {
        val file = File(".$fileName")

        var status = ""
        var body = ""
        if (file.exists()) {
            status = "HTTP/1.1 200 OK\r\n"
            body = file.readText()
        } else {
            status = "HTTP/1.1 404 Not Found\r\n"
            body = "<HTML><HEAD><TITLE>Not Found</TITLE></HEAD><BODY>Not Found</BODY></HTML>"
        }

        return StringBuilder().append(status).append("\r\n").append(body)
            .toString().toByteArray(Charset.defaultCharset())
    }

    private fun getHealth(): ByteArray {
        return "HTTP/1.1 200 OK\r\n\r\n".toByteArray(Charset.defaultCharset())
    }
}