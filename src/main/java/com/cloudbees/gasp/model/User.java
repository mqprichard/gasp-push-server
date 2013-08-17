/*
 * Copyright (c) 2013 Mark Prichard, CloudBees
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudbees.gasp.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Model class for Gasp! Users: the gasp-server database is the master
 * (schema as shown below), updates sent via WEAVE@cloud and REST/JSON
 *
 * +-------+--------------+------+-----+---------+----------------+
 * | Field | Type         | Null | Key | Default | Extra          |
 * +-------+--------------+------+-----+---------+----------------+
 * | id    | int(11)      | NO   | PRI | NULL    | auto_increment |
 * | name  | varchar(255) | YES  |     | NULL    |                |
 * +-------+--------------+------+-----+---------+----------------+
 *
 * @author Mark Prichard
 */
@XmlRootElement
public class User {
    private int id;
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
