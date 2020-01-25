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

import lu.kremi151.chatster.api.ChatsterPlugin

open class Chatster {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
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
        val plugins = ArrayList<ChatsterPlugin>()
        loadPlugins(plugins)
        pluginsTime = System.currentTimeMillis() - pluginsTime
        LOGGER.info("Loaded {} plugins in {} ms", plugins.size, pluginsTime)

        initTime = System.currentTimeMillis() - initTime
        LOGGER.info("Initialized Chatster in {} ms", initTime)
    }

    open fun loadPlugins(outPlugins: MutableList<ChatsterPlugin>) {
        // TODO: Implement
    }

    open fun loadGlobalConfig() {

    }

}