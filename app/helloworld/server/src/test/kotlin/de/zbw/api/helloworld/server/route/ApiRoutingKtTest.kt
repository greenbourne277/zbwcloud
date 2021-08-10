package de.zbw.api.helloworld.server.route

import de.zbw.api.helloworld.server.ServicePoolWithProbes
import de.zbw.business.helloworld.server.ApiBackend
import de.zbw.business.helloworld.server.UserInformation
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.testng.annotations.Test

/**
 * Testing [ApiRoutingKtTest].
 *
 * Created on 06-05-2021.
 * @author Christian Bay (c.bay@zbw.eu)
 */
class ApiRoutingKtTest() {

    @Test
    fun testHelloWorldApi() {
        val servicePool = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                }
            ),
            port = 8082,
        )

        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/api/v1/userinfo/users/2")) {
                val rec: String = response.content!!
                val obj = Json.decodeFromString<UserInformation>(rec)
                assertThat(UserInformation(ApiBackend.EXAMPLE_USER, 2L), `is`(obj))
            }
        }
    }

    @Test
    fun testHelloWorldApiFailing() {
        val servicePool = ServicePoolWithProbes(
            services = listOf(
                mockk {
                    every { isReady() } returns true
                    every { isHealthy() } returns true
                }
            ),
            port = 8082,
        )

        withTestApplication(servicePool.application()) {
            with(handleRequest(HttpMethod.Get, "/api/v1/userinfo/users/Foobar")) {
                assertThat(response.status(), `is`(HttpStatusCode.NotFound))
            }
        }
    }
}