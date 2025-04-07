package dev.luna5ama.trollhack.setting

import dev.luna5ama.trollhack.TrollHackMod
import dev.luna5ama.trollhack.gui.rgui.Component
import dev.luna5ama.trollhack.module.modules.client.ClientSettings
import dev.luna5ama.trollhack.setting.configs.AbstractConfig
import dev.luna5ama.trollhack.setting.settings.AbstractSetting
import java.io.File

internal object GuiConfig : AbstractConfig<Component>(
    "gui",
    "${TrollHackMod.DIRECTORY}/config/gui"
) {
    override val file get() = File("$filePath/${ClientSettings.guiPreset}.json")
    override val backup get() = File("$filePath/${ClientSettings.guiPreset}.bak")

    override fun addSettingToConfig(owner: Component, setting: AbstractSetting<*>) {
        val groupName = owner.uiSettingGroup.groupName
        if (groupName.isNotEmpty()) {
            getGroupOrPut(groupName).getGroupOrPut(owner.internalName).addSetting(setting)
        }
    }
}