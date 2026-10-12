package com.assemblyproject.cloudsystem.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializador/parser JSON mínimo, sem dependências externas.
 * Suficiente para os objectos simples usados pelo agente (Map, List, String,
 * Number, Boolean).
 */
final class SimpleJson {

    private SimpleJson() {
    }

    static String toJson(Object value) {
        StringBuilder sb = new StringBuilder();
        write(value, sb);
        return sb.toString();
    }

    private static void write(Object value, StringBuilder sb) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(s, sb);
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                writeString(String.valueOf(e.getKey()), sb);
                sb.append(':');
                write(e.getValue(), sb);
            }
            sb.append('}');
        } else if (value instanceof List<?> list) {
            sb.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) sb.append(',');
                first = false;
                write(item, sb);
            }
            sb.append(']');
        } else {
            writeString(value.toString(), sb);
        }
    }

    private static void writeString(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    /**
     * Parser JSON simples para objectos planos ({@code {"chave": valor}}).
     * Assume que o input é um objecto JSON válido sem aninhamento complexo.
     */
    static Map<String, Object> parseObject(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (json == null || json.isBlank()) {
            return result;
        }
        Parser parser = new Parser(json);
        return parser.parseObject();
    }

    private static final class Parser {
        private final String src;
        private int pos;

        Parser(String src) {
            this.src = src;
        }

        Map<String, Object> parseObject() {
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (!consume('{')) {
                return map;
            }
            skipWhitespace();
            if (consume('}')) {
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                consume(':');
                skipWhitespace();
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (consume('}')) {
                    break;
                }
                consume(',');
            }
            return map;
        }

        private Object parseValue() {
            skipWhitespace();
            char c = peek();
            if (c == '"') {
                return parseString();
            }
            if (c == '{') {
                return parseObject();
            }
            if (c == '[') {
                return parseArray();
            }
            if (c == 't' || c == 'f') {
                return parseBoolean();
            }
            if (c == 'n') {
                expect("null");
                return null;
            }
            return parseNumber();
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            consume('[');
            skipWhitespace();
            if (consume(']')) {
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (consume(']')) {
                    break;
                }
                consume(',');
            }
            return list;
        }

        private String parseString() {
            consume('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') {
                    break;
                }
                if (c == '\\') {
                    char esc = next();
                    switch (esc) {
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            StringBuilder hex = new StringBuilder();
                            for (int i = 0; i < 4; i++) {
                                hex.append(next());
                            }
                            sb.append((char) Integer.parseInt(hex.toString(), 16));
                        }
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Object parseNumber() {
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos);
                if (c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E' || Character.isDigit(c)) {
                    sb.append(c);
                    pos++;
                } else {
                    break;
                }
            }
            String s = sb.toString();
            if (s.contains(".") || s.contains("e") || s.contains("E")) {
                return Double.parseDouble(s);
            }
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return Double.parseDouble(s);
            }
        }

        private Boolean parseBoolean() {
            if (peek() == 't') {
                expect("true");
                return Boolean.TRUE;
            }
            expect("false");
            return Boolean.FALSE;
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }

        private char peek() {
            return pos < src.length() ? src.charAt(pos) : '\0';
        }

        private char next() {
            return src.charAt(pos++);
        }

        private boolean consume(char expected) {
            if (peek() == expected) {
                pos++;
                return true;
            }
            return false;
        }

        private void expect(String literal) {
            for (int i = 0; i < literal.length(); i++) {
                if (next() != literal.charAt(i)) {
                    throw new IllegalStateException("JSON inválido: esperado '" + literal + "'");
                }
            }
        }
    }
}
