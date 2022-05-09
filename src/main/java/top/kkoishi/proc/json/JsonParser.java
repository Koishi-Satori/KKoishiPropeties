package top.kkoishi.proc.json;

import kotlin.collections.ArrayDeque;
import top.kkoishi.proc.property.BuildFailedException;
import top.kkoishi.proc.property.Files;
import top.kkoishi.proc.property.TokenizeException;

import java.io.File;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import static top.kkoishi.proc.json.JsonSupportKt.*;

/**
 * A simple Json Parser based on Complete Principle.
 *
 * @author KKoishi_
 */
public class JsonParser {
    /**
     * Json data types.
     */
    public enum JsonType {
        /**
         * Json String Reflection in Java.
         */
        STRING,
        /**
         * Json Boolean Reflection in Java.
         */
        BOOLEAN,
        /**
         * The beginning of Json Array.
         */
        ARRAY_BEGIN,
        /**
         * The end of Json Array.
         */
        ARRAY_END,
        /**
         * Json Number reflection in Java.
         */
        NUMBER,
        /**
         * The beginning of Json Object.
         */
        OBJ_BEGIN,
        /**
         * The end of Json Object.
         */
        OBJ_END,
        /**
         * The separator between json entry's key and value.(':')
         */
        SEP_ENTRY,
        /**
         * The separator among json entries.(',')
         */
        SEP_COMMA,
        NULL
    }

    /**
     * Token class.
     * The lexer in the parser will split the json
     * text to token list before translate to an AST.
     */
    public static final class Token {
        /**
         * Construct a token instance.
         *
         * @param type  type of the token.
         * @param value value to be stored.
         */
        public Token (JsonType type, String value) {
            this.type = type;
            this.value = value;
        }

        private JsonType type;
        private String value;

        public JsonType getType () {
            return type;
        }

        public void setType (JsonType type) {
            this.type = type;
        }

        public String getValue () {
            return value;
        }

        public void setValue (String value) {
            this.value = value;
        }

