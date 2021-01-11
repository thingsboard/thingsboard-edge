/**
 * Copyright © 2016-2021 ThingsBoard, Inc.
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
package org.thingsboard.integration.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.File;
import java.util.Objects;

@ToString
@AllArgsConstructor
class EventStorageReaderPointer {

    @Getter @Setter
    private File file;
    @Getter @Setter
    private int line;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventStorageReaderPointer that = (EventStorageReaderPointer) o;
        return line == that.line &&
                Objects.equals(file.getName(), that.file.getName());
    }

    public EventStorageReaderPointer copy(){
        return new EventStorageReaderPointer(file, line);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file.getName(), line);
    }
}
