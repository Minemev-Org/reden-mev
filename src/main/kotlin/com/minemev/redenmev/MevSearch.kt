package com.minemev.redenmev


import com.minemev.gui.WebTextureComponent
import com.minemev.redenmev.Redenmev.client
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.core.*
import io.wispforest.owo.ui.util.UISounds
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.client.MinecraftClient
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class MevSearch(horizontalSizing: Sizing, verticalSizing: Sizing, val screen: MevScreen) :
    FlowLayout(horizontalSizing, verticalSizing, Algorithm.VERTICAL) {
    var list = mutableListOf<MevItem>()
    val listComponent = Containers.verticalFlow(Sizing.fill(100), Sizing.content())!!.apply {
        horizontalAlignment(HorizontalAlignment.CENTER)
    }
    val search = Components.textBox(Sizing.fill(100))!!.apply {
        setPlaceholder(Text.literal("Search..."))
        onChanged().subscribe {
            page = 1
            httpClient.dispatcher.cancelAll()
            listComponent.clearChildren()
            doRequest()
        }
    }
    var page = 1
    var totalPages = 1

    @Serializable
    class MevItem(
        val post_name: String,
        val uuid: String,
        val description: String,
        val description_md: String? = null,
        val tags: List<String>,
        val versions: List<String>,
        val User: String,
        val yt_link: String,
        val downloads: Long,
        val images: List<String>,
        val published_at: Instant,
        @Transient
        var display: FlowLayout? = null
    )

    @Serializable
    class MevSearch(
        val posts: List<MevItem>,
        val total_pages: Int
    )

    inner class PostComponent(val mev: MevItem, val isLast: Boolean) :
        FlowLayout(Sizing.fill(98), Sizing.fixed(45), Algorithm.HORIZONTAL) {
        private val nameLabel = Components.label(Text.literal(mev.post_name))

        init {
            padding(Insets.of(2))
            child(Containers.verticalFlow(Sizing.fixed(40), Sizing.fixed(40)))
            child(
                Containers.verticalFlow(Sizing.content(), Sizing.fixed(40)).apply {
                    this.child(nameLabel)
                    this.child(Components.label(
                        Text.literal("by ").formatted(Formatting.WHITE)
                            .append(Text.literal(mev.User).formatted(Formatting.GOLD))
                    ))
                    this.child(Containers.horizontalFlow(Sizing.fill(100), Sizing.content()).apply {
                        mev.tags.forEach { tag ->
                            child(Components.label(Text.literal(tag).formatted(Formatting.GRAY)).apply {
                                horizontalSizing(Sizing.content())
                                gap(4)

                                mouseDown()!!.subscribe { _, _, b ->
                                    if (b == 0) {
                                        search.text(tag)
                                    }
                                    true
                                }

                                // Añadir mouseEnter para cambiar el estilo
                                mouseEnter()!!.subscribe {
                                    this.text(Text.literal(tag).formatted(Formatting.AQUA, Formatting.UNDERLINE))
                                    tooltip(Text.literal("Search ${tag} Tag"))
                                }

                                // Añadir mouseExit para restaurar el estilo original
                                mouseLeave()!!.subscribe {
                                    this.text(Text.literal(tag).formatted(Formatting.GRAY))
                                    tooltip(null)
                                }
                            })
                        }
                    })
                    gap(2)
                }
            )
            gap(5)
            margins(Insets.vertical(1))
            mouseDown().subscribe { _, _, b ->
                if (b == 0) {
                    UISounds.playButtonSound()
                    screen.updateRight(MevDetails(Sizing.fill(50), Sizing.fill(100), screen, mev))
                    true
                } else false
            }
            mev.display = this

            if (mev.images.isNotEmpty()) {
                val size = screen.client!!.options.guiScale.value * 40 * 2
                TextureStorage.getImage("https://www.minemev.com/api/preview/${mev.uuid}?size=$size") {
                    val imageContainer = Containers.verticalFlow(Sizing.fixed(41), Sizing.fixed(41)).apply {
                        surface(Surface.flat(0x20FFFFFF).and(Surface.outline(0x80FFFFFF.toInt()))) // Aplica el Surface aquí
                        padding(Insets.of(1))
                        child(WebTextureComponent(it, 0, 0, 40, 40))
                    }

                    this.children().first().remove()
                    this.child(0, imageContainer)


                }
            }
        }

        val currentPage = page

        override fun draw(
            context: OwoUIDrawContext,
            mouseX: Int,
            mouseY: Int,
            partialTicks: Float,
            delta: Float
        ) {

            //verify if the mouse is inside the component
            val isHovering = isInBoundingBox(mouseX.toDouble(), mouseY.toDouble())
            //change on hovering
            if (isHovering) {
                this.surface(Surface.flat(0x20FFFFFF).and(Surface.outline(0x80FFFFFF.toInt())))
            } else {
                this.surface(Surface.flat(0x00FFFFFF).and(Surface.outline(0x00FFFFFF))) // Cambia a un fondo
            }



            super.draw(context, mouseX, mouseY, partialTicks, delta)

            // pagination
            if (isLast && currentPage == page && page != totalPages) {
                page++
                doRequest()
            }
        }
    }

    private fun doRequest() {
        val requestStart = System.currentTimeMillis()
        httpClient.newCall(Request.Builder().apply {
            ua()
            get()
            url("https://minemev.com/api/search?search=${search.text}&page=$page")
        }.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (e.message != "Canceled") {
                    Redenmev.LOGGER.error("Failed making a search: ${search.text}", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body!!.use {
                    val mevSearch = jsonIgnoreUnknown.decodeFromString<MevSearch>(it.string())
                    MinecraftClient.getInstance().execute {
                        if (page == 1) {
                            listComponent.clearChildren()
                            list.clear()
                        }
                        list.addAll(mevSearch.posts)
                        totalPages = mevSearch.total_pages

                        mevSearch.posts.forEachIndexed { index, mevItem ->
                            listComponent.child(PostComponent(mevItem, index == mevSearch.posts.size - 1))
                        }
                        if (list.isEmpty()) {
                            listComponent.child(
                                Components.label(Text.literal("Sorry, didn't found anything."))
                            )
                        }
                    }
                }
            }
        })
    }

    init {
        listComponent.child(
            Components.label(Text.literal("Loading content..."))
        )
        doRequest()

        this.horizontalAlignment(HorizontalAlignment.CENTER)
        this.children(
            listOf(
                search,
                Containers.verticalScroll(Sizing.fill(100), Sizing.fill(95), listComponent).apply {
                    scrollbar(ScrollContainer.Scrollbar.vanillaFlat())

                }
            )
        )

    }
}
