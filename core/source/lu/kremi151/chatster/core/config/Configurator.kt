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
import lu.kremi151.chatster.api.enums.Priority
import lu.kremi151.chatster.api.service.AutoConfigurator
import lu.kremi151.chatster.core.registry.PluginRegistry
import java.lang.IllegalStateException
import java.lang.reflect.Field
import java.util.*
import kotlin.collections.ArrayList

class Configurator(
        private val pluginRegistry: PluginRegistry
): AutoConfigurator {

    private val factories: MutableMap<Class<*>, MutableList<FactoryEntry>> = HashMap()
    private val beans: MutableMap<Class<*>, Any> = HashMap()

    init {
        beans[AutoConfigurator::class.java] = arrayListOf(BeanEntry(this, Priority.HIGHEST))
    }

    fun collectPluginProviders() {
        // Scan plugins for providers and register plugins themselves as beans
        for (plugin in pluginRegistry.plugins) {
            collectProviders(plugin)
            beans[plugin.javaClass] = arrayListOf(BeanEntry(this, Priority.HIGHEST))
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
            if (AutoConfigurator::class.java.isAssignableFrom(primaryType)) {
                throw IllegalStateException("Providers of type ${AutoConfigurator::class.java} cannot be manually defined")
            }

            val factoryEntry = FactoryEntry(obj, method, providerMeta.priority)
            var type: Class<*>? = primaryType
            while (type != null) {
                addFactory(type, factoryEntry)
                type = type.superclass
            }
        }
    }

    fun initializeBeans() {
        val factoryForTypes = HashMap<FactoryEntry, MutableList<Class<*>>>()
        for (entry in factories) {
            val requestingType = entry.key
            val factories = entry.value
            if (factories.isEmpty()) {
                continue
            }
            var requestingTypes = factoryForTypes[factories.last()]
            if (requestingTypes == null) {
                requestingTypes = ArrayList()
                factoryForTypes[factories.last()] = requestingTypes
            }
            requestingTypes.add(requestingType)
        }
        for (entry in factoryForTypes) {
            val factoryEntry = entry.key
            val requestingTypes = entry.value
            val bean = factoryEntry.method.invoke(factoryEntry.holder)
            for (requestingType in requestingTypes) {
                beans[requestingType] = bean
            }
        }
    }

    fun autoConfigurePlugins() {
        for (plugin in pluginRegistry.plugins) {
            autoConfigure(plugin)
        }
    }

    override fun autoConfigure(obj: Any) {
        val fields = obj.javaClass.declaredFields
        for (field in fields) {
            val annotation = field.getAnnotation(Inject::class.java) ?: continue
            autoConfigureField(obj, field, annotation)
        }

        // TODO: Inject configurations
    }

    private fun <T> getConfigurableValue(type: Class<T>): T? {
        // Check if we have a direct match
        var match = beans[type]
        if (match != null) {
            @Suppress("UNCHECKED_CAST")
            return match as T?
        }

        // Scan through the existing beans and find the one with the highest priority
        var maxPriority: Priority? = null
        for (bean in beans) {
            if (!type.isAssignableFrom(bean.key)) {
                continue
            }
            val factoryEntry = factories[bean.key] ?: continue
            if (factoryEntry.isEmpty()) {
                continue
            }
            val highestPriorityEntry = factoryEntry.last()
            if (maxPriority == null || maxPriority.ordinal < highestPriorityEntry.priority.ordinal) {
                maxPriority = highestPriorityEntry.priority
                match = bean.value
            }
        }
        if (match != null) {
            // Cache result for quicker lookup
            beans[type] = match

            @Suppress("UNCHECKED_CAST")
            return match as T?
        }
        return null
    }

    private fun autoConfigureField(obj: Any, field: Field, annotation: Inject) {
        field.isAccessible = true

        if (List::class.java.isAssignableFrom(field.type)) {
            if (annotation.collectionType == Any::class) {
                throw IllegalStateException("@Inject annotation for a List in field $field does not specify a collection type")
            }
            val outSet = HashSet<Any>()
            for (bean in beans) {
                if (annotation.collectionType.java.isAssignableFrom(bean.key)) {
                    outSet.add(bean.value)
                }
            }
            field.set(obj, Collections.unmodifiableList(ArrayList(outSet)))
            return
        }

        val match = getConfigurableValue(field.type)
        if (match != null) {
            field.set(obj, match)
        }

        throw IllegalStateException("Could not inject value at $field")
    }

    private data class BeanEntry(val bean: Any, val priority: Priority)

}