package com.efemoney.artistik

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.support.annotation.DrawableRes
import android.support.v7.content.res.AppCompatResources
import com.efemoney.artistik.StateAwareBuilder.State.Single
import kotlin.math.abs

@DslMarker
annotation class Dsl

@Dsl
class DrawableBoundsBuilder
internal constructor(var left: Int = 0, var top: Int = 0, var right: Int = 0, var bottom: Int = 0)

@Dsl
interface DrawableBuilder<out D : Drawable> {

    fun bounds(left: Int = 0, top: Int = 0,
               right: Int = 0, bottom: Int = 0,
               configure: DrawableBoundsBuilder.() -> Unit = { })

    fun build(): D
}

@Dsl
abstract class StateAwareBuilder<in T> {

    fun app(state: Int) = Single(state)
    fun attr(state: Int) = Single(state)
    fun custom(state: Int) = Single(state)

    fun item(vararg states: State) =
            if (states.isEmpty()) wildcard else states.reduce { acc, i -> acc and i }
    fun state(vararg states: State) =
            if (states.isEmpty()) wildcard else states.reduce { acc, i -> acc and i }

    abstract infix fun State.to(item: T)

    // States
    val aboveAnchor   get() = Single(android.R.attr.state_above_anchor)
    val accelerated   get() = Single(android.R.attr.state_accelerated)
    val activated     get() = Single(android.R.attr.state_activated)
    val active        get() = Single(android.R.attr.state_active)
    val checkable     get() = Single(android.R.attr.state_checkable)
    val checked       get() = Single(android.R.attr.state_checked)
    val dragCanAccept get() = Single(android.R.attr.state_drag_can_accept)
    val dragHovered   get() = Single(android.R.attr.state_drag_hovered)
    val empty         get() = Single(android.R.attr.state_empty)
    val enabled       get() = Single(android.R.attr.state_enabled)
    val expanded      get() = Single(android.R.attr.state_expanded)
    val first         get() = Single(android.R.attr.state_first)
    val focused       get() = Single(android.R.attr.state_focused)
    val hovered       get() = Single(android.R.attr.state_hovered)
    val last          get() = Single(android.R.attr.state_last)
    val longPressable get() = Single(android.R.attr.state_long_pressable)
    val middle        get() = Single(android.R.attr.state_middle)
    val multiline     get() = Single(android.R.attr.state_multiline)
    val pressed       get() = Single(android.R.attr.state_pressed)
    val selected      get() = Single(android.R.attr.state_selected)
    val single        get() = Single(android.R.attr.state_single)
    val windowFocused get() = Single(android.R.attr.state_window_focused)

    val wildcard = State.Set(mutableListOf())
    val nothing = Single(0)

    sealed class State {

        abstract infix fun and(state: State): State

        abstract fun get(): IntArray

        class Single(internal var value: Int) : State() {

            operator fun not() = this.apply { value = -abs(value) }
            operator fun unaryPlus() = this.apply { value = abs(value) }
            operator fun unaryMinus() = this.apply { value = -abs(value) }

            override fun and(state: State): State = when (state) {
                is Single -> Set(mutableListOf(value, state.value))
                is Set -> state.and(this)
            }

            override fun get() = intArrayOf(value)
        }

        class Set(private val values: MutableList<Int>) : State() {

            override fun and(state: State): State = when (state) {
                is Single -> this.apply { values += state.value }
                is Set -> this.apply { values += state.values }
            }

            override fun get() = values.toIntArray()
        }
    }
}

abstract class StateAwareDrawableBuilder<out D : Drawable> :
        StateAwareBuilder<Drawable>(),
        DrawableBuilder<D>

abstract class StateAwareColorBuilder : StateAwareBuilder<Int>()



class ColorStateListBuilder : StateAwareColorBuilder() {

    private val s = arrayListOf<IntArray>()
    private val c = arrayListOf<Int>()

    override fun State.to(item: Int) {
        s.add(this.get())
        c.add(item)
    }

    fun build() = ColorStateList(s.toTypedArray(), c.toIntArray())
}

class StateListBuilder : StateAwareDrawableBuilder<StateListDrawable>() {

    private val d = StateListDrawable()

    override fun bounds(left: Int, top: Int, right: Int, bottom: Int,
                        configure: DrawableBoundsBuilder.() -> Unit) {
        val builder = DrawableBoundsBuilder(left, top, right, bottom)
        builder.configure()
        d.setBounds(builder.left, builder.top, builder.right, builder.bottom)
    }

    override infix fun State.to(item: Drawable) = d.addState(get(), item)

    operator fun plus(item: Drawable) = d.addState(wildcard.get(), item)

    override fun build() = d
}

class LayerListBuilder : DrawableBuilder<LayerDrawable> {

    private val b = Rect()
    private val d = arrayListOf<Drawable>()

    infix fun <D : Drawable> to(item: D) { d += item }

    override fun bounds(left: Int, top: Int, right: Int, bottom: Int,
                        configure: DrawableBoundsBuilder.() -> Unit) {
        val builder = DrawableBoundsBuilder(left, top, right, bottom)
        builder.configure()
        b.set(builder.left, builder.top, builder.right, builder.bottom)
    }

    override fun build() = LayerDrawable(d.toTypedArray()).apply { if (!b.isEmpty) bounds = b }

    class LayerListItemBuilder {
        val id = -1
        val idx = -1
        val dr = null
    }
}



fun colorStateList(configure: ColorStateListBuilder.() -> Unit): ColorStateList {
    val builder = ColorStateListBuilder()
    builder.configure()
    return builder.build()
}

fun stateList(configure: StateListBuilder.() -> Unit): StateListDrawable {
    val builder = StateListBuilder()
    builder.configure()
    return builder.build()
}

fun layerList(configure: LayerListBuilder.() -> Unit): LayerDrawable {
    val builder = LayerListBuilder()
    builder.configure()
    return builder.build()
}

fun color(color: Int): ColorDrawable = ColorDrawable(color)

fun drawable(context: Context, @DrawableRes resId: Int) =
        AppCompatResources.getDrawable(context, resId) ?: throw Resources.NotFoundException()

@JvmName("drawableRes")
fun Context.drawable(@DrawableRes resId: Int) = drawable(this, resId)