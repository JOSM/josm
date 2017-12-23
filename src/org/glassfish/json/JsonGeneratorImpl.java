/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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
import javax.json.stream.JsonGenerationException;
import javax.json.stream.JsonGenerator;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * @author Jitendra Kotamraju
 */
class JsonGeneratorImpl implements JsonGenerator {

    private static final char[] INT_MIN_VALUE_CHARS = "-2147483648".toCharArray();
    private static final int[] INT_CHARS_SIZE_TABLE = { 9, 99, 999, 9999, 99999,
            999999, 9999999, 99999999, 999999999, Integer.MAX_VALUE };

    private static final char [] DIGIT_TENS = {
            '0', '0', '0', '0', '0', '0', '0', '0', '0', '0',
            '1', '1', '1', '1', '1', '1', '1', '1', '1', '1',
            '2', '2', '2', '2', '2', '2', '2', '2', '2', '2',
            '3', '3', '3', '3', '3', '3', '3', '3', '3', '3',
            '4', '4', '4', '4', '4', '4', '4', '4', '4', '4',
            '5', '5', '5', '5', '5', '5', '5', '5', '5', '5',
            '6', '6', '6', '6', '6', '6', '6', '6', '6', '6',
            '7', '7', '7', '7', '7', '7', '7', '7', '7', '7',
            '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
            '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',
    } ;

