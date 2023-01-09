/*
 * (c) Copyright 2022 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.gradle.autoparallelizable

import nebula.test.IntegrationSpec


class AutoParallelizableIntegSpec extends IntegrationSpec {
    def test() {
        file('file')
        directory('dir')

        // language=gradle
        buildFile << '''
            import integtest.DoIt.DoItTask
            
            task doIt(type: DoItTask) {
                stringValue = 'heh'
                fileValue = file('file')
                dirValue = file('dir')
                intsValue = [1, 2 ,3] 
                filesValue += file('lol1')
                filesValue += file('lol2')
            }
        '''.stripIndent(true)

        when:
        def stdout = runTasksSuccessfully('doIt').standardOutput

        then:
        stdout.contains 'string: heh'
        stdout.contains 'file: file'
        stdout.contains 'dir: dir'
        stdout.contains 'ints: [1, 2, 3]'
        stdout.contains 'files: lol1, lol2'
    }
}
