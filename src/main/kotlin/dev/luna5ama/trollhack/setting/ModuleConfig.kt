package dev.luna5ama.trollhack.setting

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.module.AbstractModule
import dev.luna5ama.trollhack.module.modules.client.ClientSettings
import dev.luna5ama.trollhack.setting.configs.NameableConfig
import java.io.File

internal object ModuleConfig : NameableConfig<AbstractModule>(
    "modules",
    "${TrollHackMod.DIRECTORY}/config/modules",
) {
    override val file: File get() = File("$filePath/${ClientSettings.modulePreset}.json")
    override val backup get() = File("$filePath/${ClientSettings.modulePreset}.bak")
}