package playground.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.core.PageContext
import com.varabyte.kobweb.silk.components.forms.TextInput
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import playground.utilities.setTitle

@Page
@Composable
fun HomePage(ctx: PageContext) {
    LaunchedEffect(Unit) { ctx.setTitle("Welcome to Kobweb!") }

    Text("Please enter your name")
    var name by remember { mutableStateOf("") }
    TextInput(name, onTextChange = { name = it })
    P()
    Text("Hello ${name.takeIf { it.isNotBlank() } ?: "World"}!")
}
