import com.jetbrains.handson.httpapi.module
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.Test
import kotlin.test.assertEquals

class ItemTests {
    @Test
    fun testGetItem() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/v1/items/100101").apply {
                assertEquals(
                    """{"100101":{"name":"lol","description":"something"}}""",
                    response.content
                )
                assertEquals(HttpStatusCode.OK, response.status())
            }
        }
    }
}