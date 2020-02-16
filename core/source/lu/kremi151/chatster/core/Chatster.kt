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

import com.fasterxml.jackson.databind.ObjectMapper
import com.mojang.brigadier.CommandDispatcher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

import lu.kremi151.chatster.api.plugin.ChatsterPlugin
import lu.kremi151.chatster.api.annotations.Plugin
import lu.kremi151.chatster.api.command.CommandRegistry
import lu.kremi151.chatster.api.command.LiteralCommandBuilder
import lu.kremi151.chatster.api.message.Message
import lu.kremi151.chatster.api.profile.ProfileFactory
import lu.kremi151.chatster.api.profile.ProfileLauncher
import lu.kremi151.chatster.api.service.MessageHandler
import lu.kremi151.chatster.core.command.builder.LiteralCommandBuilderImpl
import lu.kremi151.chatster.core.command.builder.RootCommandBuilderImpl
import lu.kremi151.chatster.core.config.StaticBeanFactory
import lu.kremi151.chatster.core.context.CommandContextImpl
import lu.kremi151.chatster.core.context.ProfileContext
import lu.kremi151.chatster.core.plugin.CorePlugin
import lu.kremi151.chatster.core.registry.CommandRegistration
import lu.kremi151.chatster.core.registry.PluginRegistration
import lu.kremi151.chatster.core.registry.PluginRegistry
import lu.kremi151.chatster.core.services.CommandDispatcherHolder
import lu.kremi151.chatster.core.services.CommandMessageHandler
import lu.kremi151.chatster.core.services.PlaintextCredentialStore
import lu.kremi151.chatster.core.threading.ProfileThread
import lu.kremi151.chatster.core.threading.RunningProfilesState
import lu.kremi151.jector.AutoConfigurator
import lu.kremi151.jector.Jector
import lu.kremi151.jector.annotations.Inject
import lu.kremi151.jector.annotations.Provider
import lu.kremi151.jector.enums.Priority
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.concurrent.Executors
import java.util.jar.JarInputStream

