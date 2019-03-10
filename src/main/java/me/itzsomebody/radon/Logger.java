/*
 * Copyright (C) 2018 ItzSomebody
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package me.itzsomebody.radon;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utils to print fancy stuff in the console and to write log file.
 *
 * @author ItzSomebody
 */
public class Logger {
    /**
     * The {@link SimpleDateFormat} that will be used for logging.
     */
    private final static SimpleDateFormat FORMAT = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");

    /**
     * Prints a formatted message into the console and returns the result as
     * a {@link String}.
     *
     * @param string to write to the console.
     */
    public static void stdOut(String string) {
        String date = FORMAT.format(new Date(System.currentTimeMillis()));
        String formatted = "[" + date + "] INFO: " + string;
        System.out.println(formatted);
    }

    /**
     * Prints a formatted message into the console and returns the result as
     * a {@link String}.
     *
     * @param string to write to the console.
     */
    public static void stdErr(String string) {
        String date = FORMAT.format(new Date(System.currentTimeMillis()));
        String formatted = "[" + date + "] ERROR: " + string;
        System.out.println(formatted);
    }

    /**
     * Prints a formatted message into the console and returns the result as
     * a {@link String}.
     *
     * @param string to write to the console.
     */
    public static void stdWarn(String string) {
        String date = FORMAT.format(new Date(System.currentTimeMillis()));
        String formatted = "[" + date + "] WARNING: " + string;
        System.out.println(formatted);
    }
}
