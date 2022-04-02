package pavlova.alexandra

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream
import java.nio.charset.Charset
import java.security.Security
import java.security.cert.X509Certificate
import java.util.*
import javax.imageio.ImageIO
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

fun main(args: Array<String>) {
    val parser = ArgParser("mail")
    val host by parser.option(ArgType.String, description = "Host").default("smtp.gmail.com")
    val port by parser.option(ArgType.Int, description = "Port").default(465)
    val username by parser.option(ArgType.String, description = "Username").required()
    val password by parser.option(ArgType.String, description = "Password").required()

    try {
        parser.parse(args)
        val client = ServerManager(host, port)
        client.run(username, password)
    } catch (e: Exception) {
        println("Error! ${e.message}")
    }
}

class ServerManager(host: String, port: Int) {
    private val sslContext: SSLContext = SSLContext.getInstance("TLS")
    private val socket: SSLSocket
    private val reader: Scanner
    private val writer: OutputStream

    init {
        Security.setProperty("crypto.policy", "limited")
        System.setProperty("javax.net.debug", "ssl:handshake")
        System.setProperty("jdk.tls.namedGroups", "secp256r1, secp384r1, secp521r1, secp160k1")
        System.setProperty("javax.net.debug", "ssl:handshake")
        System.setProperty("jdk.tls.client.enableStatusRequestExtension", "false")
        System.setProperty("jsse.enableFFDHEExtension", "false")
        System.setProperty("jdk.tls.client.protocols", "TLSv1.1,TLSv1.2")

        sslContext.init(null, arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) {
                //println("Skip trust check for: " + x509Certificates[0])
            }

            override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) {
                //println("Skip trust check for: " + x509Certificates[0])
            }

            override fun getAcceptedIssuers(): Array<X509Certificate?> {
                return arrayOfNulls(0)
            }
        }), null)
        this.socket = sslContext.socketFactory.createSocket(host, port) as SSLSocket
        println("###")
        socket.useClientMode = true
        socket.addHandshakeCompletedListener {
                evt -> println("Handshake completed: ${evt.session.protocol} - ${evt.session.cipherSuite}, ${evt.socket}")
        }
        socket.startHandshake()
        this.reader = Scanner(socket.inputStream)
        this.writer = socket.outputStream
        println("Socket connection: " + socket.isConnected)
    }

    fun run(username: String, password: String, name: String = "aaa") {
        try {
            println(socket.remoteSocketAddress)
            val response = getResponse()
            if (!response.toString().startsWith("220")) {
                throw Exception("Код ответа 220 не получен от сервера")
            }

            // Отправляем команду HELO и получаем ответ сервера.
            sendAndCheckResponse("HELO $name\r\n", 250)

            // Разбираемся с SSL
            //sendAndCheckResponse("STARTTLS\r\n", 220)

            // Аутентификация пользователя
            sendAndCheckResponse("AUTH LOGIN\r\n", 334)
            sendAndCheckResponse("${toBase64(username)}\r\n", 334)
            sendAndCheckResponse("${toBase64(password)}\r\n", 235)

            // Отправляем команду  MAIL FROM.
            sendAndCheckResponse("MAIL FROM: <$username>\r\n", 250)

            // Отправляем команду RCPT TO.
            sendAndCheckResponse("RCPT TO: <pavlovaalexandra29@gmail.com>\r\n", 250)

            // Отправляем команду DATA.
            sendAndCheckResponse("DATA\r\n", 354)

            // Отправляем данные сообщения.
            val message = StringBuilder()
            message
                .append("From: <LOL>\r\n")
                .append("To: <st076358@student.spbu.ru>\r\n")
                .append("Subject: Test message with Hello\r\n")
                .append("Content-Type: text/plain\r\n")
                .append("Hi Dear,\r\n")
                .append("This lab is too hard.\r\n")
            //sendAndCheckResponse(message.toString(), -1, true)
            val picture = File("/home/alex/Proga/comp_networks/lab5/src/main/kotlin/pavlova/alexandra/sad_cat.jpg")
            sendImageAndCheckResponse(picture, -1, true)

            // Завершаем строкой с одной точкой.
            sendAndCheckResponse("\r\n.\r\n", 250)

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
        var response: String? = null
        try {
            response = reader.nextLine()
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
        println("SENDING: $message")

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

        println("SENDING: ${path.name}")

        val message = StringBuilder()
            .append("Content-Type: image/jpeg; name=${path.name}\r\n")
            .append("Content-Transfer-Encoding: base64\r\n")
        sendAndCheckResponse(message.toString(), -1, true)

        val byteArrayWriter = ByteArrayOutputStream()
        val img = ImageIO.read(path)
        val bufferedOutputStream = BufferedOutputStream(writer)
        ImageIO.write(img, path.extension, byteArrayWriter)
        bufferedOutputStream.write(toBase64(byteArrayWriter.toByteArray()))
        bufferedOutputStream.flush()
        sendAndCheckResponse("\r\n", -1, true)

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

private fun toBase64(request: ByteArray): ByteArray {
    val enc = Base64.getMimeEncoder() ?: throw Exception()
    return enc.encode(request)
}

private fun fromBase64(request: String): String {
    return String(Base64.getDecoder().decode(request))
}