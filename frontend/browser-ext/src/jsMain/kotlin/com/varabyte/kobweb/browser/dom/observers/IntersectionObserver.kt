package com.varabyte.kobweb.browser.dom.observers

import com.varabyte.kobweb.browser.dom.observers.externals.IntersectionObserverEntry
import org.w3c.dom.DOMRectReadOnly
import org.w3c.dom.Element
import kotlin.js.json
import com.varabyte.kobweb.browser.dom.observers.externals.IntersectionObserver as ActualIntersectionObserver

/**
 * Provides a way to asynchronously observe changes in the intersection of a target element with an ancestor element.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/API/Intersection_Observer_API">Intersection Observer API</a>
 */
class IntersectionObserver(options: Options? = null, resized: (List<Entry>, IntersectionObserver) -> Unit) {
    constructor(options: Options? = null, resized: (List<Entry>) -> Unit) : this(
        options,
        { entries, _ -> resized(entries) })

    private val _actualObserver = ActualIntersectionObserver({ actualEntries, _ ->
        resized.invoke(actualEntries.map { Entry.from(it) }, this)
    }, options?.toJson())

    data class Options(
        val root: Element? = null,
        val rootMargin: String? = null,
        val thresholds: List<Double>? = null,
    ) {
        companion object {} // Created so compose-html-ext can extend it

        internal fun toJson() = json().apply {
            root?.let { this["root"] = it }
            rootMargin?.let { this["rootMargin"] = it.toString() }
            thresholds?.takeIf { it.isNotEmpty() }?.let { this["threshold"] = it.toTypedArray() }
        }
    }

    data class Entry(
        val target: Element,
        val boundingClientRect: DOMRectReadOnly,
        val intersectionRatio: Double,
        val intersectionRect: DOMRectReadOnly,
        val isIntersecting: Boolean,
        val rootBounds: DOMRectReadOnly,
    ) {
        companion object {
            internal fun from(actualEntry: IntersectionObserverEntry) = Entry(
                actualEntry.target,
                actualEntry.boundingClientRect,
                actualEntry.intersectionRatio,
                actualEntry.intersectionRect,
                actualEntry.isIntersecting,
                actualEntry.rootBounds
            )
        }
    }

    val root: Element? get() = _actualObserver.root as? Element
    val rootMargin get() = _actualObserver.rootMargin
    val thresholds: List<Double> get() = _actualObserver.thresholds.toList()

    fun observe(element: Element): Unit = _actualObserver.observe(element)
    fun unobserve(element: Element): Unit = _actualObserver.unobserve(element)
    fun disconnect(): Unit = _actualObserver.disconnect()
    fun takeRecords(): List<Entry> = _actualObserver.takeRecords().map { Entry.from(it) }
}
