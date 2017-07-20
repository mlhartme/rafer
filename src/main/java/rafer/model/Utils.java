/*
 * Copyright Michael Hartmeier
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
package rafer.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    public static final SimpleDateFormat LINKED_FMT = new SimpleDateFormat("yyMMdd");
    public static final SimpleDateFormat DAY_FMT = new SimpleDateFormat("yyyy/MM/dd");
    public static final SimpleDateFormat MONTH_FMT = new SimpleDateFormat("yyyy/MM");

    public static final String STARTED = new SimpleDateFormat("yyMMdd-hhmmss").format(new Date());


    public static final String RAF = ".RAF";
    public static final String JPG = ".JPG";
}
