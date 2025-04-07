package dev.luna5ama.trollhack.module

import dev.luna5ama.trollhack.event.ListenerOwner
import dev.luna5ama.trollhack.event.events.ModuleToggleEvent
import dev.luna5ama.trollhack.graphics.color.ColorRGB
import dev.luna5ama.trollhack.setting.configs.NameableConfig
import dev.luna5ama.trollhack.setting.settings.AbstractSetting
import dev.luna5ama.trollhack.setting.settings.IMutableSetting
import dev.luna5ama.trollhack.setting.settings.ImmutableSetting
import dev.luna5ama.trollhack.setting.settings.SettingRegister
import dev.luna5ama.trollhack.setting.settings.impl.collection.CollectionSetting
import dev.luna5ama.trollhack.setting.settings.impl.collection.MapSetting
import dev.luna5ama.trollhack.setting.settings.impl.number.DoubleSetting
import dev.luna5ama.trollhack.setting.settings.impl.number.FloatSetting
import dev.luna5ama.trollhack.setting.settings.impl.number.IntegerSetting
import dev.luna5ama.trollhack.setting.settings.impl.other.BindSetting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.BooleanSetting
import dev.luna5ama.trollhack.setting.settings.impl.primitive.EnumSetting
import dev.luna5ama.trollhack.translation.ITranslateSrc
import dev.luna5ama.trollhack.translation.TranslateSrc
import dev.luna5ama.trollhack.translation.TranslateType
import dev.luna5ama.trollhack.util.Bind
import dev.luna5ama.trollhack.util.IDRegistry
import dev.luna5ama.trollhack.util.interfaces.Alias
import dev.luna5ama.trollhack.util.interfaces.DisplayEnum
import dev.luna5ama.trollhack.util.interfaces.Nameable
import dev.luna5ama.trollhack.util.text.NoSpamMessage
import net.minecraft.client.Minecraft

