package com.minemev.redenmev

import com.minemev.redenmev.Redenmev.MOD_NAME
import com.minemev.redenmev.Redenmev.LOGGER
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import org.lwjgl.glfw.GLFW

class RedenmevClient : ClientModInitializer {
    private lateinit var openMevScreenKey: KeyBinding

    override fun onInitializeClient() {
        LOGGER.info("Hello Technitians of TMC")

        // register hotkey
        openMevScreenKey = KeyBindingHelper.registerKeyBinding(
            KeyBinding(
                "Open Minemev GUI",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                MOD_NAME
            )
        )

        // register the click event
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client ->
            if (openMevScreenKey.wasPressed()) {
                client.setScreen( MevScreen())
            }
        })
    }
}