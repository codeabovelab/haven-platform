/*
 * Copyright 2016 Code Above Lab LLC
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

package com.codeabovelab.dm.fs.entity;

import javax.persistence.*;
import java.util.UUID;

/**
 */
@Entity
@Table(name = "file_attribute")
public class FileAttribute {
    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false, name = "file_id")
    private UUID fileId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String data;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileAttribute)) {
            return false;
        }

        FileAttribute that = (FileAttribute) o;

        if (data != null ? !data.equals(that.data) : that.data != null) {
            return false;
        }
        if (fileId != null ? !fileId.equals(that.fileId) : that.fileId != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = fileId != null ? fileId.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (data != null ? data.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FileAttribute{" +
          "id=" + id +
          ", fileId='" + fileId + '\'' +
          ", name='" + name + '\'' +
          ", data='" + data + '\'' +
          '}';
    }
}
