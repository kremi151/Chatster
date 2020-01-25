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
import lu.kremi151.chatster.api.profile.Profile
import lu.kremi151.chatster.api.profile.ProfileConfig

class ProfileThread<MessageType: Message, ProfileConfigType: ProfileConfig> (
        private val profile: Profile<MessageType, ProfileConfigType>,
        private val config: ProfileConfigType,
        private val context: ProfileContext<MessageType, ProfileConfig>
): Thread() {

    override fun run() {
        try {
            profile.setup(config)
            profile.listenForMessages(this::handleInboundMessage)
            context.onShutdown(this, profile, null)
        } catch (t: Throwable) {
            context.onShutdown(this, profile, t)
        }
    }

    private fun handleInboundMessage(message: Message) {
        context.enqueueWorkerTask(Runnable {
            context.handleMessage(message, profile)
        })
    }

}