    private static final char [] DIGIT_ONES = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    } ;

    /**
     * All possible chars for representing a number as a String
     */
    private static final char[] DIGITS = {
            '0' , '1' , '2' , '3' , '4' , '5' ,
            '6' , '7' , '8' , '9'
    };

    private static enum Scope {
        IN_NONE,
        IN_OBJECT,
        IN_FIELD,
        IN_ARRAY
    }

    private final BufferPool bufferPool;
    private final Writer writer;
    private Context currentContext = new Context(Scope.IN_NONE);
    private final Deque<Context> stack = new ArrayDeque<>();

    // Using own buffering mechanism as JDK's BufferedWriter uses synchronized
    // methods. Also, flushBuffer() is useful when you don't want to actually
    // flush the underlying output source
    private final char buf[];     // capacity >= INT_MIN_VALUE_CHARS.length
    private int len = 0;

    JsonGeneratorImpl(Writer writer, BufferPool bufferPool) {
        this.writer = writer;
        this.bufferPool = bufferPool;
        this.buf = bufferPool.take();
    }

    JsonGeneratorImpl(OutputStream out, BufferPool bufferPool) {
        this(out, StandardCharsets.UTF_8, bufferPool);
    }

    JsonGeneratorImpl(OutputStream out, Charset encoding, BufferPool bufferPool) {
        this(new OutputStreamWriter(out, encoding), bufferPool);
    }

    @Override
    public void flush() {
        flushBuffer();
        try {
            writer.flush();
        } catch (IOException ioe) {
            throw new JsonException(JsonMessages.GENERATOR_FLUSH_IO_ERR(), ioe);
        }
    }

    @Override
    public JsonGenerator writeStartObject() {
        if (currentContext.scope == Scope.IN_OBJECT) {
            throw new JsonGenerationException(JsonMessages.GENERATOR_ILLEGAL_METHOD(currentContext.scope));
        }
        if (currentContext.scope == Scope.IN_NONE && !currentContext.first) {
            throw new JsonGenerationException(JsonMessages.GENERATOR_ILLEGAL_MULTIPLE_TEXT());
        }
        writeComma();
        writeChar('{');
        stack.push(currentContext);
        currentContext = new Context(Scope.IN_OBJECT);
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(String name) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException(
                    JsonMessages.GENERATOR_ILLEGAL_METHOD(currentContext.scope));
        }
        writeName(name);
        writeChar('{');
        stack.push(currentContext);
        currentContext = new Context(Scope.IN_OBJECT);
        return this;
    }

    private JsonGenerator writeName(String name) {
        writeComma();
        writeEscapedString(name);
        writeColon();
        return this;
    }

    @Override
    public JsonGenerator write(String name, String fieldValue) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException(
                    JsonMessages.GENERATOR_ILLEGAL_METHOD(currentContext.scope));
        }
        writeName(name);
        writeEscapedString(fieldValue);
        return this;
    }

    @Override
    public JsonGenerator write(String name, int value) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException(
                    JsonMessages.GENERATOR_ILLEGAL_METHOD(currentContext.scope));
        }
        writeName(name);
        writeInt(value);
        return this;
    }

    @Override
    public JsonGenerator write(String name, long value) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException(
                    JsonMessages.GENERATOR_ILLEGAL_METHOD(currentContext.scope));
        }
        writeName(name);
        writeString(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(String name, double value) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException(
                    JsonMessages.GENERATOR_ILLEGAL_METHOD(currentContext.scope));
        }
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new NumberFormatException(JsonMessages.GENERATOR_DOUBLE_INFINITE_NAN());
        }
        writeName(name);
        writeString(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(String name, BigInteger value) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException(
                    JsonMessages.GENERATOR_ILLEGAL_METHOD(currentContext.scope));
        }
        writeName(name);
        writeString(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(String name, BigDecimal value) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException(
                    JsonMessages.GENERATOR_ILLEGAL_METHOD(currentContext.scope));
        }
        writeName(name);
        writeString(String.valueOf(value));
        return this;
    }

    @Override
    public JsonGenerator write(String name, boolean value) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException(
                    JsonMessages.GENERATOR_ILLEGAL_METHOD(currentContext.scope));
        }
        writeName(name);
        writeString(value? "true" : "false");
        return this;
    }

    @Override
    public JsonGenerator writeNull(String name) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException(
                    JsonMessages.GENERATOR_ILLEGAL_METHOD(currentContext.scope));
        }
        writeName(name);
        writeString("null");
        return this;
    }

    @Override
    public JsonGenerator write(JsonValue value) {
        checkContextForValue();

        switch (value.getValueType()) {
            case ARRAY:
                JsonArray array = (JsonArray)value;
                writeStartArray();
                for(JsonValue child: array) {
                    write(child);
                }
                writeEnd();
                break;
            case OBJECT:
                JsonObject object = (JsonObject)value;
                writeStartObject();
                for(Map.Entry<String, JsonValue> member: object.entrySet()) {
                    write(member.getKey(), member.getValue());
                }
                writeEnd();
                break;
            case STRING:
                JsonString str = (JsonString)value;
                write(str.getString());
                break;
            case NUMBER:
                JsonNumber number = (JsonNumber)value;
                writeValue(number.toString());
                popFieldContext();
                break;
            case TRUE:
                write(true);
                break;
            case FALSE:
                write(false);
                break;
            case NULL:
                writeNull();
                break;
        }

        return this;
    }

    @Override
    public JsonGenerator writeStartArray() {
        if (currentContext.scope == Scope.IN_OBJECT) {
            throw new JsonGenerationException(JsonMessages.GENERATOR_ILLEGAL_METHOD(currentContext.scope));
        }
        if (currentContext.scope == Scope.IN_NONE && !currentContext.first) {
            throw new JsonGenerationException(JsonMessages.GENERATOR_ILLEGAL_MULTIPLE_TEXT());
        }
        writeComma();
        writeChar('[');
        stack.push(currentContext);
        currentContext = new Context(Scope.IN_ARRAY);
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(String name) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException(
                    JsonMessages.GENERATOR_ILLEGAL_METHOD(currentContext.scope));
        }
        writeName(name);
        writeChar('[');
        stack.push(currentContext);
        currentContext = new Context(Scope.IN_ARRAY);
        return this;
    }

    @Override
    public JsonGenerator write(String name, JsonValue value) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException(
                    JsonMessages.GENERATOR_ILLEGAL_METHOD(currentContext.scope));
        }
        switch (value.getValueType()) {
            case ARRAY:
                JsonArray array = (JsonArray)value;
                writeStartArray(name);
                for(JsonValue child: array) {
                    write(child);
                }
                writeEnd();
                break;
            case OBJECT:
                JsonObject object = (JsonObject)value;
                writeStartObject(name);
                for(Map.Entry<String, JsonValue> member: object.entrySet()) {
                    write(member.getKey(), member.getValue());
                }
                writeEnd();
                break;
            case STRING:
                JsonString str = (JsonString)value;
                write(name, str.getString());
                break;
            case NUMBER:
                JsonNumber number = (JsonNumber)value;
                writeValue(name, number.toString());
                break;
            case TRUE:
                write(name, true);
                break;
            case FALSE:
                write(name, false);
                break;
            case NULL:
                writeNull(name);
                break;
        }
        return this;
    }

    @Override
    public JsonGenerator write(String value) {
        checkContextForValue();
        writeComma();
        writeEscapedString(value);
        popFieldContext();
        return this;
    }


    @Override
    public JsonGenerator write(int value) {
        checkContextForValue();
        writeComma();
        writeInt(value);
        popFieldContext();
        return this;
    }

    @Override
    public JsonGenerator write(long value) {
        checkContextForValue();
        writeValue(String.valueOf(value));
        popFieldContext();
        return this;
    }

    @Override
    public JsonGenerator write(double value) {
        checkContextForValue();
        if (Double.isInfinite(value) || Double.isNaN(value)) {
            throw new NumberFormatException(JsonMessages.GENERATOR_DOUBLE_INFINITE_NAN());
        }
        writeValue(String.valueOf(value));
        popFieldContext();
        return this;
    }

    @Override
    public JsonGenerator write(BigInteger value) {
        checkContextForValue();
        writeValue(value.toString());
        popFieldContext();
        return this;
    }

    private void checkContextForValue() {
        if ((!currentContext.first && currentContext.scope != Scope.IN_ARRAY && currentContext.scope != Scope.IN_FIELD)
                || (currentContext.first && currentContext.scope == Scope.IN_OBJECT)) {
            throw new JsonGenerationException(
                    JsonMessages.GENERATOR_ILLEGAL_METHOD(currentContext.scope));
        }
    }

    @Override
    public JsonGenerator write(BigDecimal value) {
        checkContextForValue();
        writeValue(value.toString());
        popFieldContext();

        return this;
    }

    private void popFieldContext() {
        if (currentContext.scope == Scope.IN_FIELD) {
            currentContext = stack.pop();
        }
    }

    @Override
    public JsonGenerator write(boolean value) {
        checkContextForValue();
        writeComma();
        writeString(value ? "true" : "false");
        popFieldContext();
        return this;
    }

    @Override
    public JsonGenerator writeNull() {
        checkContextForValue();
        writeComma();
        writeString("null");
        popFieldContext();
        return this;
    }

    private void writeValue(String value) {
        writeComma();
        writeString(value);
    }

    private void writeValue(String name, String value) {
        writeComma();
        writeEscapedString(name);
        writeColon();
        writeString(value);
    }

    @Override
    public JsonGenerator writeKey(String name) {
        if (currentContext.scope != Scope.IN_OBJECT) {
            throw new JsonGenerationException(
                    JsonMessages.GENERATOR_ILLEGAL_METHOD(currentContext.scope));
        }
        writeName(name);
        stack.push(currentContext);
        currentContext = new Context(Scope.IN_FIELD);
        currentContext.first = false;
        return this;
    }

    @Override
    public JsonGenerator writeEnd() {
        if (currentContext.scope == Scope.IN_NONE) {
            throw new JsonGenerationException("writeEnd() cannot be called in no context");
        }
        writeChar(currentContext.scope == Scope.IN_ARRAY ? ']' : '}');
        currentContext = stack.pop();
        popFieldContext();
        return this;
    }

    protected void writeComma() {
        if (!currentContext.first && currentContext.scope != Scope.IN_FIELD) {
            writeChar(',');
        }
        currentContext.first = false;
    }

    protected void writeColon() {
        writeChar(':');
    }

    private static class Context {
        boolean first = true;
        final Scope scope;

        Context(Scope scope) {
            this.scope = scope;
        }

    }

    @Override
    public void close() {
        if (currentContext.scope != Scope.IN_NONE || currentContext.first) {
            throw new JsonGenerationException(JsonMessages.GENERATOR_INCOMPLETE_JSON());
        }
        flushBuffer();
        try {
            writer.close();
        } catch (IOException ioe) {
            throw new JsonException(JsonMessages.GENERATOR_CLOSE_IO_ERR(), ioe);
        }
        bufferPool.recycle(buf);
    }

    // begin, end-1 indexes represent characters that need not
    // be escaped
    //
    // XXXssssssssssssXXXXXXXXXXXXXXXXXXXXXXrrrrrrrrrrrrrrXXXXXX
    //    ^           ^                     ^             ^
    //    |           |                     |             |
    //   begin       end                   begin         end
    void writeEscapedString(String string) {
        writeChar('"');
        int len = string.length();
        for(int i = 0; i < len; i++) {
            int begin = i, end = i;
            char c = string.charAt(i);
            // find all the characters that need not be escaped
            // unescaped = %x20-21 | %x23-5B | %x5D-10FFFF
            while(c >= 0x20 && c <= 0x10ffff && c != 0x22 && c != 0x5c) {
                i++; end = i;
                if (i < len) {
                    c = string.charAt(i);
                } else {
                    break;
                }
            }
            // Write characters without escaping
            if (begin < end) {
                writeString(string, begin, end);
                if (i == len) {
                    break;
                }
            }

            switch (c) {
                case '"':
                case '\\':
                    writeChar('\\'); writeChar(c);
                    break;
                case '\b':
                    writeChar('\\'); writeChar('b');
                    break;
                case '\f':
                    writeChar('\\'); writeChar('f');
                    break;
                case '\n':
                    writeChar('\\'); writeChar('n');
                    break;
                case '\r':
                    writeChar('\\'); writeChar('r');
                    break;
                case '\t':
                    writeChar('\\'); writeChar('t');
                    break;
                default:
                    String hex = "000" + Integer.toHexString(c);
                    writeString("\\u" + hex.substring(hex.length() - 4));
            }
        }
        writeChar('"');
    }

    void writeString(String str, int begin, int end) {
        while (begin < end) {       // source begin and end indexes
            int no = Math.min(buf.length - len, end - begin);
            str.getChars(begin, begin + no, buf, len);
            begin += no;            // Increment source index
            len += no;              // Increment dest index
            if (len >= buf.length) {
                flushBuffer();
            }
        }
    }

    void writeString(String str) {
        writeString(str, 0, str.length());
    }

    void writeChar(char c) {
        if (len >= buf.length) {
            flushBuffer();
        }
        buf[len++] = c;
    }

    // Not using Integer.toString() since it creates intermediary String
    // Also, we want the chars to be copied to our buffer directly
    void writeInt(int num) {
        int size;
        if (num == Integer.MIN_VALUE) {
            size = INT_MIN_VALUE_CHARS.length;
        } else {
            size = (num < 0) ? stringSize(-num) + 1 : stringSize(num);
        }
        if (len+size >= buf.length) {
            flushBuffer();
        }
        if (num == Integer.MIN_VALUE) {
            System.arraycopy(INT_MIN_VALUE_CHARS, 0, buf, len, size);
        } else {
            fillIntChars(num, buf, len+size);
        }
        len += size;
    }

    // flushBuffer writes the buffered contents to writer. But incase of
    // byte stream, an OuputStreamWriter is created and that buffers too.
    // We may need to call OutputStreamWriter#flushBuffer() using
    // reflection if that is really required (commented out below)
    void flushBuffer() {
        try {
            if (len > 0) {
                writer.write(buf, 0, len);
                len = 0;
            }
        } catch (IOException ioe) {
            throw new JsonException(JsonMessages.GENERATOR_WRITE_IO_ERR(), ioe);
        }
    }

