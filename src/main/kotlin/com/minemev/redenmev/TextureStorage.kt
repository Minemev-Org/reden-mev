package com.minemev.redenmev


import com.minemev.gui.WebTexture

import net.minecraft.client.MinecraftClient
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO

object TextureStorage {
    private val cache = mutableMapOf<String, Result<WebTexture>>()

    fun getImage(url: String, action: (WebTexture) -> Unit) {
        getImage(url, action) {}
    }

    fun getImage(url: String, action: (WebTexture) -> Unit, failed: (Throwable) -> Unit) {
        MinecraftClient.getInstance().execute {
            httpClient.newCall(Request.Builder().apply {
                ua()
                get()
                url(url)
            }.build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (e.message != "Canceled") {
                        Redenmev.LOGGER.error("Failed request: ${call.request().url}", e)
                        MinecraftClient.getInstance().execute {
                            failed(e)
                        }
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!response.isSuccessful) return
                        var bytes = response.body!!.bytes()
                        try {
                            if (response.header("content-type") != "image/png") {
                                val image = ImageIO.read(bytes.inputStream())
                                if (image == null) {
                                    cache[url] =
                                        Result.failure(Exception("unknown image format: " + response.header("content-type")))
                                }
                                ByteArrayOutputStream().use {
                                    ImageIO.write(image, "png", it)
                                    bytes = it.toByteArray()
                                }
                            }
                            val texture = WebTexture(bytes)
                            texture.load(MinecraftClient.getInstance().resourceManager)
                            MinecraftClient.getInstance().execute {
                                cache[url] = Result.success(texture)
                                action(texture)
                            }
                        } catch (e: Throwable) {
                            cache[url] = Result.failure(e)
                            MinecraftClient.getInstance().execute {
                                failed(e)
                            }
                            Redenmev.LOGGER.error("Error reading image: $url", e)
                        }
                    }
                }
            })
        }
    }
}
