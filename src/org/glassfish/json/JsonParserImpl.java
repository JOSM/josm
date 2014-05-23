/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

import javax.json.*;
import javax.json.stream.JsonLocation;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParsingException;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.*;

import org.glassfish.json.JsonTokenizer.JsonToken;
import org.glassfish.json.api.BufferPool;

/**
 * JSON parser implementation. NoneContext, ArrayContext, ObjectContext is used
 * to go to next parser state.
 *
 * @author Jitendra Kotamraju
 */
public class JsonParserImpl implements JsonParser {

    private Context currentContext = new NoneContext();
    private Event currentEvent;

    private final Stack stack = new Stack();
    private final StateIterator stateIterator;
    private final JsonTokenizer tokenizer;

    public JsonParserImpl(Reader reader, BufferPool bufferPool) {
        tokenizer = new JsonTokenizer(reader, bufferPool);
        stateIterator = new StateIterator();
    }

    public JsonParserImpl(InputStream in, BufferPool bufferPool) {
        UnicodeDetectingInputStream uin = new UnicodeDetectingInputStream(in);
        tokenizer = new JsonTokenizer(new InputStreamReader(uin, uin.getCharset()), bufferPool);
        stateIterator = new StateIterator();
    }

    public JsonParserImpl(InputStream in, Charset encoding, BufferPool bufferPool) {
        tokenizer = new JsonTokenizer(new InputStreamReader(in, encoding), bufferPool);
        stateIterator = new StateIterator();
    }

    public String getString() {
        if (currentEvent == Event.KEY_NAME || currentEvent == Event.VALUE_STRING
                || currentEvent == Event.VALUE_NUMBER) {
            return tokenizer.getValue();
        }
        throw new IllegalStateException(
                JsonMessages.PARSER_GETSTRING_ERR(currentEvent));
    }

    @Override
    public boolean isIntegralNumber() {
        if (currentEvent != Event.VALUE_NUMBER) {
            throw new IllegalStateException(
                    JsonMessages.PARSER_ISINTEGRALNUMBER_ERR(currentEvent));
        }
        return tokenizer.isIntegral();
    }

    @Override
    public int getInt() {
        if (currentEvent != Event.VALUE_NUMBER) {
            throw new IllegalStateException(
                    JsonMessages.PARSER_GETINT_ERR(currentEvent));
        }
        return tokenizer.getInt();
    }

    boolean isDefinitelyInt() {
        return tokenizer.isDefinitelyInt();
    }

    @Override
    public long getLong() {
        if (currentEvent != Event.VALUE_NUMBER) {
            throw new IllegalStateException(
                    JsonMessages.PARSER_GETLONG_ERR(currentEvent));
        }
        return tokenizer.getBigDecimal().longValue();
    }

    @Override
    public BigDecimal getBigDecimal() {
        if (currentEvent != Event.VALUE_NUMBER) {
            throw new IllegalStateException(
                    JsonMessages.PARSER_GETBIGDECIMAL_ERR(currentEvent));
        }
        return tokenizer.getBigDecimal();
    }

    @Override
    public JsonLocation getLocation() {
        return tokenizer.getLocation();
    }

    public JsonLocation getLastCharLocation() {
        return tokenizer.getLastCharLocation();
    }

    public boolean hasNext() {
        return stateIterator.hasNext();
    }

    public Event next() {
        return stateIterator.next();
    }

    private class StateIterator implements  Iterator<JsonParser.Event> {

        @Override
        public boolean hasNext() {
            if (stack.isEmpty() && (currentEvent == Event.END_ARRAY || currentEvent == Event.END_OBJECT)) {
                JsonToken token = tokenizer.nextToken();
                if (token != JsonToken.EOF) {
                    throw new JsonParsingException(JsonMessages.PARSER_EXPECTED_EOF(token),
                            getLastCharLocation());
                }
                return false;
            }
            return true;
        }

        @Override
        public JsonParser.Event next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return currentEvent = currentContext.getNextEvent();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public void close() {
        try {
            tokenizer.close();
        } catch (IOException e) {
            throw new JsonException(JsonMessages.PARSER_TOKENIZER_CLOSE_IO(), e);
        }
    }

