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

package lu.kremi151.chatster.core.command.builder

import lu.kremi151.chatster.api.command.ExecutedCommand
import lu.kremi151.chatster.api.context.CommandContext

class ExecutedCommandImpl(

    /**
     * For usage inside of the Chatster core (e.g. to show command usages)
     * @return
     */
    @get:Deprecated("Should not be accessed externally")
    val command: com.mojang.brigadier.context.CommandContext<CommandContext>

) : ExecutedCommand {

    @Suppress("DEPRECATION") // It is totally fine to use the deprecated command inside of this class
    override val context: CommandContext get() = command.source

    @Suppress("DEPRECATION") // It is totally fine to use the deprecated command inside of this class
    override fun <T> getArgument(name: String, clazz: Class<T>): T {
        return command.getArgument(name, clazz)
    }

}
