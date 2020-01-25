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

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import lu.kremi151.chatster.api.command.LiteralCommandBuilder
import lu.kremi151.chatster.api.context.CommandContext

class LiteralCommandBuilderImpl(
        private val rootNode: LiteralArgumentBuilder<CommandContext>
) : ArgumentableCommandBuilderImpl(null), LiteralCommandBuilder {

    val appliedRootNode: LiteralArgumentBuilder<CommandContext>
        get() = apply(rootNode) as LiteralArgumentBuilder<CommandContext>

    override fun top(): LiteralCommandBuilderImpl {
        return this
    }

}