    // Using the optimized stack impl as we don't require other things
    // like iterator etc.
    private static final class Stack {
        private Context head;

        private void push(Context context) {
            context.next = head;
            head = context;
        }

        private Context pop() {
            if (head == null) {
                throw new NoSuchElementException();
            }
            Context temp = head;
            head = head.next;
            return temp;
        }

        private boolean isEmpty() {
            return head == null;
        }
    }

    private abstract class Context {
        Context next;
        abstract Event getNextEvent();
    }

    private final class NoneContext extends Context {
        @Override
        public Event getNextEvent() {
            // Handle 1. {     2. [
            JsonToken token = tokenizer.nextToken();
            if (token == JsonToken.CURLYOPEN) {
                stack.push(currentContext);
                currentContext = new ObjectContext();
                return Event.START_OBJECT;
            } else if (token == JsonToken.SQUAREOPEN) {
                stack.push(currentContext);
                currentContext = new ArrayContext();
                return Event.START_ARRAY;
            }
            throw parsingException(token, "[CURLYOPEN, SQUAREOPEN]");
        }
    }

    private JsonParsingException parsingException(JsonToken token, String expectedTokens) {
        JsonLocation location = getLastCharLocation();
        return new JsonParsingException(
                JsonMessages.PARSER_INVALID_TOKEN(token, location, expectedTokens), location);
    }

    private final class ObjectContext extends Context {
        private boolean firstValue = true;

        /*
         * Some more things could be optimized. For example, instead
         * tokenizer.nextToken(), one could use tokenizer.matchColonToken() to
         * match ':'. That might optimize a bit, but will fragment nextToken().
         * I think the current one is more readable.
         *
         */
        @Override
        public Event getNextEvent() {
            // Handle 1. }   2. name:value   3. ,name:value
            JsonToken token = tokenizer.nextToken();
            if (currentEvent == Event.KEY_NAME) {
                // Handle 1. :value
                if (token != JsonToken.COLON) {
                    throw parsingException(token, "[COLON]");
                }
                token = tokenizer.nextToken();
                if (token.isValue()) {
                    return token.getEvent();
                } else if (token == JsonToken.CURLYOPEN) {
                    stack.push(currentContext);
                    currentContext = new ObjectContext();
                    return Event.START_OBJECT;
                } else if (token == JsonToken.SQUAREOPEN) {
                    stack.push(currentContext);
                    currentContext = new ArrayContext();
                    return Event.START_ARRAY;
                }
                throw parsingException(token, "[CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]");
            } else {
                // Handle 1. }   2. name   3. ,name
                if (token == JsonToken.CURLYCLOSE) {
                    currentContext = stack.pop();
                    return Event.END_OBJECT;
                }
                if (firstValue) {
                    firstValue = false;
                } else {
                    if (token != JsonToken.COMMA) {
                        throw parsingException(token, "[COMMA]");
                    }
                    token = tokenizer.nextToken();
                }
                if (token == JsonToken.STRING) {
                    return Event.KEY_NAME;
                }
                throw parsingException(token, "[STRING]");
            }
        }

    }

    private final class ArrayContext extends Context {
        private boolean firstValue = true;

        // Handle 1. ]   2. value   3. ,value
        @Override
        public Event getNextEvent() {
            JsonToken token = tokenizer.nextToken();
            if (token == JsonToken.SQUARECLOSE) {
                currentContext = stack.pop();
                return Event.END_ARRAY;
            }
            if (firstValue) {
                firstValue = false;
            } else {
                if (token != JsonToken.COMMA) {
                    throw parsingException(token, "[COMMA]");
                }
                token = tokenizer.nextToken();
            }
            if (token.isValue()) {
                return token.getEvent();
            } else if (token == JsonToken.CURLYOPEN) {
                stack.push(currentContext);
                currentContext = new ObjectContext();
                return Event.START_OBJECT;
            } else if (token == JsonToken.SQUAREOPEN) {
                stack.push(currentContext);
                currentContext = new ArrayContext();
                return Event.START_ARRAY;
            }
            throw parsingException(token, "[CURLYOPEN, SQUAREOPEN, STRING, NUMBER, TRUE, FALSE, NULL]");
        }

    }

}
