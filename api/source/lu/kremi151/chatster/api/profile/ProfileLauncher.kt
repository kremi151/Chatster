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

package lu.kremi151.chatster.api.profile

import lu.kremi151.chatster.api.message.Message
import lu.kremi151.chatster.api.util.Handler
import java.io.File

interface ProfileLauncher {

    var id: String

    fun setup(folder: File)
    fun listenForMessages(handleMessage: Handler<Message>)

    fun hasPermission(permission: String): Boolean

    fun sendTextMessage(inboundMessage: Message, response: String)
    fun sendTextMessage(inboundMessage: Message, file: File)
    fun sendTextMessage(inboundMessage: Message, response: String, file: File)

    fun sendWritingStatus(inboundMessage: Message, startedWriting: Boolean)

    fun acknowledgeMessage(inboundMessage: Message)

}