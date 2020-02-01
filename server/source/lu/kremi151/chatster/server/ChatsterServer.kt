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

package lu.kremi151.chatster.server

import lu.kremi151.chatster.api.profile.ProfileLauncher
import lu.kremi151.chatster.core.Chatster
import lu.kremi151.chatster.core.config.Configurator
import lu.kremi151.chatster.server.profile.CLIProfileLauncher

class ChatsterServer(private val useCli: Boolean): Chatster() {

    override fun loadProfiles(classLoader: ClassLoader, configurator: Configurator, outProfiles: MutableList<ProfileLauncher>) {
        super.loadProfiles(classLoader, configurator, outProfiles)
        if (useCli) {
            outProfiles.add(CLIProfileLauncher())
        }
    }

}