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

package lu.kremi151.chatster.server.profile

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import lu.kremi151.chatster.api.message.Message
import lu.kremi151.chatster.api.profile.ProfileLauncher
import lu.kremi151.chatster.api.util.Handler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.invoke.MethodHandles
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class CLIProfileLauncher: ProfileLauncher {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }

    override var id: String = ""

    private fun println(text: String) {
        System.out.println("CLI > $text")
    }

    override fun setup(folder: File) {
        LOGGER.info("Using $javaClass for profile $id. This should only be used for testing purposes.")
    }

    override fun listenForMessages(handleMessage: Handler<Message>) {
        Scanner(System.`in`).use { scanner ->
            while (true) {
                val line = scanner.nextLine()
                handleMessage(Message(line))
            }
        }
    }

    override fun hasPermission(permission: String): Boolean {
        return true
    }

    override fun sendTextMessage(inboundMessage: Message, response: String) {
        println(response)
    }

    override fun sendTextMessage(inboundMessage: Message, file: File) {
        println("[File] ${file.absolutePath} (${file.length()} bytes)")
    }

    override fun sendTextMessage(inboundMessage: Message, response: String, file: File) {
        sendTextMessage(inboundMessage, response)
        sendTextMessage(inboundMessage, file)
    }

    override fun sendWritingStatus(inboundMessage: Message, startedWriting: Boolean) {
        if (startedWriting) {
            println("Bot is writing")
        } else {
            println("Bot stopped writing")
        }
    }

    override fun acknowledgeMessage(inboundMessage: Message) {
        println("Bot has read the message")
    }

}
