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

package lu.kremi151.chatster.core.registry

import lu.kremi151.chatster.api.plugin.ChatsterPlugin
import java.lang.IllegalStateException

class PluginRegistry {

    private var pluginList: List<ChatsterPlugin> = emptyList()
    private var idToPlugins: Map<String, PluginRegistration> = emptyMap()

    @Synchronized
    fun register(plugin: PluginRegistration) {
        val otherPlugin = idToPlugins[plugin.id]
        if (otherPlugin != null) {
            throw IllegalStateException("A plugin with id ${plugin.id} is already registered (Conflict between plugins \"${plugin.name}\" and \"${otherPlugin.name}\")")
        }

        pluginList = plugins.plus(plugin.plugin)
        idToPlugins = idToPlugins.plus(Pair(plugin.id, plugin))
    }

    val size get() = idToPlugins.size
    val plugins get() = pluginList

}