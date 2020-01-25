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

package lu.kremi151.chatster.core.config

import lu.kremi151.chatster.api.annotations.Inject
import lu.kremi151.chatster.api.annotations.Provider
import lu.kremi151.chatster.core.registry.PluginRegistry
import java.lang.IllegalStateException
import java.lang.reflect.Field

class Configurator(
        private val pluginRegistry: PluginRegistry
) {

    private val factories: MutableMap<Class<*>, MutableList<FactoryEntry>> = HashMap()
    private val primaryFactories: MutableMap<Class<*>, FactoryEntry> = HashMap()
    private val beans: MutableMap<Class<*>, Any> = HashMap()

    fun collectPluginProviders() {
        // Scan plugins for providers and register plugins themselves as beans
        for (plugin in pluginRegistry.plugins) {
            collectProviders(plugin)
            beans[plugin.javaClass] = plugin
        }
    }

    private fun addFactory(type: Class<*>, entry: FactoryEntry) {
        var factoriesList = factories[type]
        if (factoriesList == null) {
            factoriesList = ArrayList()
            factories[type] = factoriesList
        }
        factoriesList.add(entry)
        factoriesList.sortBy { it.priority.ordinal }
    }

    fun collectProviders(obj: Any) {
        val methods = obj.javaClass.declaredMethods
        for (method in methods) {
            val providerMeta = method.getAnnotation(Provider::class.java) ?: continue
            if (method.parameterCount != 0) {
                throw IllegalStateException("Provider factory $method has ${method.parameterCount} parameters, but there should be none")
            }
            val primaryType = method.returnType
            val previousDefinition = primaryFactories[primaryType]
            if (previousDefinition != null) {
                throw IllegalStateException("A provider for type $primaryType has already been defined, conflict between existing $previousDefinition and $method")
            }

            val factoryEntry = FactoryEntry(obj, method, providerMeta.priority)

            primaryFactories[primaryType] = factoryEntry
            addFactory(primaryType, factoryEntry)

            var type: Class<*>? = primaryType
            while (type != null) {
                if (!primaryFactories.containsKey(type)) {
                    addFactory(type, factoryEntry)
                }
                type = type.superclass
            }
        }
    }

    fun autoConfigurePlugins() {
        for (plugin in pluginRegistry.plugins) {
            autoConfigure(plugin)
        }
    }

    fun autoConfigure(obj: Any) {
        val fields = obj.javaClass.declaredFields
        for (field in fields) {
            if (!field.isAnnotationPresent(Inject::class.java)) {
                continue
            }
            autoConfigureField(obj, field)
        }

        // TODO: Inject configurations
    }

    private fun autoConfigureField(obj: Any, field: Field) {
        field.isAccessible = true

        // Check if we already created a bean
        var fieldType: Class<*>? = field.type
        while (fieldType != null) {
            val bean = beans[fieldType]
            if (bean != null) {
                field.set(obj, bean)
                return
            }
            fieldType = fieldType.superclass
        }

        // Create a bean if not already created
        fieldType = field.type
        while (fieldType != null) {
            val factoryEntries = factories[fieldType]
            if (factoryEntries != null && !factoryEntries.isEmpty()) {
                val factoryEntry = factoryEntries.last()
                val bean = factoryEntry.method.invoke(factoryEntry.holder)
                beans[fieldType] = bean
                field.set(obj, bean)
                return
            }
            fieldType = fieldType.superclass
        }

        throw IllegalStateException("Could not inject value at $field")
    }

}