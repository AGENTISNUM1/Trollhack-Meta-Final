package dev.luna5ama.trollhack.module.modules.client

import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.gui.hudgui.elements.client.ActiveModules
import dev.luna5ama.trollhack.gui.hudgui.elements.client.GradientWatermark
import dev.luna5ama.trollhack.gui.hudgui.elements.world.TextRadar
import dev.luna5ama.trollhack.manager.managers.HotbarSwitchManager
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.module.modules.combat.*
import dev.luna5ama.trollhack.module.modules.combat.AutoRegear
import dev.luna5ama.trollhack.module.modules.combat.BedAura
import dev.luna5ama.trollhack.module.modules.combat.CombatSetting
import dev.luna5ama.trollhack.module.modules.combat.HoleESP
import dev.luna5ama.trollhack.module.modules.exploit.Bypass
import dev.luna5ama.trollhack.module.modules.movement.ElytraFlightNew
import dev.luna5ama.trollhack.module.modules.movement.Step
import dev.luna5ama.trollhack.module.modules.movement.Strafe
import dev.luna5ama.trollhack.module.modules.player.FastUse
import dev.luna5ama.trollhack.module.modules.player.InventorySync
import dev.luna5ama.trollhack.module.modules.player.PacketMine
import dev.luna5ama.trollhack.module.modules.render.Nametags
import dev.luna5ama.trollhack.module.modules.wizard.BetterPot
import dev.luna5ama.trollhack.util.atValue

