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

package lu.kremi151.chatster.core.plugin.command

import com.mojang.brigadier.tree.CommandNode
import lu.kremi151.chatster.api.command.*
import lu.kremi151.chatster.api.context.CommandContext
import lu.kremi151.chatster.core.command.builder.ExecutedCommandImpl
import lu.kremi151.chatster.core.services.CommandDispatcherHolder
import lu.kremi151.jector.annotations.Inject

class CommandProviderHelp: CommandProvider {

    companion object {
        private const val ARG_COMMAND = "command"
    }

    @Inject
    private lateinit var commandDispatcherHolder: CommandDispatcherHolder

    override fun registerCommands(builder: RootCommandBuilder, registry: CommandRegistry) {
        registry.registerCommand(builder.literal("help")
                .executes(object : CommandExecutor {
                    override fun execute(command: ExecutedCommand): Boolean {
                        @Suppress("DEPRECATION") // It is totally fine to use the deprecated command inside of this class
                        val rootNode = (command as ExecutedCommandImpl).command.rootNode
                        showUsage(command.context, rootNode)
                        return true
                    }
                })
                .top())
        registry.registerCommand(builder.literal("man")
                .argWord(ARG_COMMAND)
                .executes(object : CommandExecutor {
                    override fun execute(command: ExecutedCommand): Boolean {
                        @Suppress("DEPRECATION") // It is totally fine to use the deprecated command inside of this class
                        var node = (command as ExecutedCommandImpl).command.rootNode
                        node = node!!.getChild(command.getStringArgument(ARG_COMMAND))
                        if (node == null) {
                            return false
                        }
                        showUsage(command.context, node)
                        return true
                    }
                })
                .top())
    }

    private fun showUsage(context: CommandContext, node: CommandNode<CommandContext>) {
        val helpLines = commandDispatcherHolder.commandDispatcher.getAllUsage(node, context, true)
        val sb = StringBuilder("Available commands:\n")
        for (cmd in helpLines) {
            sb.append("\n!").append(cmd)
        }
        context.sendTextMessage(sb.toString())
    }

}