open class Chatster {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        private const val MANIFEST_PLUGIN_CLASS = "Chatster-Plugin-Class"
    }

    @Inject
    private lateinit var commandDispatcherHolder: CommandDispatcherHolder

    @Inject
    private lateinit var objectMapper: ObjectMapper

    @Inject(collectionType = ProfileFactory::class)
    private lateinit var profileFactories: List<ProfileFactory<*>>

    @Inject(collectionType = MessageHandler::class)
    private lateinit var messageHandlers: List<MessageHandler>

    private var stopping: Boolean = false
    private val runningProfiles = RunningProfilesState()
    private val workerExecutor = Executors.newFixedThreadPool(4)
    private val profileLauncherExecutor = Executors.newSingleThreadExecutor()

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
        val pluginRegistry = PluginRegistry(configFolder)
        loadSystemPlugins(pluginRegistry)
        val pluginClassLoader = loadPlugins(pluginRegistry)
        pluginsTime = System.currentTimeMillis() - pluginsTime
        LOGGER.info("Loaded {} plugins in {} ms", pluginRegistry.size, pluginsTime)

        LOGGER.info("Pre-initialize plugins")
        pluginsTime = System.currentTimeMillis()
        pluginRegistry.preInitializePlugins()
        pluginsTime = System.currentTimeMillis() - pluginsTime
        LOGGER.info("Pre-initialized plugins in {} ms", pluginsTime)

        LOGGER.info("Configure plugins")
        configTime = System.currentTimeMillis()
        val configurator = Jector()
        configurator.collectProviders(this)
        collectPluginProviders(configurator, pluginRegistry)
        configurator.initializeBeans()
        autoConfigurePlugins(configurator, pluginRegistry)
        configurator.autoConfigure(this)
        configTime = System.currentTimeMillis() - configTime
        LOGGER.info("Configured plugins in {} ms", configTime)

        LOGGER.info("Initialize plugins")
        pluginsTime = System.currentTimeMillis()
        pluginRegistry.initializePlugins(objectMapper)
        pluginsTime = System.currentTimeMillis() - pluginsTime
        LOGGER.info("Initialized plugins in {} ms", pluginsTime)

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

        LOGGER.info("Loading profiles")
        var profileTime = System.currentTimeMillis()
        val profiles = ArrayList<ProfileLauncher>()
        loadProfiles(pluginClassLoader, configurator, profiles)
        profileTime = System.currentTimeMillis() - profileTime
        LOGGER.info("Loaded {} profiles in {} ms", profiles.size, profileTime)

        LOGGER.info("Launching profiles")
        profileTime = System.currentTimeMillis()
        for (profile in profiles) {
            launchProfile(profile)
        }
        profileTime = System.currentTimeMillis() - profileTime
        LOGGER.info("Launched profiles in {} ms", profileTime)

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
    open val profilesFolder: File get() = createFolderIfNotExists("profiles")

    protected open fun loadPlugins(registry: PluginRegistry): ClassLoader {
        val pluginsFolder = pluginsFolder
        val pluginJars = pluginsFolder.listFiles { _, name -> name.endsWith(".jar") }
        if (pluginJars == null || pluginJars.isEmpty()) {
            return javaClass.classLoader
        }
        val urls = ArrayList<URL>()
        val classNamesToLoad = ArrayList<String>()
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
            urls.add(pluginJar.toURI().toURL())
            classNamesToLoad.add(pluginClassName!!)
        }
        val childClassLoader = URLClassLoader(
                urls.toTypedArray(),
                javaClass.classLoader
        )
        for (className in classNamesToLoad) {
            val registration = loadPlugin(childClassLoader, className)
            registry.register(registration)
            LOGGER.info("Loaded plugin {} ({})", registration.name, registration.id)
        }
        return childClassLoader
    }

    protected open fun loadSystemPlugins(registry: PluginRegistry) {
        registry.register(PluginRegistration(CorePlugin.ID, CorePlugin.NAME, CorePlugin()))
    }

    private fun loadPlugin(classLoader: ClassLoader, pluginClassName: String): PluginRegistration {
        val clazz = Class.forName(pluginClassName, true, classLoader)
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

    private fun collectPluginProviders(jector: Jector, pluginRegistry: PluginRegistry) {
        for (plugin in pluginRegistry.plugins) {
            jector.collectProviders(plugin)
            jector.collectProviders(listOf(StaticBeanFactory(plugin, plugin.javaClass)), Priority.HIGHEST, false)
        }
    }

    private fun autoConfigurePlugins(autoConfigurator: AutoConfigurator, pluginRegistry: PluginRegistry) {
        for (plugin in pluginRegistry.plugins) {
            autoConfigurator.autoConfigure(plugin)
        }
    }

    protected open fun loadProfiles(classLoader: ClassLoader, configurator: AutoConfigurator, outProfiles: MutableList<ProfileLauncher>) {
        val profilesFolder = this.profilesFolder
        val subfolders = profilesFolder.listFiles { file -> file.isDirectory && file.canRead() }
        if (subfolders == null || subfolders.isEmpty()) {
            return
        }
        for (subfolder in subfolders) {
            val profileFile = File(subfolder, "profile.json")
            if (!profileFile.exists() || !profileFile.canRead()) {
                continue
            }
            val jsonNode = FileInputStream(profileFile).use { inputStream -> objectMapper.readTree(inputStream) }
            val classNameNode = jsonNode.get("className")
            val className = if (classNameNode == null || classNameNode.isNull) null else classNameNode.asText(null)
            var profile: ProfileLauncher
            if (className == null || className.isBlank()) {
                LOGGER.warn("ProfileLauncher at {} does not specify a className, skipping", profileFile)
                continue
            }
            try {
                val loadedClass = Class.forName(className, true, classLoader)
                if (!ProfileLauncher::class.java.isAssignableFrom(loadedClass)) {
                    LOGGER.warn("Profile defined in {} does not use an implementation of {}, skipping", profileFile, ProfileLauncher::class.java.name)
                    continue
                }
                var profileFactory: ProfileFactory<*>? = null
                for (candidate in profileFactories) {
                    if (candidate.profileClass == loadedClass) {
                        profileFactory = candidate
                        break
                    }
                }
                if (profileFactory == null) {
                    LOGGER.warn("No factory found for profile launcher class {}", loadedClass)
                    continue
                }
                profile = profileFactory.parse(jsonNode)
            } catch (e: Exception) {
                LOGGER.warn("Could not load profile from {}", profileFile, e)
                continue
            }
            configurator.autoConfigure(profile)
            outProfiles.add(profile)
        }
    }

    private fun launchProfile(profile: ProfileLauncher) {
        if (this.stopping) {
            throw IllegalStateException("Chatster is stopping, it cannot launch any more profiles")
        }
        try {
            val profileFolder = File(profilesFolder, profile.id)
            val botProfile = ProfileThread(profile, profileFolder, profileContext, messageHandlers)
            synchronized(runningProfiles) {
                runningProfiles.add(botProfile)
                botProfile.start()
            }
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    protected open fun onProfileTerminated(profile: ProfileLauncher, exception: Throwable?, numStillRunning: Int) {
        if (this.stopping || exception == null) {
            // Profile terminated in a normal way or we're currently stopping, so there is no need to restart it
            return
        }
        profileLauncherExecutor.submit {
            if (runningProfiles.isProfileRunning(profile.id)) {
                // Already running
                return@submit
            }
            LOGGER.info("Relaunching profile {}...", profile.id)
            try {
                launchProfile(profile)
            } catch (e: Exception) {
                LOGGER.warn("Could not relaunch profile {}", profile.id, e)
            }
        }
    }

    private val profileContext = object : ProfileContext {

        override fun onShutdown(thread: ProfileThread, profile: ProfileLauncher, exception: Throwable?) {
            if (exception == null) {
                LOGGER.warn("ProfileLauncher thread {} has shutdown in a normal way", profile.id)
            } else {
                LOGGER.warn("ProfileLauncher thread {} has crashed", profile.id, exception)
            }
            val profileThreadsRunning: Int
            synchronized(runningProfiles) {
                runningProfiles.remove(thread)
                profileThreadsRunning = runningProfiles.size()
            }
            onProfileTerminated(profile, exception, profileThreadsRunning)
        }

        override fun enqueueWorkerTask(runnable: Runnable) {
            workerExecutor.submit(runnable)
        }

        override fun handleMessage(message: Message, profile: ProfileLauncher) {
            var text = message.message
            if (text == null || !text.startsWith("!")) {
                return
            }
            text = text.substring(1)
            val commandDispatcher = commandDispatcherHolder.commandDispatcher
            commandDispatcher.execute(text, CommandContextImpl(message, profile))
        }

    }

    @Provider
    fun createCommandDispatcherHolder(): CommandDispatcherHolder {
        return CommandDispatcherHolder(CommandDispatcher())
    }

    @Provider
    fun createCommandMessageHandler(): MessageHandler {
        return CommandMessageHandler(profileContext)
    }

    @Provider(priority = Priority.LOWEST)
    fun createPlaintextCredentialStore(): PlaintextCredentialStore {
        return PlaintextCredentialStore()
    }

    @Provider
    fun createObjectMapper(): ObjectMapper {
        return ObjectMapper()
    }

}