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

package lu.kremi151.chatster.core.threading

import lu.kremi151.chatster.api.message.Message
import lu.kremi151.chatster.api.profile.ProfileLauncher
import lu.kremi151.chatster.core.context.ProfileContext
import java.io.File

class ProfileThread<MessageType: Message> (
        val profile: ProfileLauncher,
        private val profileFolder: File,
        private val context: ProfileContext<MessageType>
): Thread() {

    override fun run() {
        try {
            profile.setup(profileFolder)
            profile.listenForMessages(this::handleInboundMessage)
            context.onShutdown(this, profile, null)
        } catch (t: Throwable) {
            context.onShutdown(this, profile, t)
        }
    }

    private fun handleInboundMessage(message: Message) {
        context.enqueueWorkerTask(Runnable {
            profile.acknowledgeMessage(message)
            context.handleMessage(message, profile)
        })
    }

}