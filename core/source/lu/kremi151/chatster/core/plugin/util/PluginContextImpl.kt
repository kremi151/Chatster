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

package lu.kremi151.chatster.core.plugin.util

import com.fasterxml.jackson.databind.ObjectMapper
import lu.kremi151.chatster.api.plugin.PluginContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter

class PluginContextImpl(
        private val pluginId: String,
        private val configFolder: File,
        private val objectMapper: ObjectMapper
): PluginContext {

    private val configFile: File
        get() = File(configFolder, "$pluginId.json")

    override fun <T> loadConfig(clazz: Class<T>): T? {
        val configFile = this.configFile
        if (!configFile.exists()) {
            return null
        }
        return FileReader(configFile).use { objectMapper.readValue(it, clazz) }
    }

    override fun saveConfig(config: Any) {
        val configFile = this.configFile
        FileWriter(configFile).use { objectMapper.writeValue(it, config) }
    }

}
