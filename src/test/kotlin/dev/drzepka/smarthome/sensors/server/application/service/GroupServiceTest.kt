package dev.drzepka.smarthome.sensors.server.application.service

import dev.drzepka.smarthome.sensors.server.application.dto.group.CreateGroupRequest
import dev.drzepka.smarthome.sensors.server.domain.entity.Group
import dev.drzepka.smarthome.sensors.server.domain.repository.DeviceRepository
import dev.drzepka.smarthome.sensors.server.domain.repository.GroupRepository
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@Suppress("RedundantUnitExpression")
@ExtendWith(MockitoExtension::class)
internal class GroupServiceTest {

    private val groupRepository = mock<GroupRepository>()
    private val deviceRepository = mock<DeviceRepository>()

    @Test
    fun `should create group`() {
        val request = CreateGroupRequest().apply {
            name = "name"
            description = "description"
        }

        whenever(groupRepository.save(any())).thenAnswer {
            it.getArgument<Group>(0).id = 1
            Unit
        }

        val resource = getService().createGroup(request)

        then(resource.name).isEqualTo(request.name)
        then(resource.description).isEqualTo(request.description)

        val captor = argumentCaptor<Group>()
        verify(groupRepository).save(captor.capture())

        val entity = captor.firstValue
        then(entity.name).isEqualTo(request.name)
        then(entity.description).isEqualTo(request.description)
        then(entity.createdAt).isNotNull
    }

    private fun getService(): GroupService = GroupService(groupRepository, deviceRepository)
}