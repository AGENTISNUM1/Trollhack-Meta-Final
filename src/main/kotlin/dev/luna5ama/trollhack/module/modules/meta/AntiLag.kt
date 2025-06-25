
import com.mojang.realmsclient.gui.ChatFormatting
import dev.fastmc.common.TickTimer
import dev.fastmc.common.TimeUnit
import dev.luna5ama.trollhack.event.events.PacketEvent
import dev.luna5ama.trollhack.event.safeListener
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import net.minecraft.network.play.server.SPacketChat
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

internal object AntiLag : Module(
    name = "AntiUnicode",
    description = "stops lag msg",
    category = Category.META,
    modulePriority = 200
) {
    private var maxSymbolCount by setting("Max Symbol Count", 100, 10..250, 10)
    private var notify by setting("Notify", true)
    private val delay: TickTimer = TickTimer(TimeUnit.TICKS)

    init{
        safeListener<PacketEvent.Receive> {
            if (it.packet is SPacketChat) {
                val text = (it.packet as SPacketChat).chatComponent.unformattedText
                var symbolCount = 0
                for (element in text) {
                    val c = element
                    if (isSymbol(c)) {
                        ++symbolCount
                    }
                    if (symbolCount <= maxSymbolCount) continue
                    if (notify && delay.tickAndReset(10)) {
                        NoSpamMessage.sendMessage("$chatName Lag message blocked!")
                        delay.reset()
                    }
                    it.cancel()
                    break
                }
            }
        }
    }

    private fun isSymbol(charIn: Char): Boolean {
        return !(charIn in 'A'..'Z' || charIn in 'a'..'z' || charIn in '0'..'9')
    }
}