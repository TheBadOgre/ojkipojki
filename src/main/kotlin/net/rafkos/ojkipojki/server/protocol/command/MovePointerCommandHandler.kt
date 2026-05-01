package net.rafkos.ojkipojki.server.protocol.command

import net.rafkos.ojkipojki.server.ServerContext
import net.rafkos.ojkipojki.server.application.CommandContext
import net.rafkos.ojkipojki.shared.domain.Pointer
import net.rafkos.ojkipojki.shared.protocol.Handler
import net.rafkos.ojkipojki.shared.protocol.command.MovePointerCommand
import net.rafkos.ojkipojki.shared.protocol.event.PointersUpdatedEvent

class MovePointerCommandHandler : Handler<MovePointerCommand> {
    override fun handle(action: MovePointerCommand) {
        val clientId = CommandContext.clientId ?: return
        val color = ServerContext.clientColorRegistry.getColor(clientId) ?: return
        val pointer = Pointer(action.x, action.y, color.first, color.second, color.third)
        ServerContext.pointerRepository.save(clientId, pointer)

        ServerContext.eventBroadcastService.broadcastCustom(excludeClientId = clientId) { recipientId ->
            PointersUpdatedEvent(ServerContext.pointerRepository.findAllExcept(recipientId))
        }
    }
}
