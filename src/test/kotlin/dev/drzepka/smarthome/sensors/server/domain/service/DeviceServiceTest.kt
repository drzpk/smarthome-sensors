package dev.drzepka.smarthome.sensors.server.domain.service

import dev.drzepka.smarthome.sensors.server.application.FieldError
import dev.drzepka.smarthome.sensors.server.application.dto.device.CreateDeviceRequest
import dev.drzepka.smarthome.sensors.server.application.service.DeviceService
import dev.drzepka.smarthome.sensors.server.domain.entity.Device
import dev.drzepka.smarthome.sensors.server.domain.entity.Group
import dev.drzepka.smarthome.sensors.server.domain.exception.NotFoundException
import dev.drzepka.smarthome.sensors.server.domain.exception.ValidationException
import dev.drzepka.smarthome.sensors.server.domain.repository.DeviceRepository
import dev.drzepka.smarthome.sensors.server.domain.repository.GroupRepository
import org.assertj.core.api.Assertions
import org.assertj.core.api.BDDAssertions.assertThatCode
import org.assertj.core.api.BDDAssertions.catchThrowable
import org.assertj.core.api.BDDAssertions.then
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

@ExtendWith(MockitoExtension::class)
internal class DeviceServiceTest {

    private val deviceRepository = mock<DeviceRepository>()
    private val groupRepository = mock<GroupRepository>()

    @Test
    fun `should create device`() {
        val request = CreateDeviceRequest().apply {
            name = "name"
            description = "description"
            mac = "mac"
            groupId = 2
        }

        whenever(groupRepository.findById(eq(2))).thenReturn(Group().apply { id = 2 })

        whenever(deviceRepository.save(any())).thenAnswer { invocation ->
            (invocation.getArgument(0) as Device).apply { id = 1 }
        }

        val resource = getService().createDevice(request)

        then(resource.name).isEqualTo(request.name)
        then(resource.description).isEqualTo(request.description)

        val captor = argumentCaptor<Device>()
        verify(deviceRepository, times(1)).save(captor.capture())

        val entity = captor.firstValue
        then(entity.name).isEqualTo(request.name)
        then(entity.description).isEqualTo(request.description)
        then(entity.active).isTrue
    }

    @Test
    fun `should validate request when creating device`() {
        val emptyRequest = CreateDeviceRequest()

        val caught = catchThrowable { getService().createDevice(emptyRequest) }
        then(caught).isInstanceOf(ValidationException::class.java)

        val validationException = caught as ValidationException
        then(validationException.validationErrors.errors).hasSize(4)

        val validationErrors = validationException.validationErrors.errors
        then(validationErrors[0]).isInstanceOf(FieldError::class.java)
        then((validationErrors[0] as FieldError).field).isEqualTo("name")
        then(validationErrors[1]).isInstanceOf(FieldError::class.java)
        then((validationErrors[1] as FieldError).field).isEqualTo("description")
        then(validationErrors[2]).isInstanceOf(FieldError::class.java)
        then((validationErrors[2] as FieldError).field).isEqualTo("mac")
        then(validationErrors[3]).isInstanceOf(FieldError::class.java)
        then((validationErrors[3] as FieldError).field).isEqualTo("groupId")
    }

    @Test
    fun `should get device`() {
        val activeDevice = Device(Group()).apply { id = 1; active = true }
        whenever(deviceRepository.findById(1)).thenReturn(activeDevice)

        val inactiveDevice = Device(Group()).apply { id = 2;active = false }
        whenever(deviceRepository.findById(2)).thenReturn(inactiveDevice)

        val service = getService()

        assertThatCode {
            service.getDevice(1)
        }.doesNotThrowAnyException()

        Assertions.assertThatExceptionOfType(NotFoundException::class.java).isThrownBy {
            service.getDevice(2)
        }
    }

    @Test
    fun `should delete device`() {
        val device = Device(Group()).apply { active = true }
        whenever(deviceRepository.findById(12)).thenReturn(device)

        getService().deleteDevice(12)

        then(device.active).isFalse
        then(device.group).isNull()
        verify(deviceRepository, times(1)).save(same(device))
    }

    private fun getService(): DeviceService = DeviceService(deviceRepository, groupRepository)
}