internal object AutoConfig : Module(
    name = "Auto Config",
    description = "Automaticly config the client",
    category = Category.CLIENT,
    visible = false
) {
    private val page = setting("Page", Page.COMBAT)

    // Combat Page
    private val rotations by setting("Use Rotations", true, page.atValue(Page.COMBAT))
    private val configpot by setting("Configure AutoPot", true, page.atValue(Page.COMBAT))
    private val potmode by setting("AutoPot Delay", PotMode.LOW_PING, { configpot && page.value == Page.COMBAT })
    private val configba by setting("Configure BedAura", true, page.atValue(Page.COMBAT))
    private val bamode by setting("BedAura Config Mode", BaMode.AUTO, { configba && page.value == Page.COMBAT})
    private val configregear by setting("Configure AutoRegear", true, page.atValue(Page.COMBAT))
    private val configpmine by setting("Configure PacketMine", true, page.atValue(Page.COMBAT))
    private val combatconfig by setting("Configure Combat Settings", true, page.atValue(Page.COMBAT))
    private val configessentials by setting("Configure Essentials", true, page.atValue(Page.COMBAT), description = "Configures several other modules needed for combat")
    private val enableneeded by setting("Enable essential modules", false, page.atValue(Page.COMBAT))

    // Movement Page
    private val configstep by setting("Configure Step", true, page.atValue(Page.MOVEMENT))
    private val configstrafe by setting("Configure Strafe", true, page.atValue(Page.MOVEMENT))
    private val configelyfly by setting("Configure ElytraFlight", true, page.atValue(Page.MOVEMENT))

    // Render page
    private val configrender = setting("Configure Renders", false, page.atValue(Page.RENDER))
    private val theme = setting("Theme", Themes.ORIGINAL, {page.value == Page.RENDER && configrender.value})
    private val customthemecolor = setting("Custom Color", ColorRGB(100, 100, 100), false, {page.value == Page.RENDER && configrender.value && theme.value == Themes.CUSTOM})
    private val packetminetheme by setting("Change Packet Mine", false, {page.value == Page.RENDER && configrender.value})
    private val holeesptheme by setting("Change HoleESP", true, {page.value == Page.RENDER && configrender.value})
    private val friendcolor by setting("Change Friend Color", false, {page.value == Page.RENDER && configrender.value})
    private val selectioncolor by setting("Change Selection Color", false, {page.value == Page.RENDER && configrender.value})
    private val chamscolor by setting("Change chams colors", false, {page.value == Page.RENDER && configrender.value})
    private val bacolor by setting("Change BedAura color", false, {page.value == Page.RENDER && configrender.value})


    private val bgcolor = ColorRGB(3, 3, 3, 160)
    private var themecolor = ColorRGB(100, 100, 100)
    private val unsafeholecolor = ColorRGB(240, 240, 240)
    private enum class Page {
        COMBAT, MOVEMENT, RENDER
    }
    private enum class PotMode {
        HIGH_PING, LOW_PING
    }
    private enum class BaMode {
        SWITCHSLOWER, SWITCHFAST, INSTANT, AUTO
    }
    private enum class Themes {
        PURPLE, MINT, THUNDER, ICE, RED, PINK, LIGHTGREEN, SKYBLUE, ORIGINAL, CUSTOM
    }

    init {
        onEnable {
            if (rotations) configrots()
            if (configpot) configbpot()
            if (configba) configbedaura()
            if (configregear) configaregear()
            if (configpmine) configpacketmine()
            if (combatconfig) configcombat()
            if (configessentials) essentialsconfig()
            if (configstep) stepconfig()
            if (configstrafe) strafeconfig()
            if (configelyfly) elytraflyconfig()
            if (configrender.value) themeconfig()
            disable()
        }
    }

    private fun configrots() {
        Bypass.blockPlaceRotation = false
        Bypass.crystalRotation = false
    }

    private fun configbedaura() {
        BedAura.enablerotationPitch = false
        BedAura.ghostSwitchBypass = HotbarSwitchManager.Override.NONE
        BedAura.smartDamage = false
        BedAura.noSuicide = 0.0f
        BedAura.minDamage = 2.3f
        BedAura.maxSelfDamage = 20.0f
        BedAura.damageBalance = -3.0f
        BedAura.range = 5.6f
        BedAura.updateDelay = 5
        when (bamode) {
            BaMode.AUTO -> {
                BedAura.autoSetSpeed = true
                BedAura.placeDelay = 31
                BedAura.breakDelay = 35

            }
            BaMode.SWITCHSLOWER -> {
                BedAura.timingMode = BedAura.TimingMode.SWITCH
                BedAura.breakDelay = 41
                BedAura.placeDelay = 41
            }
            BaMode.SWITCHFAST -> {
                BedAura.timingMode = BedAura.TimingMode.SWITCH
                BedAura.placeDelay = 31
                BedAura.breakDelay = 31
            }
            BaMode.INSTANT -> {
                BedAura.timingMode = BedAura.TimingMode.INSTANT
            }
        }
        BedAura.delay = 68
    }

    private fun configbpot() {
        BetterPot.keepHealInHotbar = true
        BetterPot.ghostSwitchBypass = HotbarSwitchManager.Override.NONE
        BetterPot.healHealth = 19.5f
        when (potmode) {
            PotMode.LOW_PING -> {
                BetterPot.healDelay = 91
            }
            PotMode.HIGH_PING -> {
                BetterPot.healDelay = 68
            }
        }

    }

    private fun configaregear() {
        AutoRegear.placeRange = 4f
        AutoRegear.clickDelayMs = 0
        AutoRegear.postDelayMs = 0
        AutoRegear.moveTimeoutMs = 0
    }

    private fun configpacketmine() {
        PacketMine.miningMode = PacketMine.MiningMode.CONTINUOUS_INSTANT
    }

    private fun configcombat() {
        CombatSetting.chams = false
        CombatSetting.healthPriority = 0.8f
        CombatSetting.crosshairPriority = 0.7f
    }

    private fun essentialsconfig() {
        FastUse.enable()
        InventorySync.enable()
        FastUse.multiUse = 2
        FastUse.delay = 0
        FastUse.allItems = false
        AutoHoleFill.hRange = 2.5f
        AutoHoleFill.fourBlocksHole = false
    }

    private fun stepconfig() {
        Step.mode = Step.Mode.PACKET
        Step.useTimer = false
        Step.minHeight = 0.6f
        Step.maxHeight = 2.1f
        Step.postTimer = 1.0f
    }

    private fun strafeconfig() {
        Strafe.enable()
        Strafe.timerBoost = 1.10f
    }

    private fun elytraflyconfig() {
        ElytraFlightNew.speed = 2.95f
    }

    private fun themeconfig() {
        if (theme.value == Themes.ORIGINAL) {
            HoleESP.bedrockColor = ColorRGB(31, 255, 31)
            HoleESP.obbyColor = ColorRGB(255, 255, 31)
            HoleESP.trappedColor = ColorRGB(255, 31, 31)
            ClickGUI.primarySetting = ColorRGB(255, 140, 180, 220)
            ClickGUI.backgroundSetting = ColorRGB(40, 32, 36, 160)
            GradientWatermark.color1 = ColorRGB(255, 140, 180)
            ActiveModules.color1 = ColorRGB(255, 140, 180)
        }
        if (theme.value == Themes.CUSTOM) {
            if (holeesptheme) {
                HoleESP.bedrockColor = customthemecolor.value
                HoleESP.obbyColor = unsafeholecolor
                HoleESP.trappedColor = unsafeholecolor
            }
            ClickGUI.primarySetting = customthemecolor.value
            ClickGUI.backgroundSetting = bgcolor
            ActiveModules.color1 = customthemecolor.value
            GradientWatermark.color1 = customthemecolor.value
            if (friendcolor) {
                TextRadar.friendcolor = customthemecolor.value
                Nametags.friendcolor = customthemecolor.value
            }
            if (packetminetheme) {
                PacketMine.useCustomColor = true
                PacketMine.customColor = customthemecolor.value
            }
        }
        if (theme.value == Themes.PURPLE) {
            themecolor = ColorRGB(146, 148, 255)
            if (holeesptheme) {
                HoleESP.bedrockColor = themecolor
                HoleESP.obbyColor = unsafeholecolor
                HoleESP.trappedColor = unsafeholecolor
            }
            ClickGUI.primarySetting = themecolor
            ActiveModules.color1 = themecolor
            GradientWatermark.color1 = themecolor
            if (friendcolor) {
                TextRadar.friendcolor = themecolor
                Nametags.friendcolor = themecolor
            }
            if (packetminetheme) {
                PacketMine.useCustomColor = true
                PacketMine.customColor = themecolor
            }
            ClickGUI.backgroundSetting = bgcolor

        }
        if (theme.value == Themes.MINT) {
            themecolor = ColorRGB(170, 240, 221)
            if (holeesptheme) {
                HoleESP.bedrockColor = themecolor
                HoleESP.obbyColor = unsafeholecolor
                HoleESP.trappedColor = unsafeholecolor
            }
            ClickGUI.primarySetting = themecolor
            ActiveModules.color1 = themecolor
            GradientWatermark.color1 = themecolor
            if (friendcolor) {
                TextRadar.friendcolor = themecolor
                Nametags.friendcolor = themecolor
            }
            if (packetminetheme) {
                PacketMine.useCustomColor = true
                PacketMine.customColor = themecolor
            }
            ClickGUI.backgroundSetting = bgcolor

        }
        if (theme.value == Themes.ICE) {
            themecolor = ColorRGB(185,232,234)
            if (holeesptheme) {
                HoleESP.bedrockColor = themecolor
                HoleESP.obbyColor = unsafeholecolor
                HoleESP.trappedColor = unsafeholecolor
            }
            ClickGUI.primarySetting = themecolor
            ActiveModules.color1 = themecolor
            GradientWatermark.color1 = themecolor
            if (friendcolor) {
                TextRadar.friendcolor = themecolor
                Nametags.friendcolor = themecolor
            }
            if (packetminetheme) {
                PacketMine.useCustomColor = true
                PacketMine.customColor = themecolor
            }
            ClickGUI.backgroundSetting = bgcolor

        }
        if (theme.value == Themes.THUNDER) {
            themecolor = ColorRGB(0, 77, 127)
            if (holeesptheme) {
                HoleESP.bedrockColor = themecolor
                HoleESP.obbyColor = unsafeholecolor
                HoleESP.trappedColor = unsafeholecolor
            }
            ClickGUI.primarySetting = themecolor
            ActiveModules.color1 = themecolor
            GradientWatermark.color1 = themecolor
            if (friendcolor) {
                TextRadar.friendcolor = themecolor
                Nametags.friendcolor = themecolor
            }
            if (packetminetheme) {
                PacketMine.useCustomColor = true
                PacketMine.customColor = themecolor
            }
            ClickGUI.backgroundSetting = bgcolor

        }
        if (theme.value == Themes.RED) {
            themecolor = ColorRGB(136, 8, 8)
            if (holeesptheme) {
                HoleESP.bedrockColor = themecolor
                HoleESP.obbyColor = unsafeholecolor
                HoleESP.trappedColor = unsafeholecolor
            }
            ClickGUI.primarySetting = themecolor
            ActiveModules.color1 = themecolor
            GradientWatermark.color1 = themecolor
            if (friendcolor) {
                TextRadar.friendcolor = themecolor
                Nametags.friendcolor = themecolor
            }
            if (packetminetheme) {
                PacketMine.useCustomColor = true
                PacketMine.customColor = themecolor
            }
            ClickGUI.backgroundSetting = bgcolor

        }
        if (theme.value == Themes.SKYBLUE) {
            themecolor = ColorRGB(135, 206, 235)
            if (holeesptheme) {
                HoleESP.bedrockColor = themecolor
                HoleESP.obbyColor = unsafeholecolor
                HoleESP.trappedColor = unsafeholecolor
            }
            ClickGUI.primarySetting = themecolor
            ActiveModules.color1 = themecolor
            GradientWatermark.color1 = themecolor
            if (friendcolor) {
                TextRadar.friendcolor = themecolor
                Nametags.friendcolor = themecolor
            }
            if (packetminetheme) {
                PacketMine.useCustomColor = true
                PacketMine.customColor = themecolor
            }
            ClickGUI.backgroundSetting = bgcolor

        }
        if (theme.value == Themes.PINK) {
            themecolor = ColorRGB(255, 204, 255)
            if (holeesptheme) {
                HoleESP.bedrockColor = themecolor
                HoleESP.obbyColor = unsafeholecolor
                HoleESP.trappedColor = unsafeholecolor
            }
            ClickGUI.primarySetting = themecolor
            ActiveModules.color1 = themecolor
            GradientWatermark.color1 = themecolor
            if (friendcolor) {
                TextRadar.friendcolor = themecolor
                Nametags.friendcolor = themecolor
            }
            if (packetminetheme) {
                PacketMine.useCustomColor = true
                PacketMine.customColor = themecolor
            }
            ClickGUI.backgroundSetting = bgcolor

        }
        if (theme.value == Themes.LIGHTGREEN) {
            themecolor = ColorRGB(204, 255, 204)
            if (holeesptheme) {
                HoleESP.bedrockColor = themecolor
                HoleESP.obbyColor = unsafeholecolor
                HoleESP.trappedColor = unsafeholecolor
            }
            ClickGUI.primarySetting = themecolor
            ActiveModules.color1 = themecolor
            GradientWatermark.color1 = themecolor
            if (friendcolor) {
                TextRadar.friendcolor = themecolor
                Nametags.friendcolor = themecolor
            }
            if (packetminetheme) {
                PacketMine.useCustomColor = true
                PacketMine.customColor = themecolor
            }
            ClickGUI.backgroundSetting = bgcolor

        }
    }

}