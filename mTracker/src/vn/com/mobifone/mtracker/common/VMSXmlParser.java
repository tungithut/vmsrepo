package vn.com.mobifone.mtracker.common;
/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */


import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class parses XML feeds from stackoverflow.com.
 * Given an InputStream representation of a feed, it returns a List of entries,
 * where each list element represents a single entry (post) in the XML feed.
 */
public class VMSXmlParser {
    private static final String ns = null;

    // We don't use namespaces

    public Entry parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }

    private Entry readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        Entry entry = null;
        String version = null;
        String message = null;
        String event = null;
        String preferCellTowner = null;
        String keepFixGPS = null;
        String accuracyBeforeLogging = null;
        String timeIntervalForAccuracy = null;

        parser.require(XmlPullParser.START_TAG, ns, "maintag");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("version")) {
                version = readVersion(parser);
            } else if (name.equals("message")) {
                message = readMessage(parser);
            } else if (name.equals("event")) {
                event = readEvent(parser);
            } else if (name.equals("preferCellTowner")) {
            	preferCellTowner = readTag(parser,"preferCellTowner"); 
            } else if (name.equals("keepFixGPS")) {
            	keepFixGPS = readTag(parser,"keepFixGPS");
            } else if (name.equals("accuracyBeforeLogging")) {
            	accuracyBeforeLogging = readTag(parser,"accuracyBeforeLogging");
            } else if (name.equals("timeIntervalForAccuracy")) {
            	timeIntervalForAccuracy = readTag(parser,"timeIntervalForAccuracy");
            }
            
        }
        return new Entry(version,message,event, preferCellTowner,
        		keepFixGPS, accuracyBeforeLogging, timeIntervalForAccuracy );
    }

    // This class represents a single entry (post) in the XML feed.
    // It includes the data members "title," "link," and "summary."
    public static class Entry {
        public final String version;
        public final String message;
        public final String event;
        
        public final String preferCellTowner;
        public final String keepFixGPS;
        public final String accuracyBeforeLogging;
        public final String timeIntervalForAccuracy;

        private Entry(String version, String message, String event, String preferCell, 
        		String keepFix, String accuracyBeforeLogging, String timeInterval) {
            this.version = version;
            this.message = message;
            this.event = event;
            this.preferCellTowner = preferCell;
            this.keepFixGPS = keepFix;
            this.accuracyBeforeLogging = accuracyBeforeLogging;
            this.timeIntervalForAccuracy = timeInterval;
        }
    }
    

    // Processes title tags in the feed.
    private String readVersion(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "version");
        String version = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "version");
        return version;
    }

    private String readEvent(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "event");
        String event = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "event");
        return event;
    }

    // Processes summary tags in the feed.
    private String readMessage(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "message");
        String message = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "message");
        return message;
    }
    
    // Processes given tag :
    private String readTag(XmlPullParser parser,String tagName) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, tagName);
        String tagValue = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, tagName);
        return tagValue;
    }

    // For the tags title and summary, extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }
    
}
