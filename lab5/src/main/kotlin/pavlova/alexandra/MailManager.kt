package pavlova.alexandra

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*
import javax.imageio.ImageIO

private fun putIfMissing(props: Properties, key: String, value: String) {
    if (!props.containsKey(key)) {
        props[key] = value
    }
}

fun main(args: Array<String>) {
    val parser = ArgParser("mail")
    val host by parser.option(ArgType.String, description = "Host").default("mail.spbu.ru")
    val port by parser.option(ArgType.Int, description = "Port").default(25)
    val username by parser.option(ArgType.String, description = "Username").required()
    val password by parser.option(ArgType.String, description = "Password").default("8dEq6fzh")

    try {
        parser.parse(args)
        val client = ServerManager(host, port)
        client.run(username, password)
    } catch (e: Exception) {
        println("Error! ${e.message}")
    }
}

class ServerManager(host: String, port: Int) {
    private val socket = Socket(host, port)
    private val reader = Scanner(socket.getInputStream())
    private val writer = socket.getOutputStream()

    fun run(username: String, password: String) {
        try {
            val response = getResponse()
            if (!response.toString().startsWith("220")) {
                throw Exception("Код ответа 220 не получен от сервера")
            }

            // Отправляем команду HELO и получаем ответ сервера.
            sendAndCheckResponse("HELO $username\r\n", 250)

            // Аутентификация пользователя
            sendAndCheckResponse("AUTH LOGIN\r\n", 334)
            sendAndCheckResponse("${toBase64(username)}\r\n", 334)
            sendAndCheckResponse("${toBase64(password)}\r\n", 235)

            // Отправляем команду  MAIL FROM.
            sendAndCheckResponse("MAIL FROM: $username\r\n", 250)

            // Отправляем команду RCPT TO.
            sendAndCheckResponse("RCPT TO: pavlovaalexandra29@gmail.com\r\n", 250)

            // Отправляем команду DATA.
            sendAndCheckResponse("DATA\r\n", 354)

            // Отправляем данные сообщения.
            val message = StringBuilder()
            message.append("From: $username\r\n").append("To: st076358@student.spbu.ru")
                .append("Subject: Test message with Hello\r\n").append("Hi Dear,\r\n")
                .append("This lab is too hard.\r\n")
            sendAndCheckResponse(message.toString(), -1, true)
            sendImageAndCheckResponse(File("./sad_cat.jpg"), -1, true)

            // Завершаем строкой с одной точкой.
            sendAndCheckResponse(".\r\n", 250)

            // Отправляем команду QUIT.
            sendAndCheckResponse("QUIT\r\n")
            println(getResponse())

            close()
        } catch (e: Exception) {
            println("Error! ${e.message}")
            close()
        }
    }

    private fun getResponse(): String? {
        val response = reader.nextLine()
        try {
            val parts = response.split(" ")
            println("RESPONSE: ${parts[0]} ${fromBase64(parts[1])}")
        } catch (e: Exception) {
            println("RESPONSE: $response\n")
        }
        return response
    }

    private fun close() {
        reader.close()
        socket.close()
    }

    private fun sendAndCheckResponse(message: String, code: Int = -1,
                                     noResponse: Boolean = false) {
        print("SENDING: $message")

        writer.write(modifyRequest(message))

        if (noResponse) {
            return
        }

        val response = getResponse()

        if (code != -1 && response != null && !response.toString().startsWith(code.toString())) {
            throw Exception("Код ответа $code не получен от сервера")
        }
    }

    private fun sendImageAndCheckResponse(path: File, code: Int = -1,
                                          noResponse: Boolean = false) {

        print("SENDING: $path")

        val byteArrayWriter = ByteArrayOutputStream()
        val img = ImageIO.read(path)
        ImageIO.write(img, path.extension, byteArrayWriter)
        val size = ByteBuffer.allocate(4).putInt(byteArrayWriter.size()).array()
        writer.write(size);
        writer.write(byteArrayWriter.toByteArray());
        writer.flush();

        if (noResponse) {
            return
        }

        val response = getResponse()

        if (code != -1 && response != null && !response.toString().startsWith(code.toString())) {
            throw Exception("Код ответа $code не получен от сервера")
        }
    }

}

private fun modifyRequest(request: String): ByteArray {
    return request.toByteArray(Charset.defaultCharset())
}

private fun toBase64(request: String): String {
    val enc = Base64.getMimeEncoder() ?: throw Exception()
    return enc.encodeToString(request.toByteArray(Charset.defaultCharset()))
}

private fun fromBase64(request: String): String {
    return String(Base64.getDecoder().decode(request))
}