package com.varabyte.kobweb.compose.internal.runtime

import androidx.compose.runtime.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.dom.clear
import org.jetbrains.compose.web.dom.DOMScope
import org.jetbrains.compose.web.internal.runtime.ComposeWebInternalApi
import org.jetbrains.compose.web.internal.runtime.GlobalSnapshotManager
import org.jetbrains.compose.web.internal.runtime.JsMicrotasksDispatcher
import org.jetbrains.compose.web.renderComposable
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.asList
import org.w3c.dom.get

@OptIn(ComposeWebInternalApi::class)
fun <TElement : Element> renderComposable(
    root: TElement,
    monotonicFrameClock: MonotonicFrameClock = DefaultMonotonicFrameClock,
    content: @Composable DOMScope<TElement>.() -> Unit
): Composition {
    GlobalSnapshotManager.ensureStarted()

    val context = monotonicFrameClock + JsMicrotasksDispatcher()
    val recomposer = Recomposer(context)

    CoroutineScope(context).launch(start = CoroutineStart.UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
    }

    val composition = ControlledComposition(
        applier = DomApplier(DomNodeWrapper(root)),
        parent = recomposer
    )
    val scope = object : DOMScope<TElement> {
        override val DisposableEffectScope.scopeElement: TElement
            get() = root
    }
    composition.setContent @Composable {
        content(scope)
    }
    return composition
}

@Suppress("UNCHECKED_CAST")
fun renderComposable(
    rootElementId: String,
    content: @Composable DOMScope<Element>.() -> Unit
): Composition = renderComposable(
    root = document.getElementById(rootElementId)!!,
    content = content
)

@ComposeWebInternalApi
private class DomApplier(
    root: DomNodeWrapper
) : AbstractApplier<DomNodeWrapper>(root) {
    private fun Node.indexOf(child: Node): Int {
        return childNodes.asList().indexOf(child)
    }

    private var isRemovalPending = false
    private val pendingRemovals = mutableSetOf<Node>()

    override fun insertTopDown(index: Int, instance: DomNodeWrapper) {
        // ignored. Building tree bottom-up
    }

    override fun insertBottomUp(index: Int, instance: DomNodeWrapper) {
        // Sometimes a "remove" is followed by an "insert", which is a fancy way to move an element. With the DOM APIs,
        // however, pulling something out of the tree and adding it back resets its state. You really want to use
        // "move" for that. So we track the node and delay the removal just a bit in case we detect this case.
        if (pendingRemovals.remove(instance.node)) {
            val fromIndex = current.node.indexOf(instance.node)
            current.move(fromIndex, index, 1)
        } else {
            current.insert(index, instance)
        }
    }

    override fun remove(index: Int, count: Int) {
        val wrapper = current // Create local copy for closure
        wrapper.node.childNodes.asList().subList(index, index + count).forEach {
            pendingRemovals.add(it)
        }
        if (!isRemovalPending) {
            isRemovalPending = true
            // Postpone the removal until the end of the current event loop, giving us a chance to intercept a followup
            // "insert" request.
            window.setTimeout({
                // If we got here and any nodes still in the removeNodesRequests list were not moved, so we can safely remove them now
                pendingRemovals.forEach { node ->
                    wrapper.node.indexOf(node).takeIf { it >= 0 }?.let { removeIndex ->
                        wrapper.remove(removeIndex, 1)
                    }
                }
                pendingRemovals.clear()
                isRemovalPending = false
            }, 0)
        }
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.move(from, to, count)
    }

    override fun onClear() {
        // or current.node.clear()?; in all examples it calls 'clear' on the root
        root.node.clear()
    }
}

@ComposeWebInternalApi
private class DomNodeWrapper(val node: Node) {
    fun insert(index: Int, nodeWrapper: DomNodeWrapper) {
        val length = node.childNodes.length
        if (index < length) {
            node.insertBefore(nodeWrapper.node, node.childNodes[index]!!)
        } else {
            node.appendChild(nodeWrapper.node)
        }
    }

    fun remove(index: Int, count: Int) {
        repeat(count) {
            node.removeChild(node.childNodes[index]!!)
        }
    }

    fun move(from: Int, to: Int, count: Int) {
        if (from == to) {
            return // nothing to do
        }

        for (i in 0 until count) {
            // if "from" is after "to," the from index moves because we're inserting before it
            val fromIndex = if (from > to) from + i else from
            val toIndex = if (from > to) to + i else to + count - 2

            val child = node.removeChild(node.childNodes[fromIndex]!!)
            node.insertBefore(child, node.childNodes[toIndex]!!)
        }
    }
}
