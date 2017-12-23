/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.json;

import org.glassfish.json.api.BufferPool;

import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;

/**
 * JsonReader impl using parser and builders.
 *
 * @author Jitendra Kotamraju
 */
class JsonReaderImpl implements JsonReader {
    private final JsonParserImpl parser;
    private boolean readDone;
    private final BufferPool bufferPool;

    JsonReaderImpl(Reader reader, BufferPool bufferPool) {
        parser = new JsonParserImpl(reader, bufferPool);
        this.bufferPool = bufferPool;
    }

    JsonReaderImpl(InputStream in, BufferPool bufferPool) {
        parser = new JsonParserImpl(in, bufferPool);
        this.bufferPool = bufferPool;
    }

    JsonReaderImpl(InputStream in, Charset charset, BufferPool bufferPool) {
        parser = new JsonParserImpl(in, charset, bufferPool);
        this.bufferPool = bufferPool;
    }

    @Override
    public JsonStructure read() {
        if (readDone) {
            throw new IllegalStateException(JsonMessages.READER_READ_ALREADY_CALLED());
        }
        readDone = true;
        if (parser.hasNext()) {
            try {
                JsonParser.Event e = parser.next();
                if (e == JsonParser.Event.START_ARRAY) {
                    return parser.getArray();
                } else if (e == JsonParser.Event.START_OBJECT) {
                    return parser.getObject();
                }
            } catch (IllegalStateException ise) {
                throw new JsonParsingException(ise.getMessage(), ise, parser.getLastCharLocation());
            }
        }
        throw new JsonException(JsonMessages.INTERNAL_ERROR());
    }

    @Override
    public JsonObject readObject() {
        if (readDone) {
            throw new IllegalStateException(JsonMessages.READER_READ_ALREADY_CALLED());
        }
        readDone = true;
        if (parser.hasNext()) {
            try {
                parser.next();
                return parser.getObject();
            } catch (IllegalStateException ise) {
                throw new JsonParsingException(ise.getMessage(), ise, parser.getLastCharLocation());
            }
        }
        throw new JsonException(JsonMessages.INTERNAL_ERROR());
    }

    @Override
    public JsonArray readArray() {
        if (readDone) {
            throw new IllegalStateException(JsonMessages.READER_READ_ALREADY_CALLED());
        }
        readDone = true;
        if (parser.hasNext()) {
            try {
                parser.next();
                return parser.getArray();
            } catch (IllegalStateException ise) {
                throw new JsonParsingException(ise.getMessage(), ise, parser.getLastCharLocation());
            }
        }
        throw new JsonException(JsonMessages.INTERNAL_ERROR());
    }

    @Override
    public JsonValue readValue() {
        if (readDone) {
            throw new IllegalStateException(JsonMessages.READER_READ_ALREADY_CALLED());
        }
        readDone = true;
        if (parser.hasNext()) {
            try {
                parser.next();
                return parser.getValue();
            } catch (IllegalStateException ise) {
                throw new JsonParsingException(ise.getMessage(), ise, parser.getLastCharLocation());
            }
        }
        throw new JsonException(JsonMessages.INTERNAL_ERROR());
    }

    @Override
    public void close() {
        readDone = true;
        parser.close();
    }
}