        @Override
        public String toString () {
            return "Token{" +
                    "type=" + type +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

    /**
     * Simple token list used to store tokens.
     */
    public static final class TokenList {
        private int head = 0;
        private Token[] elements = new Token[0];

        public TokenList () {
        }

        public int size () {
            return elements.length - head;
        }

        public boolean isEmpty () {
            return elements.length - head <= 1;
        }

        void add (Token token) {
            grow(1);
            elements[elements.length - 1] = token;
        }

        void plus (TokenList tokenList) {
            final int aSize = tokenList.elements.length - tokenList.head;
            grow(aSize);
            for (int i = 1; i <= aSize; i++) {
                elements[elements.length - i] = tokenList.remove();
            }
            tokenList.clear();
        }

        Token remove () {
            if (elements.length - 1 == head) {
                throw new NoSuchElementException("The TokenList is empty.");
            }
            final Token oldVal = elements[head];
            elements[head++] = null;
            return oldVal;
        }

        private void grow (int needed) {
            final int size = elements.length;
            final Token[] cpy = elements;
            elements = new Token[size + needed];
            System.arraycopy(cpy, 0, elements, 0, cpy.length);
        }

        public void clear () {
            for (int i = head; i < elements.length; i++) {
                elements[i] = null;
            }
            head = 0;

            elements = null;
        }

        @Override
        public String toString () {
            final StringBuilder sb = new StringBuilder("TokenList{");
            for (int i = head; i < elements.length - 1; i++) {
                sb.append(elements[i]).append(", ");
            }
            sb.append(elements[elements.length - 1]).append("}");
            return sb.toString();
        }

        public void forEach (Consumer<Token> action) {
            for (int i = head; i < elements.length; i++) {
                action.accept(elements[i]);
            }
        }
    }

    protected static class Builder {
        protected TokenList tokens;
        JsonObject result = null;
        private boolean closed = false;

        public Builder (TokenList tokens) {
            this.tokens = tokens;
        }

        private void setTokens (TokenList tokens) throws BuildFailedException {
            if (closed) {
                throw new BuildFailedException("The Json Builder has been closed.");
            }
            if (this.tokens != null) {
                this.tokens.clear();
            }
            this.tokens = tokens;
        }

        public void build () throws BuildFailedException, JsonSyntaxException {
            if (closed) {
                throw new BuildFailedException("The Json Builder has been closed.");
            }
            closed = true;
            if (tokens.remove().type != JsonType.OBJ_BEGIN) {
                throw new JsonSyntaxException("The json must have a object begin token.");
            }
            result = build0();
            System.out.println(result);
        }

        @SuppressWarnings("EnhancedSwitchMigration")
        protected JsonObject build0 () throws BuildFailedException, JsonSyntaxException {
            final JsonObject object = new JsonObject();
            Entry temp = null;
            while (!tokens.isEmpty()) {
                final var token = tokens.remove();
                switch (token.type) {
                    case STRING: {
                        if (temp != null) {
                            temp.setValue(jsonTokenCast(token));
                        } else {
                            temp = new Entry(token.value, null);
                        }
                        break;
                    }
                    case NUMBER: {
                        if (temp == null) {
                            throw new BuildFailedException();
                        }
                        temp.setValue(jsonTokenCast(token));
                        break;
                    }
                    case BOOLEAN: {
                        if (temp == null) {
                            throw new BuildFailedException();
                        }
                        temp.setValue("0".equals(token.value));
                        break;
                    }
                    case SEP_ENTRY: {
                        //todo:finish this.
                        break;
                    }
                    case SEP_COMMA: {
                        if (temp == null) {
                            throw new JsonSyntaxException();
                        }
                        object.data.add(temp.cast2Pair());
                        temp = null;
                        break;
                    }
                    case OBJ_BEGIN: {
                        if (temp == null) {
                            throw new JsonSyntaxException();
                        }
                        //build object.
                        temp.setValue(build0());
                        break;
                    }
                    case OBJ_END: {
                        if (temp != null) {
                            object.data.add(temp.cast2Pair());
                        }
                        return object;
                    }
                    case ARRAY_END: {
                        //this should not happen.
                        throw new JsonSyntaxException();
                    }
                    case ARRAY_BEGIN: {
                        if (temp == null) {
                            throw new JsonSyntaxException();
                        }
                        //build array.
                        final var buf = buildArrayElement();
                        final Object[] arr = new Object[buf.size()];
                        int index = 0;
                        while (!buf.isEmpty()) {
                            arr[index++] = buf.removeLast();
                        }
                        temp.setValue(arr);
                        break;
                    }
                    case NULL: {
                        if (temp == null) {
                            throw new JsonSyntaxException();
                        }
                        temp.setValue(null);
                        break;
                    }
                    default: throw new BuildFailedException();
                }
            }
            if (temp != null) {
                object.data.add(temp.cast2Pair());
            }
            return object;
        }

        @SuppressWarnings("EnhancedSwitchMigration")
        protected ArrayDeque<Object> buildArrayElement () throws JsonSyntaxException, BuildFailedException {
            final ArrayDeque<Object> arr = new ArrayDeque<>();
            while (!tokens.isEmpty()) {
                final var token = tokens.remove();
                switch (token.type) {
                    case STRING:
                    case NUMBER: {
                        arr.add(jsonTokenCast(token));
                        break;
                    }
                    case BOOLEAN: {
                        arr.add("0".equals(token.value));
                        break;
                    }
                    case SEP_COMMA: {
                        //continue.
                        break;
                    }
                    case OBJ_BEGIN: {
                        arr.add(build0());
                        break;
                    }
                    case SEP_ENTRY:
                    case OBJ_END: {
                        throw new JsonSyntaxException();
                    }
                    case ARRAY_BEGIN: {
                        final var buf = buildArrayElement();
                        final Object[] array = new Object[buf.size()];
                        int index = 0;
                        while (!buf.isEmpty()) {
                            array[index++] = buf.removeLast();
                        }
                        arr.add(array);
                        break;
                    }
                    case ARRAY_END: {
                        return arr;
                    }
                    case NULL:{
                        arr.add(null);
                        break;
                    }
                    default: {
                        //this should not happen.
                        throw new BuildFailedException();
                    }
                }
            }
            throw new JsonSyntaxException();
        }
    }

    protected Iterator<Character> rest;
    protected final StringBuilder buf = new StringBuilder();
    protected char lookForward;
    protected final ArrayDeque<Character> stack = new ArrayDeque<>(8);
    protected Builder builder = new Builder(null);

    public JsonParser (Iterator<Character> rest) {
        this.rest = rest;
    }

    public JsonParser (String json) {
        this(JsonSupportKt.getStringIterator(json));
    }

    public void parse () throws JsonSyntaxException, BuildFailedException {
        builder.setTokens(new TokenList());
        lookForward();
        block(this);
//        //debug use.
//        this.builder.tokens.forEach(System.out::println);
        this.builder.build();
    }

    @SuppressWarnings("all")
    protected final void jump () throws TokenizeException, BuildFailedException {
        if (!rest.hasNext() && stack.isEmpty()) {
            return;
        }
        switch (lookForward) {
            case '{':
            case '}': {
                block(this);
                break;
            }
            case ':': {
                builder.tokens.add(new Token(JsonType.SEP_ENTRY, null));
                sep(this);
                break;
            }
            case ',': {
                builder.tokens.add(new Token(JsonType.SEP_COMMA, null));
                sep(this);
                break;
            }
            case ' ':
            case '\t':
            case '\n':
            case '\r': {
                lookForward();
                jump();
                break;
            }
            case '"': {
                key(this);
                break;
            }
            case 't':
            case 'f': {
                clearBuf();
                this.buf.append(lookForward);
                bool(this);
                this.jump();
            }
            case '[': {
                array(this);
                break;
            }
            case ']': {
                if (stack.isEmpty()) {
                    throw new TokenizeException();
                }
                if (stack.removeLast() == '[') {
                    this.builder.tokens.add(new Token(JsonType.ARRAY_END, null));
                    lookForward();
                    jump();
                } else {
                    throw new JsonSyntaxException();
                }
                break;
            }
            case 'n': {
                clearBuf();
                this.buf.append(lookForward);
                nil(this);
                this.jump();
            }
            default: {
                if (JsonSupportKt.getNUMBER_MAP().contains(lookForward)) {
                    numberValue(this);
                    break;
                } else {
                    if (!rest.hasNext() && stack.isEmpty()) {
                        return;
                    }
                    throw new JsonSyntaxException("The token '" + lookForward + "' is illegal.");
                }
            }
        }
    }

    public JsonObject result () {
        return builder.result;
    }

    public void reset (String json) {
        this.rest = JsonSupportKt.getStringIterator(json);
        builder = new Builder(null);
    }

    public void reset (Iterator<Character> iterator) {
        this.rest = iterator;
        builder = new Builder(null);
    }

    protected final void lookForward () {
        lookForward = rest.next();
    }

    protected final void clearBuf () {
        clear(buf);
    }
}
