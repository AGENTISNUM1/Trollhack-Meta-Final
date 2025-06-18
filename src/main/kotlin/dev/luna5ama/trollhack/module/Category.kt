package dev.luna5ama.trollhack.module

import dev.luna5ama.trollhack.translation.TranslateType
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum

enum class Category(override val displayName: CharSequence) : DisplayEnum {
    COMBAT(TranslateType.COMMON commonKey "Combat"),
    MISC(TranslateType.COMMON commonKey "Misc"),
    EXPLOIT(TranslateType.COMMON commonKey "Exploit"),
    MOVEMENT(TranslateType.COMMON commonKey "Movement"),
    PLAYER(TranslateType.COMMON commonKey "Player"),
    RENDER(TranslateType.COMMON commonKey "Render"),
    META(TranslateType.COMMON commonKey "Meta"),
    CLIENT(TranslateType.COMMON commonKey "Client");

    override fun toString() = displayString
}