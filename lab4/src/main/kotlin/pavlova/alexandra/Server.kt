package pavlova.alexandra

import java.io.File
import java.io.FileWriter
import java.net.*
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
            println("Client request: $request")
            val commands = request.split(' ')
            val url = commands[1]
            val response = when(commands[0]) {
                "GET" -> when (url) {
                    "/", "" -> getHealth()
                    else -> getCite(url.substring(1))
                }
                else -> modifyRequest("")
            }
            writer.write(response)
            println("Response sent:")
            //println(response.toString(Charset.defaultCharset()))
            writer.close()
            client.close()
        } catch (e: Exception) {
            println("Error! ${e.message}")
            client.close()
        }
    }

    private fun getCite(url: String): ByteArray {
        if (url == "favicon.ico") {
            return modifyRequest("aa")
        }
        println("Connecting to: $url from ${client.inetAddress}")
        val urlHost = url.split("/")[0]

        try {
            val ipHost = InetAddress.getByName(urlHost)
            println("Request resolved: ${ipHost.hostName}")

            val address = ipHost.hostAddress
            println("Request IP address: $address")

            val serverSocket = Socket(ipHost, 80)

            if (serverSocket.isBound) {
                println("Socket connection: OK")
            } else {
                throw Exception("Socket connection: Failed")
            }

            val writerSocket = serverSocket.getOutputStream()
            val readerSocket = Scanner(serverSocket.getInputStream())

            /*var request = url.substringAfter(ipHost.hostName)
            request = if (request.isNotEmpty()) {
                request.substring(1)
            } else {
                "/"
            }*/

            writerSocket.write(modifyRequest("GET http://$url HTTP/1.1\r\n" +
                    "\r\n"))
            println("Request: <GET http://$url HTTP/1.1 Host: ${ipHost.hostName} Connection: close>")
            val res = StringBuilder()
            while (readerSocket.hasNext()) {
                val str = readerSocket.nextLine()
                res.append(str + "\r\n")
            }
            writerSocket.close()
            serverSocket.close()
            return modifyRequest(res.toString())

        } catch (e: Exception) {
            println("Error! ${e.message}")
            val status = "HTTP/1.1 404 Not Found\r\n\r\n"
            val body = "<HTML><HEAD><TITLE>Not Found</TITLE></HEAD><BODY>Not Found</BODY></HTML>\r\n\r\n"
            client.close()

            return modifyRequest(StringBuilder().append(status).append(body).toString())
        }

        /*val file = File(".$url")

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
            .toString().toByteArray(Charset.defaultCharset())*/
    }

    private fun getHealth(): ByteArray {
        return modifyRequest("HTTP/1.1 200 OK\r\n\r\n" +
                "<HTML><HEAD><TITLE>Proxy Server</TITLE></HEAD><BODY>Proxy Server</BODY></HTML>\r\n\r\n")
    }

    private fun modifyRequest(request: String): ByteArray {
        return request.toByteArray(Charset.defaultCharset())
    }
}