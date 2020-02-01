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

package lu.kremi151.chatster.core.context

import lu.kremi151.chatster.api.context.CommandContext
import lu.kremi151.chatster.api.message.Message
import lu.kremi151.chatster.api.profile.ProfileLauncher
import java.io.File

class CommandContextImpl(
        private val inboundMessage: Message,
        private val profile: ProfileLauncher
): CommandContext {

    override fun sendTextMessage(message: String) {
        profile.sendTextMessage(inboundMessage, message)
    }

    override fun sendTextMessage(file: File) {
        profile.sendTextMessage(inboundMessage, file)
    }

    override fun sendTextMessage(message: String, file: File) {
        profile.sendTextMessage(inboundMessage, message, file)
    }

    override fun sendWriting(started: Boolean) {
        profile.sendWritingStatus(inboundMessage, started)
    }

    override fun hasPermission(permission: String): Boolean {
        return profile.hasPermission(permission)
    }

}
