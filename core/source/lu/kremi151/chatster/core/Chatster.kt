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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

import lu.kremi151.chatster.api.plugin.ChatsterPlugin
import lu.kremi151.chatster.api.annotations.Plugin
import lu.kremi151.chatster.core.registry.PluginRegistry
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URLClassLoader
import java.util.jar.JarInputStream

open class Chatster {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

        private val MANIFEST_PLUGIN_SCAN_PACKAGE = "Chatster-Scan-Package"
    }

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
        loadPlugins(pluginRegistry)
        pluginsTime = System.currentTimeMillis() - pluginsTime
        LOGGER.info("Loaded {} plugins in {} ms", pluginRegistry.size, pluginsTime)

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

    open fun loadPlugins(registry: PluginRegistry) {
        val pluginsFolder = pluginsFolder
        val pluginJars = pluginsFolder.listFiles { _, name -> name.endsWith(".jar") }
        if (pluginJars == null || pluginJars.isEmpty()) {
            return
        }
        for (pluginJar in pluginJars) {
            var scanPackage: String? = null
            JarInputStream(FileInputStream(pluginJar)).use { jarStream ->
                val manifest = jarStream.manifest
                val attributes = manifest.mainAttributes
                scanPackage = attributes.getValue(MANIFEST_PLUGIN_SCAN_PACKAGE)
            }
            if (scanPackage == null || scanPackage!!.isBlank()) {
                continue
            }
            loadPlugin(pluginJar, scanPackage!!, registry)
        }
    }

    private fun loadPlugin(pluginJar: File, scanPackage: String, registry: PluginRegistry) {
        val childClassLoader = URLClassLoader(
                arrayOf(pluginJar.toURI().toURL()),
                javaClass.classLoader
        )
        scanPackageForPluginDefinitions(scanPackage, childClassLoader, registry)
    }

    private fun scanPackageForPluginDefinitions(packageName: String, classLoader: ClassLoader, registry: PluginRegistry) {
        val urls = ClasspathHelper.forPackage(packageName, classLoader)
        val reflections = Reflections(ConfigurationBuilder.build().setUrls(urls)
                .addClassLoaders(classLoader).addScanners(SubTypesScanner(), TypeAnnotationsScanner()))
        val classes = reflections.getTypesAnnotatedWith(Plugin::class.java)
        for (clazz in classes) {
            if (!ChatsterPlugin::class.java.isAssignableFrom(clazz)) {
                LOGGER.warn("Plugin class {} does not extend {}, skipping", clazz.name, ChatsterPlugin::class.java.name)
                continue
            }
            val meta = clazz.getAnnotation(Plugin::class.java)
            if (meta == null) {
                LOGGER.warn("Plugin class {} is not annotated with {}, skipping", clazz.name, Plugin::class.java.name)
                continue
            }
            try {
                val plugin = clazz.getConstructor().newInstance() as ChatsterPlugin
                registry.register(plugin)
                LOGGER.info("Loaded plugin {} ({})", meta.name, meta.id)
            } catch (e: Exception) {
                throw IOException("Could not load plugin", e)
            }
        }
    }

    open fun loadGlobalConfig() {
        // TODO: Implement
    }

}