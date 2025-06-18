package dev.luna5ama.trollhack.gui.rgui.windows

import dev.luna5ama.trollhack.graphics.RenderUtils2D
import dev.luna5ama.trollhack.graphics.shaders.WindowBlurShader
import dev.luna5ama.trollhack.gui.IGuiScreen
import dev.luna5ama.trollhack.module.modules.client.ClickGUI
import dev.luna5ama.trollhack.setting.GuiConfig
import dev.luna5ama.trollhack.setting.configs.AbstractConfig
import dev.luna5ama.trollhack.util.interfaces.Nameable
import dev.luna5ama.trollhack.util.math.vector.Vec2f

open class BasicWindow(
    screen: IGuiScreen,
    name: CharSequence,
    uiSettingGroup: UiSettingGroup,
    config: AbstractConfig<out Nameable> = GuiConfig
) : CleanWindow(name, screen, uiSettingGroup, config) {
    override fun onRender(absolutePos: Vec2f) {
        super.onRender(absolutePos)
        WindowBlurShader.render(renderWidth, renderHeight)
        if (ClickGUI.titleBar) {
            RenderUtils2D.drawRoundedRectFilled(0.0f, draggableHeight, renderWidth, renderHeight, ClickGUI.radius, ClickGUI.backGround)
        } else {
            RenderUtils2D.drawTopRoundedRectFilled(0.0f, 0.0f, renderWidth, renderHeight, ClickGUI.radius, ClickGUI.backGround)
        }
        if (ClickGUI.windowOutline) {
            RenderUtils2D.drawRoundedRectOutline(0.0f, 0.0f, renderWidth, renderHeight, ClickGUI.radius, 1.0f, ClickGUI.primary.alpha(255))
        }
        if (ClickGUI.titleBar) {
            RenderUtils2D.drawTopRoundedRectFilled(0.0f, 0.0f, renderWidth, draggableHeight, ClickGUI.radius, ClickGUI.primary)
        }
    }
}