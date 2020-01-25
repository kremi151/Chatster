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

    private val factories: MutableMap<Class<*>, FactoryEntry> = HashMap()
    private val primaryFactories: MutableMap<Class<*>, FactoryEntry> = HashMap()
    private val beans: MutableMap<Class<*>, Any> = HashMap()

    fun collectPluginProviders() {
        for (plugin in pluginRegistry.plugins) {
            collectProviders(plugin)
        }
    }

    fun collectProviders(obj: Any) {
        val methods = obj.javaClass.declaredMethods
        for (method in methods) {
            if (!method.isAnnotationPresent(Provider::class.java)) {
                continue
            }
            if (method.parameterCount != 0) {
                throw IllegalStateException("Provider factory $method has ${method.parameterCount} parameters, but there should be none")
            }
            val primaryType = method.returnType
            val previousDefinition = primaryFactories[primaryType]
            if (previousDefinition != null) {
                throw IllegalStateException("A provider for type $primaryType has already been defined, conflict between existing $previousDefinition and $method")
            }

            val factoryEntry = FactoryEntry(obj, method)

            primaryFactories[primaryType] = factoryEntry
            factories[primaryType] = factoryEntry

            var type: Class<*>? = primaryType
            while (type != null) {
                if (!primaryFactories.containsKey(type) && !factories.containsKey(type)) {
                    factories[type] = factoryEntry
                }
                type = type.superclass
            }

            // TODO: Use per-type lists of factory methods
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
            val factoryEntry = factories[fieldType]
            if (factoryEntry != null) {
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