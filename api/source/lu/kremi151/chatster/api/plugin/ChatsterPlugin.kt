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

package lu.kremi151.chatster.api.plugin

import lu.kremi151.chatster.api.command.CommandProvider
import lu.kremi151.chatster.api.util.Handler

abstract class ChatsterPlugin {

    @Deprecated("Use onPreInitialize(PreInitPluginContext)")
    open fun onPreInitialize() {}

    open fun onPreInitialize(context: PreInitPluginContext) {
        @Suppress("DEPRECATION")
        onPreInitialize()
    }

    @Deprecated("Use onLoad(InitPluginContext)")
    open fun onLoad() {}

    open fun onLoad(context: InitPluginContext) {
        @Suppress("DEPRECATION")
        onLoad()
    }

    open fun onRegisterCommands(register: Handler<CommandProvider>) {}

}
