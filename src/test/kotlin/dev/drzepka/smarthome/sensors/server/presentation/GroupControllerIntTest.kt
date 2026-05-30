package dev.drzepka.smarthome.sensors.server.presentation

import dev.drzepka.smarthome.sensors.server.BaseIntegrationTest
import dev.drzepka.smarthome.sensors.server.application.dto.group.GroupResource
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test

class GroupControllerIntTest : BaseIntegrationTest() {

    @Test
    fun `should create group`() = testApp { client ->
        val response = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "name": "Living Room",
                    "description": "Sensors in the living room"
                }
            """.trimIndent())
        }

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<GroupResource>()
        then(body.id).isGreaterThan(0)
        then(body.name).isEqualTo("Living Room")
        then(body.description).isEqualTo("Sensors in the living room")
    }

    @Test
    fun `should return 422 when group name is missing`() = testApp { client ->
        val response = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "description": "desc"
                }
            """.trimIndent())
        }

        then(response.status).isEqualTo(HttpStatusCode.UnprocessableEntity)
    }

    @Test
    fun `should list all groups`() = testApp { client ->
        client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "name": "Group A",
                    "description": "desc"
                }
            """.trimIndent())
        }
        client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "name": "Group B",
                    "description": "desc"
                }
            """.trimIndent())
        }

        val response = client.get("/api/groups")

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<List<GroupResource>>()
        then(body).hasSize(2)
        then(body.map { it.name }).containsExactlyInAnyOrder("Group A", "Group B")
    }

    @Test
    fun `should get group by id`() = testApp { client ->
        val created = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "name": "Bedroom",
                    "description": "desc"
                }
            """.trimIndent())
        }.body<GroupResource>()

        val response = client.get("/api/groups/${created.id}")

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<GroupResource>()
        then(body.id).isEqualTo(created.id)
        then(body.name).isEqualTo("Bedroom")
    }

    @Test
    fun `should return 404 for unknown group`() = testApp { client ->
        val response = client.get("/api/groups/9999")
        then(response.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `should update group`() = testApp { client ->
        val created = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "name": "Old Name",
                    "description": "old"
                }
            """.trimIndent())
        }.body<GroupResource>()

        val response = client.patch("/api/groups/${created.id}") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "name": "New Name"
                }
            """.trimIndent())
        }

        then(response.status).isEqualTo(HttpStatusCode.OK)
        val body = response.body<GroupResource>()
        then(body.name).isEqualTo("New Name")
    }

    @Test
    fun `should return 404 when updating unknown group`() = testApp { client ->
        val response = client.patch("/api/groups/9999") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "name": "New Name"
                }
            """.trimIndent())
        }

        then(response.status).isEqualTo(HttpStatusCode.NotFound)
    }

    @Test
    fun `should delete group`() = testApp { client ->
        val created = client.post("/api/groups") {
            contentType(ContentType.Application.Json)
            setBody("""
                {
                    "name": "ToDelete",
                    "description": "desc"
                }
            """.trimIndent())
        }.body<GroupResource>()

        val deleteResponse = client.delete("/api/groups/${created.id}")
        then(deleteResponse.status).isEqualTo(HttpStatusCode.NoContent)

        val getResponse = client.get("/api/groups/${created.id}")
        then(getResponse.status).isEqualTo(HttpStatusCode.NotFound)
    }
}
