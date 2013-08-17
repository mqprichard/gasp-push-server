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
 * Model class for Gasp! Reviews: the gasp-server database is the master
 * (schema as shown below), updates sent via WEAVE@cloud and REST/JSON
 *
 * +---------------+--------------+------+-----+---------+----------------+
 * | Field         | Type         | Null | Key | Default | Extra          |
 * +---------------+--------------+------+-----+---------+----------------+
 * | id            | int(11)      | NO   | PRI | NULL    | auto_increment |
 * | comment       | varchar(255) | YES  |     | NULL    |                |
 * | star          | int(11)      | YES  |     | NULL    |                |
 * | restaurant_id | int(11)      | NO   | MUL | NULL    |                |
 * | user_id       | int(11)      | NO   | MUL | NULL    |                |
 * +---------------+--------------+------+-----+---------+----------------+

 *
 * @author Mark Prichard
 */
@XmlRootElement
public class Review {
    private int id;
    private String comment;
    private String star;
    private int restaurant_id;
    private int user_id;

    public int getId() {
        return id;
    }

    public String getComment() {
        return comment;
    }

    public String getStar() {
        return star;
    }

    public int getRestaurant_id() {
        return restaurant_id;
    }

    public int getUser_id() {
        return user_id;
    }
}
