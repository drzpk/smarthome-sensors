package dev.drzepka.smarthome.sensors.server.presentation

import dev.drzepka.smarthome.sensors.server.application.dto.group.CreateGroupRequest
import dev.drzepka.smarthome.sensors.server.application.dto.group.UpdateGroupRequest
import dev.drzepka.smarthome.sensors.server.application.service.GroupService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.ktor.ext.get

fun Route.groupController() {

    val groupService = get<GroupService>()

    route("/groups") {
        post("") {
            val request = call.receive<CreateGroupRequest>()
            val resource = transaction {
                groupService.createGroup(request)
            }

            call.respond(resource)
        }

        get("") {
            val list = transaction {
                groupService.getGroups()
            }

            call.respond(list)
        }

        get("/{id}") {
            val groupId = call.parameters["id"]!!.toInt()
            val resource = transaction {
                groupService.getGroup(groupId)
            }

            call.respond(resource)
        }

        patch("/{id}") {
            val groupId = call.parameters["id"]!!.toInt()
            val request = call.receive<UpdateGroupRequest>()
            request.id = groupId

            val resource = transaction {
                groupService.updateGroup(request)
            }

            call.respond(resource)
        }

        delete("/{id}") {
            val groupId = call.parameters["id"]!!.toInt()
            transaction {
                groupService.deleteGroup(groupId)
            }

            call.respond(HttpStatusCode.NoContent)
        }
    }
}