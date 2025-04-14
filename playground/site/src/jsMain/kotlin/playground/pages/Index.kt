package playground.pages

import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.css.Overflow
import com.varabyte.kobweb.compose.foundation.layout.Column
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.graphics.Colors
import com.varabyte.kobweb.compose.ui.modifiers.backgroundColor
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxWidth
import com.varabyte.kobweb.compose.ui.modifiers.gap
import com.varabyte.kobweb.compose.ui.modifiers.overflow
import com.varabyte.kobweb.compose.ui.modifiers.padding
import com.varabyte.kobweb.compose.ui.modifiers.size
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.core.Page
import com.varabyte.kobweb.silk.components.forms.Button
import com.varabyte.kobweb.silk.components.navigation.Link
import com.varabyte.kobweb.silk.components.text.SpanText
import org.jetbrains.compose.web.css.cssRem
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import kotlin.random.Random


@Composable
fun BaseLayout(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 2.cssRem).gap(0.5.cssRem),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SpanText(Random.nextInt().toString())
        Div(Modifier.size(200.px, 100.px).overflow(Overflow.Auto).toAttrs()) {
            repeat(25) {
                Div {
                    Text("Hello $it")
                }
            }
        }

        content()
    }
}

@Composable
fun NestedLayout(content: @Composable () -> Unit) {
    content()
    SpanText("Nested")
}

@Composable
private fun PageHandler(pageIndex: Int, changePageTo: (Int) -> Unit) {
    SpanText("Page $pageIndex")
    Button(onClick = {
        changePageTo(pageIndex - 1)
    }, enabled = pageIndex > 1) {
        Text("Prev")
    }
    Button(onClick = {
        changePageTo(pageIndex + 1)
    }, enabled = pageIndex < 4) {
        Text("Next")
    }
}

@Composable
fun Page1(changePageTo: (Int) -> Unit) {
    PageHandler(1, changePageTo)
}

@Composable
fun Page2(changePageTo: (Int) -> Unit) {
    PageHandler(2, changePageTo)
}

@Composable
fun Page3(changePageTo: (Int) -> Unit) {
    PageHandler(3, changePageTo)
}

@Composable
fun Page4(changePageTo: (Int) -> Unit) {
    PageHandler(4, changePageTo)
}

@Page
@Composable
fun HomePage() {
    var pageIndex by remember { mutableStateOf(1) }
    val layoutMap: Map<String, @Composable (@Composable () -> Unit) -> Unit> = remember {
        mapOf(
            "base" to { content -> BaseLayout(content) },
            "nested" to { content -> NestedLayout(content) },
        )
    }

    val layoutFor = remember {
        mapOf(
            "1" to "base",
            "2" to "base",
            "3" to "nested",
            "4" to "base",
            "nested" to "base",
        )
    }

    val changePageTo: (Int) -> Unit = { newIndex ->
        pageIndex = newIndex
    }

    val pageFor: Map<String, @Composable () -> Unit> = remember {
        mapOf(
            "1" to @Composable { Page1(changePageTo) },
            "2" to @Composable { Page2(changePageTo) },
            "3" to @Composable { Page3(changePageTo) },
            "4" to @Composable { Page4(changePageTo) },
        )
    }

    val pageName = pageIndex.toString()
    var layoutName: String? = layoutFor.getValue(pageName)

    val layouts: List<@Composable (@Composable () -> Unit) -> Unit> = buildList {
        while (layoutName != null) {
            add(0, layoutMap.getValue(layoutName))
            layoutName = layoutFor[layoutName]
        }
    }

    layouts.foldRight(pageFor.getValue(pageName)) { layout, accum ->
        { -> layout(accum) }
    }.invoke()
}
