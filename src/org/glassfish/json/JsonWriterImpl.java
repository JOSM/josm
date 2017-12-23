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

import javax.json.*;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * JsonWriter impl using generator.
 *
 * @author Jitendra Kotamraju
 */
class JsonWriterImpl implements JsonWriter {

    private final JsonGeneratorImpl generator;
    private boolean writeDone;
    private final NoFlushOutputStream os;

    JsonWriterImpl(Writer writer, BufferPool bufferPool) {
        this(writer, false, bufferPool);
    }

    JsonWriterImpl(Writer writer, boolean prettyPrinting, BufferPool bufferPool) {
        generator = prettyPrinting
                ? new JsonPrettyGeneratorImpl(writer, bufferPool)
                : new JsonGeneratorImpl(writer, bufferPool);
        os = null;
    }

    JsonWriterImpl(OutputStream out, BufferPool bufferPool) {
        this(out, StandardCharsets.UTF_8, false, bufferPool);
    }

    JsonWriterImpl(OutputStream out, boolean prettyPrinting, BufferPool bufferPool) {
        this(out, StandardCharsets.UTF_8, prettyPrinting, bufferPool);
    }

    JsonWriterImpl(OutputStream out, Charset charset,
                   boolean prettyPrinting, BufferPool bufferPool) {
        // Decorating the given stream, so that buffered contents can be
        // written without actually flushing the stream.
        this.os = new NoFlushOutputStream(out);
        generator = prettyPrinting
                ? new JsonPrettyGeneratorImpl(os, charset, bufferPool)
                : new JsonGeneratorImpl(os, charset, bufferPool);
    }

    @Override
    public void writeArray(JsonArray array) {
        if (writeDone) {
            throw new IllegalStateException(JsonMessages.WRITER_WRITE_ALREADY_CALLED());
        }
        writeDone = true;
        generator.writeStartArray();
        for(JsonValue value : array) {
            generator.write(value);
        }
        generator.writeEnd();
        // Flush the generator's buffered contents. This won't work for byte
        // streams as intermediary OutputStreamWriter buffers.
        generator.flushBuffer();
        // Flush buffered contents but not the byte stream. generator.flush()
        // does OutputStreamWriter#flushBuffer (package private) and underlying
        // byte stream#flush(). Here underlying stream's flush() is no-op.
        if (os != null) {
            generator.flush();
        }
    }

    @Override
    public void writeObject(JsonObject object) {
        if (writeDone) {
            throw new IllegalStateException(JsonMessages.WRITER_WRITE_ALREADY_CALLED());
        }
        writeDone = true;
        generator.writeStartObject();
        for(Map.Entry<String, JsonValue> e : object.entrySet()) {
            generator.write(e.getKey(), e.getValue());
        }
        generator.writeEnd();
        // Flush the generator's buffered contents. This won't work for byte
        // streams as intermediary OutputStreamWriter buffers.
        generator.flushBuffer();
        // Flush buffered contents but not the byte stream. generator.flush()
        // does OutputStreamWriter#flushBuffer (package private) and underlying
        // byte stream#flush(). Here underlying stream's flush() is no-op.
        if (os != null) {
            generator.flush();
        }
    }

    @Override
    public void write(JsonStructure value) {
        if (value instanceof JsonArray) {
            writeArray((JsonArray)value);
        } else {
            writeObject((JsonObject)value);
        }
    }

    @Override
    public void write(JsonValue value) {
        switch (value.getValueType()) {
            case OBJECT:
                writeObject((JsonObject) value);
                return;
            case ARRAY:
                writeArray((JsonArray) value);
                return;
            default:
                if (writeDone) {
                    throw new IllegalStateException(JsonMessages.WRITER_WRITE_ALREADY_CALLED());
                }
                writeDone = true;
                generator.write(value);
                generator.flushBuffer();
                if (os != null) {
                    generator.flush();
                }
        }
    }

    @Override
    public void close() {
        writeDone = true;
        generator.close();
    }

    private static final class NoFlushOutputStream extends FilterOutputStream {
        public NoFlushOutputStream(OutputStream out) {
            super(out);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            out.write(b, off ,len);
        }

        @Override
        public void flush() {
            // no-op
        }
    }

}
