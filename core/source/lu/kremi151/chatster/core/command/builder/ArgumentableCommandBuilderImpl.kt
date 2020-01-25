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

import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import lu.kremi151.chatster.api.command.ArgumentableCommandBuilder
import lu.kremi151.chatster.api.command.CommandExecutor
import lu.kremi151.chatster.api.command.ExecutableCommandBuilder
import lu.kremi151.chatster.api.command.LiteralCommandBuilder
import lu.kremi151.chatster.api.context.CommandContext

import java.util.HashMap
import java.util.function.Predicate

import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.*

open class ArgumentableCommandBuilderImpl(
        private val root: LiteralCommandBuilder?
) : ArgumentableCommandBuilder, BrigardierBacked {

    private val args = HashMap<RequiredArgumentBuilder<CommandContext, *>, BrigardierBacked>()
    private var predicate: Predicate<CommandContext>? = Predicate{ true }
    private var executor: CommandExecutor? = null

    override fun argInteger(name: String, min: Int, max: Int): ArgumentableCommandBuilder {
        val arg = RequiredArgumentBuilder.argument<CommandContext, Int>(name, integer(min, max))
        val newBuilder = ArgumentableCommandBuilderImpl(top())
        args[arg] = newBuilder
        return newBuilder
    }

    override fun argInteger(name: String, min: Int): ArgumentableCommandBuilder {
        val arg = RequiredArgumentBuilder.argument<CommandContext, Int>(name, integer(min))
        val newBuilder = ArgumentableCommandBuilderImpl(top())
        args[arg] = newBuilder
        return newBuilder
    }

    override fun argInteger(name: String): ArgumentableCommandBuilder {
        val arg = RequiredArgumentBuilder.argument<CommandContext, Int>(name, integer())
        val newBuilder = ArgumentableCommandBuilderImpl(top())
        args[arg] = newBuilder
        return newBuilder
    }

    override fun argGreedyString(name: String): ArgumentableCommandBuilder {
        val arg = RequiredArgumentBuilder.argument<CommandContext, String>(name, greedyString())
        val newBuilder = ArgumentableCommandBuilderImpl(top())
        args[arg] = newBuilder
        return newBuilder
    }

    override fun argString(name: String): ArgumentableCommandBuilder {
        val arg = RequiredArgumentBuilder.argument<CommandContext, String>(name, string())
        val newBuilder = ArgumentableCommandBuilderImpl(top())
        args[arg] = newBuilder
        return newBuilder
    }

    override fun argWord(name: String): ArgumentableCommandBuilder {
        val arg = RequiredArgumentBuilder.argument<CommandContext, String>(name, word())
        val newBuilder = ArgumentableCommandBuilderImpl(top())
        args[arg] = newBuilder
        return newBuilder
    }

    override fun executes(executor: CommandExecutor): ExecutableCommandBuilder {
        this.executor = executor
        return this
    }

    override fun requires(predicate: Predicate<CommandContext>): ExecutableCommandBuilder {
        this.predicate = predicate
        return this
    }

    override fun top(): LiteralCommandBuilder {
        return requireNotNull(root)
    }

    @Suppress("UNCHECKED_CAST") // TODO: Refactor this method a bit to remove unchecked type casts
    override fun apply(node: ArgumentBuilder<CommandContext, *>): ArgumentBuilder<CommandContext, *> {
        var outNode = node
        for ((key, value) in args) {
            outNode = outNode.then(value.apply(key)) as ArgumentBuilder<CommandContext, *>
        }
        if (predicate != null) {
            outNode = outNode.requires(predicate) as ArgumentBuilder<CommandContext, *>
        }
        if (executor != null) {
            outNode = outNode.executes { c ->
                try {
                    if (executor!!.execute(ExecutedCommandImpl(c))) {
                        0
                    } else {
                        1
                    }
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            } as ArgumentBuilder<CommandContext, *>
        }
        return outNode
    }
}
