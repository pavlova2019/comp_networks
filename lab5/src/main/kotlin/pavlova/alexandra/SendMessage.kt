package pavlova.alexandra

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.nio.charset.Charset
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

fun main(args: Array<String>) {

    val parser = ArgParser("client")
    val host by parser.option(ArgType.String, description = "Host").default("smtp.gmail.com")
    val port by parser.option(ArgType.Int, description = "Port").default(465)
    val userName by parser.option(ArgType.String, description = "Username").required()
    val password by parser.option(ArgType.String, description = "Password").required()

    parser.parse(args)
    val props = Properties()

    props["mail.smtp.host"] = host
    props["mail.smtp.port"] = port
    props["mail.smtp.auth"] = "true"
    props["mail.smtp.ssl.enable"] = "true"

    val session = Session.getDefaultInstance(props, object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(userName, password)
        }
    })

    session.debug = true

    println("Email to:")
    val emailTo = readLine() ?: throw Exception("That's not an email")
    println("Insert subject:")
    val subject = readLine() ?: throw Exception()
    println("Insert text:")
    val text = readLine()?: throw Exception()
    println("Insert text type:")
    val type = readLine() ?: throw Exception()
    if (type != "plain" && type != "html") {
        println("Only plain/html text available")
    }

    sendMessage(session, InternetAddress(userName), emailTo, subject, text, type)

}

fun sendMessage(session: Session, from: InternetAddress, emailTo: String, subject: String, text: String, type: String) {
    try {
        val mimeMessage = MimeMessage(session)
        mimeMessage.setFrom(from)
        mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo, false))
        mimeMessage.setText(text, Charset.defaultCharset().toString(), type)
        mimeMessage.subject = subject
        mimeMessage.sentDate = Date()

        Transport.send(mimeMessage)
    } catch (messagingException: MessagingException) {
        messagingException.printStackTrace()
    }
}