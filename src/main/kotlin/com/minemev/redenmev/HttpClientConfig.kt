package com.minemev.redenmev

import kotlinx.serialization.json.Json
import net.minecraft.MinecraftVersion
import okhttp3.*
import okhttp3.internal.userAgent
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration


val key = ""


val httpClient = OkHttpClient.Builder().apply {
    readTimeout(60.seconds.toJavaDuration())
    cache(
        Cache(
            directory = File(".cache", "reden"),
            maxSize = 100L * 1024L * 1024L // 100 MiB
        )
    )
    Logger.getLogger(OkHttpClient::class.java.name).level = Level.FINE
}.build()

fun Request.Builder.ua() = apply {
    header("Authorization", "ApiKey $key")
    header("User-Agent", "RedenMC/ Minecraft/${MinecraftVersion.create().name} (Fabric) $userAgent")
}

val jsonIgnoreUnknown = Json { ignoreUnknownKeys = true }