//    private static final Method flushBufferMethod;
//    static {
//        Method m = null;
//        try {
//            m = OutputStreamWriter.class.getDeclaredMethod("flushBuffer");
//            m.setAccessible(true);
//        } catch (Exception e) {
//            // no-op
//        }
//        flushBufferMethod = m;
//    }
//    void flushBufferOSW() {
//        flushBuffer();
//        if (writer instanceof OutputStreamWriter) {
//            try {
//                flushBufferMethod.invoke(writer);
//            } catch (Exception e) {
//                // no-op
//            }
//        }
//    }

    // Requires positive x
    private static int stringSize(int x) {
        for (int i=0; ; i++)
            if (x <= INT_CHARS_SIZE_TABLE[i])
                return i+1;
    }

    /**
     * Places characters representing the integer i into the
     * character array buf. The characters are placed into
     * the buffer backwards starting with the least significant
     * digit at the specified index (exclusive), and working
     * backwards from there.
     *
     * Will fail if i == Integer.MIN_VALUE
     */
    private static void fillIntChars(int i, char[] buf, int index) {
        int q, r;
        int charPos = index;
        char sign = 0;

        if (i < 0) {
            sign = '-';
            i = -i;
        }

        // Generate two digits per iteration
        while (i >= 65536) {
            q = i / 100;
            // really: r = i - (q * 100);
            r = i - ((q << 6) + (q << 5) + (q << 2));
            i = q;
            buf [--charPos] = DIGIT_ONES[r];
            buf [--charPos] = DIGIT_TENS[r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i <= 65536, i);
        for (;;) {
            q = (i * 52429) >>> (16+3);
            r = i - ((q << 3) + (q << 1));  // r = i-(q*10) ...
            buf [--charPos] = DIGITS[r];
            i = q;
            if (i == 0) break;
        }
        if (sign != 0) {
            buf [--charPos] = sign;
        }
    }

}
