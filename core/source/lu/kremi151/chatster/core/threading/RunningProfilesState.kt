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

import java.util.HashSet
import java.util.Optional

class RunningProfilesState {

    private val runningProfiles = HashSet<ProfileThread<*>>()
    private val runningProfileIds = HashSet<String>()

    val copyOfRunningProfiles: Set<ProfileThread<*>>
        @Synchronized get() = HashSet(runningProfiles)

    @Synchronized
    fun clear() {
        runningProfiles.clear()
        runningProfileIds.clear()
    }

    @Synchronized
    fun size(): Int {
        return runningProfiles.size
    }

    @Synchronized
    fun findProfileThread(profileId: String): Optional<ProfileThread<*>> {
        return runningProfiles.stream().filter { thread -> thread.profile.id == profileId }.findFirst()
    }

    @Synchronized
    fun add(profile: ProfileThread<*>): Boolean {
        if (runningProfileIds.contains(profile.profile.id)) {
            return false
        }
        runningProfiles.add(profile)
        runningProfileIds.add(profile.profile.id)
        return true
    }

    @Synchronized
    fun remove(profile: ProfileThread<*>) {
        runningProfiles.remove(profile)
        runningProfileIds.remove(profile.profile.id)
    }

    @Synchronized
    fun isProfileRunning(id: String): Boolean {
        return runningProfileIds.contains(id)
    }

}
