package com.minemev.redenmev



import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
import org.jetbrains.annotations.Contract
import org.slf4j.Logger

object Redenmev : ModInitializer{

	const val MOD_ID = "redenmev"
	const val MOD_NAME = "Reden MEV"
	@JvmField val LOGGER: Logger = LoggerFactory.getLogger(MOD_NAME)

	@JvmStatic
	@Contract("_ -> new")
	fun identifier(id: String): Identifier {
		return Identifier.of(MOD_ID, id)
	}
	val client = MinecraftClient.getInstance()

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		LOGGER.info("$MOD_NAME has been initialized!")

		// Register event handlers and commands here
		setupEventHandlers()
	}

	private fun setupEventHandlers() {
		// Handle server starting event
		ServerLifecycleEvents.SERVER_STARTING.register {
			LOGGER.info("Server is starting...")
			// Additional server-specific initialization can go here
		}

	}


}