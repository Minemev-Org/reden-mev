package com.minemev.redenmev

import com.minemev.gui.WebTextureComponent
import com.minemev.redenmev.mixin.malilib.IMixinGuiListBase
import fi.dy.masa.litematica.gui.GuiSchematicLoad
import fi.dy.masa.litematica.gui.widgets.WidgetSchematicBrowser
import fi.dy.masa.malilib.gui.widgets.WidgetDirectoryEntry
import fi.dy.masa.malilib.gui.widgets.WidgetFileBrowserBase.DirectoryEntry
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.core.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.GameMenuScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen
import net.minecraft.client.gui.screen.world.WorldListWidget
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.commonmark.node.Document
import org.commonmark.parser.Parser
import java.io.IOException
import java.nio.file.Path
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.io.path.*

class MevDetails(
    horizontalSizing: Sizing,
    verticalSizing: Sizing,
    val parentScreen: MevScreen,
    var info: MevSearch.MevItem
) :
    FlowLayout(horizontalSizing, verticalSizing, Algorithm.VERTICAL) {
    @Serializable
    data class FileItem(
        val default_file_name: String,
        @SerialName("file")
        val url: String,
        val file_size: Int,
        val versions: List<String>,
        val downloads: Int,
        val file_type: String
    )

    private val client = MinecraftClient.getInstance()
    private val loadingLabel = Components.label(Text.literal("Loading image...").formatted(Formatting.GRAY))!!
    private val images = ArrayList<Component>(info.images.size).apply {
        for (i in 0 until info.images.size) this.add(loadingLabel)
    }
    private val imgContainer = Containers.horizontalFlow(Sizing.fill(100), Sizing.fill(50)).apply {
        horizontalAlignment(HorizontalAlignment.CENTER)
    }!!
    private val filesContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content()).apply {
    }!!
    private var details: Component = Components.label(Text.of(info.description)).apply {
        sizing(Sizing.fill(100), Sizing.content())
    }!!
        set(value) {
            val index = detailsContainer.children().indexOf(field)
            field.remove()
            field = value
            detailsContainer.child(index, value)
        }

    private val detailsContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content()).apply {
        child(Containers.collapsible(
            Sizing.fill(100), Sizing.content(), Text.literal("Details"), false
        ).apply {
            val detailsSubContainer = Containers.verticalFlow(Sizing.fill(100), Sizing.content())

            if (info.tags.isNotEmpty()) {
                val tagsLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                tagsLayout.child(
                    Components.label(
                        Text.literal("Tags: ").formatted(Formatting.GRAY)
                    )
                )
                tagsLayout.child(Components.label(Text.literal(info.tags.joinToString(", "))))
                detailsSubContainer.child(tagsLayout)
            }

            if (info.versions.isNotEmpty()) {
                val versionsLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                versionsLayout.child(
                    Components.label(
                        Text.literal("Versions: ").formatted(Formatting.GRAY)
                    )
                )
                versionsLayout.child(Components.label(Text.literal(info.versions.joinToString(", "))))
                detailsSubContainer.child(versionsLayout)
            }

            if (info.User.isNotEmpty()) {
                val userLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                userLayout.child(
                    Components.label(
                        Text.literal("User: ").formatted(Formatting.GRAY)
                    )
                )
                userLayout.child(Components.label(Text.literal(info.User).styled {
                    it.withClickEvent(
                        ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.minemev.com/u/" + info.User)
                    ).withHoverEvent(
                        HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Go to user Profile"))
                    )
                }.formatted(Formatting.AQUA)))
                detailsSubContainer.child(userLayout)
            }

            if (info.yt_link.isNotEmpty()) {
                val ytLinkLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                ytLinkLayout.child(
                    Components.label(
                        Text.literal("Video Link: ").formatted(Formatting.GRAY)
                    )
                )
                ytLinkLayout.child(Components.label(Text.literal(info.yt_link).styled {
                    it.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, info.yt_link))
                        .withHoverEvent(
                            HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Open Video Link"))
                        )
                }.formatted(Formatting.DARK_RED)))
                detailsSubContainer.child(ytLinkLayout)
            }

            if (info.downloads > 0) {
                val downloadsLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                downloadsLayout.child(
                    Components.label(
                        Text.literal("Downloads: ").formatted(Formatting.GRAY)
                    )
                )
                downloadsLayout.child(Components.label(Text.literal(info.downloads.toString())))
                detailsSubContainer.child(downloadsLayout)
            }


            this.child(detailsSubContainer)
        })
        child(details)
    }!!


    private val detailsReadMore = HoverLabelComponent(
        Text.literal("Read More").formatted(Formatting.UNDERLINE),
        Text.literal("Read More").formatted(Formatting.UNDERLINE).formatted(Formatting.AQUA)
    ).apply {
        mouseDown()!!.subscribe { _, _, b ->
            if (b == 0) {
                remove()
                detailsContainer.verticalSizing(Sizing.content())


                val parser: Parser = Parser.builder().build()
                val node = parser.parse(info.description_md) as Document

            }
            true
        }
    }

    open class HoverLabelComponent(private val defaultText: Text, private val hoverText: Text) :
        LabelComponent(defaultText) {
        private var hover = false

        init {
            this.cursorStyle(CursorStyle.HAND)
        }

        override fun draw(context: OwoUIDrawContext?, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
            if (hover != this.isInBoundingBox(mouseX.toDouble(), mouseY.toDouble())) {
                hover = this.isInBoundingBox(mouseX.toDouble(), mouseY.toDouble())
                text(if (hover) hoverText else defaultText)
            }
            super.draw(context, mouseX, mouseY, partialTicks, delta)
        }
    }

    private var imgId = 1
    private val imageInfoLabel = Components.label(Text.empty().formatted(Formatting.GRAY))!!
    private val btnPrev = Components.button(Text.literal("<")) {
        imgId--
        if (imgId < 1) imgId = info.images.size
    }!!
    private val btnNext = Components.button(Text.literal(">")) {
        imgId++
        if (imgId > info.images.size) imgId = 1
    }!!

    init {
        println(info.images.firstOrNull())
        httpClient.newCall(Request.Builder().apply {
            ua()
            get()
            url("https://minemev.com/api/details/${info.uuid}")
        }.build()).apply {
            Redenmev.LOGGER.info("Started request: ${request().url}")
        }.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (e.message != "Canceled") {
                    Redenmev.LOGGER.error("Failed request: ${call.request().url}", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body!!.use {
                    val string = it.string()
                    info = jsonIgnoreUnknown.decodeFromString<MevSearch.MevItem>(string)
                    client!!.execute {
                        details = Components.label(Text.of(info.description)).apply {
                            sizing(Sizing.fill(100), Sizing.content())
                        }
                        if (details.height() + 70 < detailsContainer.height()) {
                            detailsReadMore.onMouseDown(.0, .0, 0)
                        }
                    }
                }
            }
        })

        this.child(Components.label(Text.literal(info.post_name).styled {
            it.withClickEvent(ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.minemev.com/p/${info.uuid}"))
                .withHoverEvent(HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("View on minemev.com")))
        }).apply {
            margins(Insets.vertical(7))
            horizontalSizing(Sizing.fill(100))
            horizontalTextAlignment(HorizontalAlignment.CENTER)
        })
        this.child(
            Containers.verticalScroll(Sizing.fill(100), Sizing.fill(100),
                Containers.verticalFlow(Sizing.fill(100), Sizing.content()).apply {
                    if (info.images.isNotEmpty()) {
                        this.child(
                            Containers.horizontalFlow(Sizing.fill(100), Sizing.content()).apply {
                                child(btnPrev)
                                child(imageInfoLabel)
                                child(btnNext)
                                horizontalAlignment(HorizontalAlignment.CENTER)
                                verticalAlignment(VerticalAlignment.CENTER)
                            }
                        )

                        info.images.mapIndexed { index, url ->
                            TextureStorage.getImage(url, {
                                images[index] = WebTextureComponent.fixedHeight(it, 0, 0, imgContainer.height())
                            }) {
                                images[index] = Components.label(Text.literal("Failed: ${it.message}").formatted(Formatting.RED)).apply {
                                    sizing(Sizing.fill(100))
                                }
                            }
                        }

                        this.child(imgContainer)
                        this.child(detailsContainer)
                        this.child(detailsReadMore)
                        this.child(Components.label(Text.of("\nFile Downloads")))
                        this.child(filesContainer)
                        this.horizontalAlignment(HorizontalAlignment.CENTER)
                    }
                }
            ).apply {
                scrollbar(ScrollContainer.Scrollbar.vanillaFlat())
            }
        )

        httpClient.newCall(Request.Builder().apply {
            ua()
            get()
            url("https://www.minemev.com/api/files/${info.uuid}")
        }.build()).apply {
            Redenmev.LOGGER.info("Started request: ${request().url}")
        }.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                val fileItems = jsonIgnoreUnknown.decodeFromString<List<FileItem>>(response.body!!.use { it.string() })
                client!!.execute {

                    val litematicFiles = fileItems.filter { it.file_type == "litematic" }
                    val worldDownloadFiles = fileItems.filter { it.file_type == "world_download" }


                    filesContainer.clearChildren()


                    if (litematicFiles.isNotEmpty()) {
                        filesContainer.child(Components.label(Text.of("Litematics")).color(Color.ofRgb(0x00E0FF)))
                        litematicFiles.forEach { file ->
                            filesContainer.child(FileComponent(parentScreen, file))
                        }
                    }


                    if (worldDownloadFiles.isNotEmpty()) {
                        filesContainer.child(Components.label(Text.of("World Downloads")).color(Color.GREEN))
                        worldDownloadFiles.forEach { file ->
                            filesContainer.child(FileComponent(parentScreen, file))
                        }
                    }
                }
            }
        })
        this.surface(Surface.VANILLA_TRANSLUCENT)
    }

    class FileComponent(
        private val parentScreen: MevScreen,
        private val file: FileItem
    ) : HoverLabelComponent(getLabel(file, false), getLabel(file, true)) {
        companion object {
            private fun getUniqueFilename(file: FileItem, parent: Path): Path {

                val extensionFromUrl = file.url.substringAfterLast('.', "")
                val extension = if (file.file_type == "litematic") {
                    if (extensionFromUrl == "zip") {
                        ".zip"
                    } else {
                        "." + extensionFromUrl
                    }
                } else {
                    "." + mapOf("world_download" to "zip").getOrDefault(file.file_type, file.file_type)
                }

                val name = file.default_file_name.replace(".$extensionFromUrl", "")
                var path = parent.resolve("$name$extension")

                if (path.exists()) {
                    var i = 2
                    while (path.exists()) {
                        path = parent.resolve("$name($i)$extension")
                        i++
                    }
                }
                Redenmev.LOGGER.info("returning path: $path")
                return path
            }

            private fun getLabel(file: FileItem, hover: Boolean): MutableText {
                val label = Text.empty()


                label.append(Text.literal("↓ ").styled {
                    it.withColor(0x2196f3)
                })

                // Añadir el nombre del archivo con estilo
                label.append(Text.literal(file.default_file_name).styled {
                    (if (hover) {
                        it.withColor(0x2196f3)
                    } else it).withUnderline(true)
                })

                // Añadir la cantidad de descargas
                label.append(" ")
                label.append(Text.literal("${file.downloads} Downloads").formatted(Formatting.GRAY))

                return label
            }
        }

        init {
            margins(Insets.of(5))
            horizontalSizing(Sizing.fill(100))
            mouseDown().subscribe { _, _, b ->
                if (b == 0) {
                    val parent = Path("schematics", "reden-downloads")
                    parent.createDirectories()
                    val path = getUniqueFilename(file, parent)
                    httpClient.newCall(Request.Builder().apply {
                        ua()
                        get()
                        url(file.url)
                    }.build()).apply {
                        Redenmev.LOGGER.info("Started request: ${request().url}")
                    }.execute().body!!.use {
                        Redenmev.LOGGER.info("trying to save file on path: ${path}")
                        path.writeBytes(it.bytes())
                    }
                    runCatching {
                        when (file.file_type) {
                            "litematic" -> openLitematica(path)
                            "world_download" -> openWorld(path, file)
                            else -> error("Unknown file type: ${file.file_type}")
                        }
                    }.onFailure {
                        Redenmev.LOGGER.error("Error opening $path", it)
                        Util.getOperatingSystem().open(file.url)
                    }
                    true
                } else false
            }
        }

        private fun openWorld(zipPath: Path, file: FileItem) {
            val levelDat = ZipFile(zipPath.toFile()).entries().iterator().asSequence()
                .map { it.name }
                .filter { it.endsWith("level.dat") }.sortedBy { it.length }.firstOrNull()
                ?: error("Bad zip file: not a save")
            val prefix = levelDat.removeSuffix("level.dat")

            val path = getUniqueFilename(file.copy(file_type = "unzipped"), Path("saves"))
            ZipInputStream(zipPath.toFile().inputStream().buffered()).use { stream ->
                while (true) {
                    val entry = stream.nextEntry ?: break
                    if (!entry.isDirectory) {
                        path.resolve(entry.name.removePrefix(prefix))
                            .createParentDirectories()
                            .outputStream().buffered()
                            .use { out -> stream.copyTo(out) }
                    }
                }
            }
            if (parentScreen.client!!.networkHandler != null) {
                GameMenuScreen(false).apply {
                    init(MinecraftClient.getInstance(), width, height)
                }.disconnect()
            }
            val select = SelectWorldScreen(parentScreen)
            parentScreen.client!!.setScreen(select)
            select.levelList.levelsFuture.join()
            select.levelList.show(select.levelList.levelsFuture.getNow(null))
            val entry = select.levelList.children().firstOrNull {
                it is WorldListWidget.WorldEntry && it.level.name == path.name
            }
            select.levelList.setSelected(entry)
            if (entry != null) {
                val index = select.levelList.children().indexOf(entry)
                select.levelList.scrollAmount = select.levelList.getRowTop(index).toDouble() - 52
            }
        }

        private fun openLitematica(path: Path) {
            if (path.extension == "zip") {
                val unzipDir = path.parent.resolve(path.nameWithoutExtension)

                // Descomprime el archivo zip sin filtrar por .litematic
                ZipInputStream(path.inputStream().buffered()).use { zipStream ->
                    while (true) {
                        val entry = zipStream.nextEntry ?: break
                        val outputFile = unzipDir.resolve(entry.name)
                        if (entry.isDirectory) {
                            outputFile.createDirectories()
                        } else {
                            outputFile.parent?.createDirectories() // Asegúrate de que los directorios padres existan
                            outputFile.outputStream().buffered().use { out ->
                                zipStream.copyTo(out)
                            }
                        }
                    }
                }

                val unzippedPath = unzipDir
                Redenmev.LOGGER.info("Unzipped files to: $unzippedPath")

                // Carga la pantalla de schematics, sin buscar directamente .litematic
                loadLitematicScreen(unzippedPath)

            } else if (path.extension == "litematic") {
                // Si es un archivo litematic, cargar directamente el archivo.
                loadLitematicFromFile(path)

            } else {
                // Si no es ni zip ni litematic, abrir la pantalla y enviar un mensaje de error
                loadLitematicScreen(path.parent)
                Redenmev.LOGGER.error("Unable to open or decompress file: ${path.name}. Try doing it manually.")
                sendErrorMessageToChat("Unable to open or decompress file: ${path.name}. Try doing it manually.")
            }
        }

        private fun loadLitematicScreen(directory: Path) {
            val guiSchematicLoad = GuiSchematicLoad()
            guiSchematicLoad.parent = parentScreen
            parentScreen.client!!.setScreen(guiSchematicLoad)

            @Suppress("UNCHECKED_CAST", "KotlinConstantConditions")
            val schematicBrowser =
                (guiSchematicLoad as IMixinGuiListBase<DirectoryEntry,
                        WidgetDirectoryEntry, WidgetSchematicBrowser>).`widget$reden`()

            schematicBrowser.switchToDirectory(directory.toFile())
        }

        private fun loadLitematicFromFile(path: Path) {
            loadLitematicScreen(path.parent)
            val schematicBrowser =
                (parentScreen.client!!.currentScreen as IMixinGuiListBase<DirectoryEntry,
                        WidgetDirectoryEntry, WidgetSchematicBrowser>).`widget$reden`()

            val entry = schematicBrowser.currentEntries.firstOrNull { it.name == path.name }
            if (entry != null) {
                schematicBrowser.setLastSelectedEntry(entry, schematicBrowser.currentEntries.indexOf(entry))
            }
        }

        private fun sendErrorMessageToChat(message: String) {
            parentScreen.client!!.player?.sendMessage(Text.literal(message),true)
        }
    }

    override fun draw(context: OwoUIDrawContext?, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        imgContainer.child(0, images[imgId - 1])
        while (imgContainer.children().size > 1) {
            imgContainer.removeChild(imgContainer.children()[1])
        }
        imageInfoLabel.text(Text.literal("Image $imgId / ${info.images.size}"))

        super.draw(context, mouseX, mouseY, partialTicks, delta)
    }
}
