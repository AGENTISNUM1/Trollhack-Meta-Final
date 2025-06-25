
import dev.luna5ama.trollhack.module.Category
import dev.luna5ama.trollhack.module.Module
import dev.luna5ama.trollhack.util.text.NoSpamMessage

internal object Compatibility : Module(
    name = "Compatibility",
    description = "Makes the client compatible with other clients",
    visible = false,
    category = Category.CLIENT,
    alwaysEnabled = true
) {
    // Big wip
    var otherprefix by setting("Client Prefix", "Default")
    var othertogglecmd by setting("Toggle Command", "Default")

    fun getotherprefix(): String {
        if (otherprefix == "Default") {
            NoSpamMessage.sendWarning("The alternative prefix has not been set. Please set this in the compatibility module settings.")
            return "."
        }
        else {
            return otherprefix
        }
    }
    fun gettogglecmd(): String {
        if (othertogglecmd == "Default") {
            NoSpamMessage.sendWarning("The alternative toggle command has not been set. Please set this in the compatibility module settings.")
            return "toggle"
        }
        else {
            return othertogglecmd
        }
    }
}