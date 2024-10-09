package com.minemev.redenmev

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.OwoUIAdapter
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import net.minecraft.client.gui.DrawContext

class MevScreen : BaseOwoScreen<FlowLayout>() {
    override fun createAdapter() = OwoUIAdapter.create(this, Containers::horizontalFlow)!!
    override fun build(rootComponent: FlowLayout) {
        rootComponent.surface(Surface.VANILLA_TRANSLUCENT)
        rootComponent.gap(5)
        rootComponent.child(MevSearch(Sizing.fill(50), Sizing.fill(100), this))
    }


    fun updateRight(component: Component) {
        this.uiAdapter.rootComponent.children().drop(1).forEach { it.remove() }
        this.uiAdapter.rootComponent.child(component)
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        super.render(context, mouseX, mouseY, delta)
        context.drawVerticalLine(
            width / 2 + 2,
            0,
            height,
            0x7fffffff
        )
    }
}