@Suppress("UNCHECKED_CAST")
open class AbstractModule(
    name: String,
    alias: Array<String> = emptyArray(),
    val category: Category,
    description: String,
    val modulePriority: Int = -1,
    var alwaysListening: Boolean = false,
    visible: Boolean = true,
    val alwaysEnabled: Boolean = false,
    val enabledByDefault: Boolean = false,
    private val config: NameableConfig<out Nameable>
) : ListenerOwner(),
    Nameable,
    Alias,
    SettingRegister<Nameable>,
    ITranslateSrc by TranslateSrc("module_${name.replace(" ", "_")}"),
    Comparable<AbstractModule> {

    final override val internalName = name.replace(" ", "")

    override val name = TranslateType.SPECIFIC key ("name" to name)
    override val alias: Array<String> = arrayOf(internalName, *alias)

    val description = TranslateType.SPECIFIC key ("description" to description)

    val id = idRegistry.register()

    private val enabled = BooleanSetting(settingName("Enabled"), false, { false }).also(::addSetting)
    val bind = BindSetting(settingName("Bind"), Bind(), { !alwaysEnabled }, {
        when (onHold.value) {
            OnHold.OFF -> if (it) toggle()
            OnHold.ENABLE -> toggle(it)
            OnHold.DISABLE -> toggle(!it)
        }
    }).also(::addSetting)
    private val onHold = EnumSetting(settingName("On Hold"), OnHold.OFF).also(::addSetting)
    private val visible = BooleanSetting(settingName("Visible"), visible).also(::addSetting)
    private val default = BooleanSetting(
        settingName("Default"),
        false,
        { settingList.isNotEmpty() },
        isTransient = true
    ).also(::addSetting)

    private enum class OnHold(override val displayName: CharSequence) : DisplayEnum {
        OFF(TranslateType.COMMON commonKey "Off"),
        ENABLE(TranslateType.COMMON commonKey "Enable"),
        DISABLE(TranslateType.COMMON commonKey "Disable")
    }

    val settingGroup get() = config.getGroupOrPut(this.internalName)
    val fullSettingList get() = config.getSettings(this)
    val settingList: List<AbstractSetting<*>> get() = fullSettingList.filter { it != bind && it != enabled && it != visible && it != default }

    val isEnabled: Boolean get() = enabled.value || alwaysEnabled
    val isDisabled: Boolean get() = !isEnabled
    val chatName: String get() = "[${name}]"
    val isVisible: Boolean get() = visible.value

    private fun addSetting(setting: AbstractSetting<*>) {
        (config as NameableConfig<Nameable>).addSettingToConfig(this, setting)
    }

    internal fun postInit() {
        enabled.value = enabledByDefault || alwaysEnabled
        if (alwaysListening) {
            subscribe()
        }
    }

    fun toggle(state: Boolean) {
        enabled.value = state
    }

    fun toggle() {
        enabled.value = !enabled.value
    }

    fun enable() {
        enabled.value = true
    }

    fun disable() {
        enabled.value = false
    }

    open fun isActive(): Boolean {
        return isEnabled || alwaysListening
    }

    open fun getHudInfo(): String {
        return ""
    }

    /* SETTING MANIPULATION FUNCTIONS */

    /**
     * Gets the raw setting object by name
     */
    fun getRawSetting(settingName: String): AbstractSetting<*>? {
        return fullSettingList.firstOrNull { it.name.equals(settingName) }
    }

    /**
     * Universal setting modifier that handles all setting types
     */
    fun setSettingValue(settingName: String, value: Any): Boolean {
        val setting = getRawSetting(settingName) ?: return false

        return when {
            // Handle BindSetting specially since it's immutable
            setting is BindSetting && value is Bind -> {
                setting.setValue(value.toString())
                true
            }

            // Handle mutable settings
            setting is IMutableSetting<*> -> {
                try {
                    when (value) {
                        is Number -> handleNumberSetting(setting, value)
                        else -> {
                            if (setting.valueClass.isInstance(value)) {
                                (setting as IMutableSetting<Any>).value = value
                                true
                            } else {
                                setting.setValue(value.toString())
                                true
                            }
                        }
                    }
                } catch (e: Exception) {
                    false
                }
            }

            // Handle other immutable settings
            else -> try {
                setting.setValue(value.toString())
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun handleNumberSetting(setting: AbstractSetting<*>, value: Number): Boolean {
        return when (setting) {
            is IntegerSetting -> { setting.value = value.toInt(); true }
            is FloatSetting -> { setting.value = value.toFloat(); true }
            is DoubleSetting -> { setting.value = value.toDouble(); true }
            else -> false
        }
    }

    /**
     * Type-safe version for known setting types
     */
    inline fun <reified T : Any> setSettingValueTyped(settingName: String, value: T): Boolean {
        val setting = getRawSetting(settingName) ?: return false

        return when {
            // Handle boolean settings
            T::class == Boolean::class && setting is BooleanSetting -> {
                setting.value = value as Boolean
                true
            }

            // Handle number settings
            T::class == Int::class && setting is IntegerSetting -> {
                setting.value = value as Int
                true
            }
            T::class == Float::class && setting is FloatSetting -> {
                setting.value = value as Float
                true
            }
            T::class == Double::class && setting is DoubleSetting -> {
                setting.value = value as Double
                true
            }

            // Handle special types
            T::class == Bind::class && setting is BindSetting -> {
                setting.setValue((value as Bind).toString())
                true
            }
            T::class == ColorRGB::class && setting.valueClass == ColorRGB::class -> {
                (setting as IMutableSetting<ColorRGB>).value = value as ColorRGB
                true
            }

            // Handle collections
            setting is CollectionSetting<*, *> && value is Collection<*> -> {
                try {
                    (setting as CollectionSetting<Any, *>).editValue {
                        it.clear()
                        it.addAll(value as Collection<Any>)
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            }

            // Handle maps
            setting is MapSetting<*, *, *> && value is Map<*, *> -> {
                try {
                    (setting as MapSetting<Any, Any, *>).value.clear()
                    (setting as MapSetting<Any, Any, *>).value.putAll(value as Map<Nothing, Nothing>)
                    true
                } catch (e: Exception) {
                    false
                }
            }

            // Fallback to universal setter
            else -> setSettingValue(settingName, value)
        }
    }

    /**
     * Gets the current value of a setting
     */
    fun <T : Any> getSettingValue(settingName: String): T? {
        return getRawSetting(settingName)?.value as? T
    }


    // For color settings
    fun setColorSetting(settingName: String, r: Int, g: Int, b: Int): Boolean {
        return setSettingValue(settingName, ColorRGB(r, g, b))
    }

    // For enum settings
    inline fun <reified T : Enum<T>> setEnumSetting(settingName: String, enumValue: T): Boolean {
        return setSettingValueTyped(settingName, enumValue)
    }

    // For collection settings
    fun <E> addToCollectionSetting(settingName: String, element: E): Boolean {
        val setting = getRawSetting(settingName) as? CollectionSetting<E, *> ?: return false
        return try {
            setting.add(element)
            true
        } catch (e: Exception) {
            false
        }
    }
    /* EVENT HANDLERS */

    protected fun onEnable(block: () -> Unit) {
        enabled.valueListeners.add { _, input ->
            if (input) {
                block()
            }
        }
    }

    protected fun onDisable(block: () -> Unit) {
        enabled.valueListeners.add { _, input ->
            if (!input) {
                block()
            }
        }
    }

    protected fun onToggle(block: (Boolean) -> Unit) {
        enabled.valueListeners.add { _, input ->
            block(input)
        }
    }

    override fun <S : AbstractSetting<*>> Nameable.setting(setting: S): S {
        (config as NameableConfig<Nameable>).addSettingToConfig(this, setting)
        return setting
    }

    final override fun settingName(input: CharSequence): CharSequence {
        return if (input is String) TranslateType.COMMON key input
        else input
    }

    override fun compareTo(other: AbstractModule): Int {
        val result = this.modulePriority.compareTo(other.modulePriority)
        if (result != 0) return result
        return this.id.compareTo(other.id)
    }

    init {
        enabled.consumers.add { prev, input ->
            val enabled = alwaysEnabled || input

            if (prev != input && !alwaysEnabled) {
                ModuleToggleEvent(this).post()
            }

            if (enabled || alwaysListening) {
                subscribe()
            } else {
                unsubscribe()
            }

            enabled
        }

        default.valueListeners.add { _, it ->
            if (it) {
                settingList.forEach { it.resetValue() }
                default.value = false
                NoSpamMessage.sendMessage(Companion, "$chatName $defaultMessage!")
            }
        }
    }

    protected companion object : ITranslateSrc by TranslateSrc("module") {
        val defaultMessage = TranslateType.COMMON key ("setToDefault" to "Set to defaults")

        private val idRegistry = IDRegistry()
        val mc: Minecraft = Minecraft.getMinecraft()
    }
}