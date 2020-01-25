/**
 * Copyright 2020 Michel Kremer (kremi151)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lu.kremi151.chatster.core

import com.mojang.brigadier.CommandDispatcher
import lu.kremi151.chatster.api.annotations.Inject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

import lu.kremi151.chatster.api.plugin.ChatsterPlugin
import lu.kremi151.chatster.api.annotations.Plugin
import lu.kremi151.chatster.api.annotations.Provider
import lu.kremi151.chatster.api.command.CommandRegistry
import lu.kremi151.chatster.api.command.LiteralCommandBuilder
import lu.kremi151.chatster.core.command.builder.LiteralCommandBuilderImpl
import lu.kremi151.chatster.core.command.builder.RootCommandBuilderImpl
import lu.kremi151.chatster.core.config.Configurator
import lu.kremi151.chatster.core.plugin.CorePlugin
import lu.kremi151.chatster.core.registry.CommandRegistration
import lu.kremi151.chatster.core.registry.PluginRegistration
import lu.kremi151.chatster.core.registry.PluginRegistry
import lu.kremi151.chatster.core.services.CommandDispatcherHolder
import lu.kremi151.chatster.core.services.ConfidentialCredentialStore
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URLClassLoader
import java.util.jar.JarInputStream

open class Chatster {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        private const val MANIFEST_PLUGIN_CLASS = "Chatster-Plugin-Class"
    }

    @Inject
    private lateinit var commandDispatcherHolder: CommandDispatcherHolder

    fun launch() {
        LOGGER.info("Initializing Chatster")
        var initTime = System.currentTimeMillis()

        LOGGER.info("Loading configuration")
        var configTime = System.currentTimeMillis()
        loadGlobalConfig()
        configTime = System.currentTimeMillis() - configTime
        LOGGER.info("Loaded configuration in {} ms", configTime)

        LOGGER.info("Loading plugins")
        var pluginsTime = System.currentTimeMillis()
        val pluginRegistry = PluginRegistry()
        loadSystemPlugins(pluginRegistry)
        loadPlugins(pluginRegistry)
        pluginsTime = System.currentTimeMillis() - pluginsTime
        LOGGER.info("Loaded {} plugins in {} ms", pluginRegistry.size, pluginsTime)

        LOGGER.info("Configure plugins")
        configTime = System.currentTimeMillis()
        val configurator = Configurator(pluginRegistry)
        configurator.collectProviders(this)
        configurator.collectPluginProviders()
        configurator.autoConfigurePlugins()
        configurator.autoConfigure(this)
        configTime = System.currentTimeMillis() - configTime
        LOGGER.info("Configured plugins in {} ms", configTime)

        LOGGER.info("Setting up commands")
        var commandsTime = System.currentTimeMillis()
        val commandsList = ArrayList<CommandRegistration>()
        pluginRegistry.collectCommandProviders(commandsList)
        val commandDispatcher = commandDispatcherHolder.commandDispatcher
        for (provider in commandsList) {
            configurator.autoConfigure(provider.commandProvider)
            provider.commandProvider.registerCommands(RootCommandBuilderImpl(commandDispatcher), object : CommandRegistry {

                override fun registerCommand(rootNode: LiteralCommandBuilder) {
                    if (!LiteralCommandBuilderImpl::class.java.isAssignableFrom(rootNode.javaClass)) {
                        throw IllegalArgumentException("Unsupported command builder class ${rootNode.javaClass}")
                    }
                    commandDispatcher.register((rootNode as LiteralCommandBuilderImpl).appliedRootNode)
                }

            })
        }
        commandsTime = System.currentTimeMillis() - commandsTime
        LOGGER.info("Set up commands in {} ms", commandsTime)

        initTime = System.currentTimeMillis() - initTime
        LOGGER.info("Initialized Chatster in {} ms", initTime)
    }

    private fun createFolderIfNotExists(path: String): File {
        val folder = File(path)
        if (!folder.exists() && !folder.mkdirs()) {
            throw IOException("Could not create folder at $folder")
        }
        return folder
    }

    open val pluginsFolder: File get() = createFolderIfNotExists("plugins")
    open val configFolder: File get() = createFolderIfNotExists("config")

    protected open fun loadPlugins(registry: PluginRegistry) {
        val pluginsFolder = pluginsFolder
        val pluginJars = pluginsFolder.listFiles { _, name -> name.endsWith(".jar") }
        if (pluginJars == null || pluginJars.isEmpty()) {
            return
        }
        for (pluginJar in pluginJars) {
            var pluginClassName: String? = null
            JarInputStream(FileInputStream(pluginJar)).use { jarStream ->
                val manifest = jarStream.manifest
                val attributes = manifest.mainAttributes
                pluginClassName = attributes.getValue(MANIFEST_PLUGIN_CLASS)
            }
            if (pluginClassName == null || pluginClassName!!.isBlank()) {
                continue
            }
            try {
                val registration = loadPlugin(pluginJar, pluginClassName!!)
                registry.register(registration)
                LOGGER.info("Loaded plugin {} ({})", registration.name, registration.id)
            } catch (e: Exception) {
                LOGGER.warn("An error occurred while loading plugin at {}, skipping", pluginJar, e)
            }
        }
    }

    protected open fun loadSystemPlugins(registry: PluginRegistry) {
        registry.register(PluginRegistration(CorePlugin.ID, CorePlugin.NAME, CorePlugin()))
    }

    private fun loadPlugin(pluginJar: File, pluginClassName: String): PluginRegistration {
        val childClassLoader = URLClassLoader(
                arrayOf(pluginJar.toURI().toURL()),
                javaClass.classLoader
        )
        val clazz = Class.forName(pluginClassName, true, childClassLoader)
        if (!ChatsterPlugin::class.java.isAssignableFrom(clazz)) {
            throw IOException("Plugin class " + clazz.name + " does is no sub class of " + ChatsterPlugin::class.java.name)
        }
        val meta = clazz.getAnnotation(Plugin::class.java) ?: throw IOException("Plugin class " + clazz.name + " is not annotated with " + Plugin::class.java.name)
        val plugin = clazz.getConstructor().newInstance() as ChatsterPlugin
        return PluginRegistration(meta.id, meta.name, plugin)
    }

    protected open fun loadGlobalConfig() {
        // TODO: Implement
    }

    @Provider
    fun createCommandDispatcherHolder(): CommandDispatcherHolder {
        return CommandDispatcherHolder(CommandDispatcher())
    }

    @Provider
    fun createConfidentialCredentialStore(): ConfidentialCredentialStore {
        return ConfidentialCredentialStore()
    }

}