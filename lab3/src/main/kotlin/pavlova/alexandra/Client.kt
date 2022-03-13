package pavlova.alexandra

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.net.Socket
import java.net.SocketAddress
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread

fun main(args: Array<String>) {
    val parser = ArgParser("client")
    val host by parser.option(ArgType.String, description = "Host").default("0.0.0.0")
    val port by parser.option(ArgType.Int, description = "Port").default(8080)
    val fileName by parser.option(ArgType.String, description = "File name").required()

    try {
        parser.parse(args)
        val client = ServerHandler(host, port)
        client.run(fileName)
    } catch (e: Exception) {
        println("Error! ${e.message}")
    }
}

class ServerHandler(private val host: String, private val port: Int) {
    private val client = Socket(host, port)
    private val writer = client.getOutputStream()
    private val reader = Scanner(client.getInputStream())

    fun run(fileName: String) {
        writer.write("GET /$fileName HTTP/1.1\r\n".toByteArray(Charset.defaultCharset()))
        thread {
            while(reader.hasNext()){
                println(reader.nextLine())
            }
            client.close()
        }
    }
}

fun getFileAddress(host: String, port: Int, fileName: String): String { //http://0.0.0.0:8080/test.xml
    return StringBuilder("http://").append(host).append(":").append(port)
        .append("/").append(fileName).toString()
}