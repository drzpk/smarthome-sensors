package dev.drzepka.smarthome.sensors.server.application.service

import dev.drzepka.smarthome.sensors.server.application.ValidationErrors
import dev.drzepka.smarthome.sensors.server.application.dto.group.UpdateGroupRequest
import dev.drzepka.smarthome.sensors.server.application.dto.group.GroupResource
import dev.drzepka.smarthome.sensors.server.application.dto.group.CreateGroupRequest
import dev.drzepka.smarthome.sensors.server.domain.entity.Group
import dev.drzepka.smarthome.sensors.server.domain.exception.NotFoundException
import dev.drzepka.smarthome.sensors.server.domain.repository.DeviceRepository
import dev.drzepka.smarthome.sensors.server.domain.repository.GroupRepository
import dev.drzepka.smarthome.sensors.server.domain.util.Logger
import kotlin.IllegalArgumentException

class GroupService(private val groupRepository: GroupRepository, private val deviceRepository: DeviceRepository) {

    private val log by Logger()

    fun getGroups(): Collection<GroupResource> {
        return groupRepository.findAll()
            .map { GroupResource.fromEntity(it) }
    }

    fun createGroup(request: CreateGroupRequest): GroupResource {
        validateCreateUpdateGroup(request, true)
        log.info("Creating new group with name '{}' and description '{}'", request.name, request.description)

        val group = Group().apply {
            name = request.name!!
            description = request.description!!
        }

        groupRepository.save(group)

        log.info("Created new group with id {}", group.id)
        return GroupResource.fromEntity(group)
    }

    fun updateGroup(request: UpdateGroupRequest): GroupResource {
        validateCreateUpdateGroup(request, false)
        log.info(
            "Updating device {} with name '{}' and description '{}'",
            request.id, request.name, request.description
        )

        val group = groupRepository.findById(request.id) ?: throw NotFoundException("Group ${request.id} wasn't found")
        request.name?.let { group.name = it }
        request.description?.let { group.description = it }

        groupRepository.save(group)
        log.info("Updated group {}", group.id)
        return GroupResource.fromEntity(group)
    }

    fun getGroup(groupId: Int): GroupResource {
        return groupRepository.findById(groupId)?.let { GroupResource.fromEntity(it) }
            ?: throw NotFoundException("Group $groupId wasn't found")
    }

    fun deleteGroup(groupId: Int) {
        val count = deviceRepository.countByGroupId(groupId)
        if (count > 0)
            throw IllegalArgumentException("Cannot delete group $groupId, because there are $count devices assigned to it")

        groupRepository.delete(groupId)
    }

    private fun validateCreateUpdateGroup(request: CreateGroupRequest, required: Boolean) {
        val validation = ValidationErrors()
        validateName(request.name, required, validation)
        validateDescription(request.description, required, validation)
        validation.verify()
    }

    private fun validateName(name: String?, required: Boolean, validation: ValidationErrors) {
        val missing = required && (name == null || name.isBlank())
        if (missing || (name != null && name.length > 64))
            validation.addFieldError("name", "Name must have length between 1 and 64 characters")

        if (name != null && groupWithNameExists(name))
            validation.addObjectError("Group with name \"${name}\" already exists")
    }

    private fun validateDescription(description: String?, required: Boolean, validation: ValidationErrors) {
        val missing = required && (description == null || description.isBlank())
        if (missing || (description != null && description.length > 64))
            validation.addFieldError("description", "Description must have length between 1 and 256 characters")
    }

    private fun groupWithNameExists(name: String): Boolean {
        return groupRepository.findByName(name) != null
    }
}