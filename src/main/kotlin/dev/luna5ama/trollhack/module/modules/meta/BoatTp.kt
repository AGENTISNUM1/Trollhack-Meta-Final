package dev.luna5ama.trollhack.module.modules.meta

import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityBoat
import net.minecraft.network.Packet
import net.minecraft.network.play.client.CPacketVehicleMove

internal object BoatTp : Module(
    name = "BoatTp",
    category = Category.META,
    description = "tp wit boat",
    modulePriority = 100
) {

    private val Loop = 4
    private val Y by setting("Desync Ammount", 0.5f, 0.1f..0.9f, 0.1f)
    private val Y2 by setting("Tp ammount", 120f, 10f..200f, 10f)
    init {
        onEnable{
            val loopCountMax: Int = Loop
            for (loopCount in 0..<loopCountMax) {
                if (mc.player.ridingEntity !is EntityBoat) continue
                val boat = mc.player.ridingEntity as EntityBoat
                val originalPos = boat.positionVector
                boat.setPosition(boat.posX, boat.posY + Y.toDouble(), boat.posZ)
                val groundPacket = CPacketVehicleMove(boat as Entity)
                boat.setPosition(boat.posX, boat.posY + Y2.toDouble(), boat.posZ)
                val skyPacket = CPacketVehicleMove(boat as Entity)
                boat.setPosition(originalPos.x, originalPos.y, originalPos.z)
                for (i in 0..99) {
                    mc.player.connection.sendPacket(skyPacket as Packet<*>)
                    mc.player.connection.sendPacket(groundPacket as Packet<*>)
                }
                mc.player.connection.sendPacket(CPacketVehicleMove(boat as Entity) as Packet<*>)
            }
            this.disable()
        }
    }
}