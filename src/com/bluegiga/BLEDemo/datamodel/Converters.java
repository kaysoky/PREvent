/*
 * Bluegiga’s Bluetooth Smart Android SW for Bluegiga BLE modules
 * Contact: support@bluegiga.com.
 *
 * This is free software distributed under the terms of the MIT license reproduced below.
 *
 * Copyright (c) 2013, Bluegiga Technologies
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files ("Software")
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF 
 * ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A  PARTICULAR PURPOSE.
 */
package com.bluegiga.BLEDemo.datamodel;

// Converters - converts value between different numeral system
public class Converters {

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    // Gets value in hexadecimal system
    public static String getHexValue(byte value[]) {
        if (value == null) {
            return "";
        }

        char[] hexChars = new char[value.length * 3];
        int v;
        for (int j = 0; j < value.length; j++) {
            v = value[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }

    // Gets value in hexadecimal system for single byte
    public static String getHexValue(byte b) {
        char[] hexChars = new char[2];
        int v;
        v = b & 0xFF;
        hexChars[0] = hexArray[v >>> 4];
        hexChars[1] = hexArray[v & 0x0F];
        return new String(hexChars);
    }

    // Gets value in ascii system
    public static String getAsciiValue(byte value[]) {
        if (value == null) {
            return "";
        }

        return new String(value);
    }

    // Gets value in decimal system
    public static String getDecimalValue(byte value[]) {
        if (value == null) {
            return "";
        }

        String result = "";
        for (byte b : value) {
            result += ((int) b & 0xff) + " ";
        }
        return result;
    }

    // Gets value in decimal system for single byte
    public static String getDecimalValue(byte b) {
        String result = "";
        result += ((int) b & 0xff);

        return result;
    }
}
