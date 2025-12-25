package com.converter.api;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CodeConvertService {

    /* =========================
       MODELS
       ========================= */

    static class PyFunction {
        String name;
        List<String> params = new ArrayList<>();
        List<String> body = new ArrayList<>();
    }

    static class Error {
        String msg;
        Error(String m) {
            msg = m;
        }
    }

    /* =========================
       ENTRY POINT
       ========================= */

    public String convert(String src, String tgt, String code) {

        if (!src.equalsIgnoreCase("python") || !tgt.equalsIgnoreCase("java"))
            return "// Unsupported conversion";

        String[] lines = code.split("\n");

        Map<String, PyFunction> functions = new LinkedHashMap<>();
        List<String> mainLines = new ArrayList<>();
        List<Error> errors = new ArrayList<>();

        /* =========================
           PASS 1 – COLLECT FUNCTIONS
           ========================= */

        PyFunction current = null;
        int funcIndent = 0;

        for (String raw : lines) {
            if (raw.trim().isEmpty()) continue;

            int indent = indent(raw);
            String line = raw.trim();

            if (line.startsWith("def ")) {
                current = new PyFunction();
                current.name = line.substring(4, line.indexOf("("));

                String params = line.substring(
                        line.indexOf("(") + 1,
                        line.indexOf(")")
                );

                if (!params.isBlank())
                    for (String p : params.split(","))
                        current.params.add(p.trim());

                functions.put(current.name, current);
                funcIndent = indent;
                continue;
            }

            if (current != null && indent > funcIndent) {
                current.body.add(raw);
            } else {
                current = null;
                mainLines.add(raw);
            }

        }

        /* =========================
           PASS 2 – GENERATE JAVA
           ========================= */

        StringBuilder out = new StringBuilder();
        out.append("public class Main {\n");

        for (PyFunction f : functions.values())
            out.append(genFunction(f, errors));

        out.append("    public static void main(String[] args) {\n");
        genBlock(out, mainLines, new Stack<>(), errors, null);
        out.append("    }\n");
        out.append("}\n");

        if (!errors.isEmpty()) {
            StringBuilder e = new StringBuilder("❌ Errors:\n");
            for (Error er : errors) e.append(er.msg).append("\n");
            return e.toString();
        }

        return out.toString();
    }

    /* =========================
       FUNCTION GENERATION
       ========================= */

    private String genFunction(PyFunction f, List<Error> errors) {

        StringBuilder sb = new StringBuilder();
        List<String> returnTypes = new ArrayList<>();

        sb.append("    static ");
        int retPos = sb.length();
        sb.append("__RET__ ").append(f.name).append("(");

        Stack<Map<String, String>> scope = new Stack<>();
        scope.push(new HashMap<>());

        for (int i = 0; i < f.params.size(); i++) {
            sb.append("int ").append(f.params.get(i));
            scope.peek().put(f.params.get(i), "int");
            if (i < f.params.size() - 1) sb.append(", ");
        }

        sb.append(") {\n");
        genBlock(sb, f.body, scope, errors, returnTypes);
        sb.append("    }\n\n");

        sb.replace(retPos, retPos + "__RET__".length(),
                resolveReturn(returnTypes));

        return sb.toString();
    }

    /* =========================
       BLOCK GENERATION
       ========================= */

    private void genBlock(StringBuilder sb,
                          List<String> lines,
                          Stack<Map<String, String>> scope,
                          List<Error> errors,
                          List<String> returnTypes) {

        Stack<Integer> indentStack = new Stack<>();
        indentStack.push(0);

        scope.push(new HashMap<>());

        for (String raw : lines) {

            if (raw.trim().isEmpty()) continue;

            int currIndent = indent(raw);
            String line = raw.trim();

            boolean isElseLike = line.startsWith("elif ") || line.equals("else:");

            if (!isElseLike) {
                while (indentStack.size() > 1 && currIndent < indentStack.peek()) {
                    sb.append("        }\n");
                    indentStack.pop();
                }
            }

            /* -------- IF -------- */
            if (line.startsWith("if ")) {
                sb.append("        if (")
                        .append(expr(line.substring(3, line.length() - 1)))
                        .append(") {\n");
                indentStack.push(currIndent + 4);
                continue;
            }

            /* -------- ELIF -------- */
            if (line.startsWith("elif ")) {
                // close previous if block
                sb.append("        }\n        else if (")
                        .append(expr(line.substring(5, line.length() - 1)))
                        .append(") {\n");
                continue;   // ❗ NO indentStack.push
            }

            /* -------- ELSE -------- */
            if (line.equals("else:")) {
                sb.append("         }\n        else {\n");
                continue;   // ❗ NO indentStack.push
            }


            /* -------- WHILE -------- */
            if (line.startsWith("while ")) {
                sb.append("        while (")
                        .append(expr(line.substring(6, line.length() - 1)))
                        .append(") {\n");
                indentStack.push(currIndent + 4);
                continue;
            }

            /* -------- FOR RANGE -------- */
            if (line.startsWith("for ")) {

                String[] p = line.split("\\bin\\s*range");
                String var = p[0].replace("for", "").trim();

                String inside = p[1].trim();
                inside = inside.substring(1, inside.length() - 2);
                String[] args = inside.split(",");

                String start = args.length > 1 ? args[0].trim() : "0";
                String end   = args.length > 1 ? args[1].trim() : args[0].trim();
                String step  = args.length == 3 ? args[2].trim() : "1";

                sb.append("        for (int ").append(var)
                        .append(" = ").append(start)
                        .append("; ").append(var).append(" < ").append(end)
                        .append("; ").append(var).append(" += ").append(step)
                        .append(") {\n");

                scope.peek().put(var, "int");
                indentStack.push(currIndent + 4);
                continue;
            }

            /* -------- RETURN -------- */
            if (line.startsWith("return")) {
                String e = expr(line.replace("return", "").trim());
                if (returnTypes != null) returnTypes.add(typeOf(e, scope));
                sb.append("        return ").append(e).append(";\n");
                continue;
            }

            /* -------- PRINT -------- */
            if (line.startsWith("print(")) {
                sb.append("        System.out.println(")
                        .append(expr(line.substring(6, line.length() - 1)))
                        .append(");\n");
                continue;
            }

            /* -------- UNSUPPORTED TYPES -------- */
            if (line.contains("=") && line.contains("[") && line.contains("]")) {
                errors.add(new Error("List data type is not supported"));
                continue;
            }

            if (line.contains("=") && line.contains("{") && line.contains("}")) {
                errors.add(new Error("Dictionary data type is not supported"));
                continue;
            }

            if (line.contains("=") && line.contains("(") && line.contains(")")) {
                errors.add(new Error("Tuple data type is not supported"));
                continue;
            }

            /* -------- ASSIGNMENT -------- */
            if (line.contains("=")) {
                String[] a = line.split("=", 2);
                String v = a[0].trim();
                String e = expr(a[1].trim());
                String t = typeOf(e, scope);

                if (!scope.peek().containsKey(v)) {
                    scope.peek().put(v, t);
                    sb.append("        ").append(t)
                            .append(" ").append(v).append(" = ").append(e).append(";\n");
                } else {
                    sb.append("        ").append(v)
                            .append(" = ").append(e).append(";\n");
                }
                continue;
            }

            /* -------- FUNCTION CALL -------- */
            if (line.matches("[a-zA-Z_][a-zA-Z0-9_]*\\(.*\\)")) {
                sb.append("        ").append(line).append(";\n");
            }
        }

        while (indentStack.size() > 1) {
            sb.append("        }\n");
            indentStack.pop();
        }

        scope.pop();
    }

    /* =========================
       HELPERS
       ========================= */

    private String expr(String e) {

        StringBuilder out = new StringBuilder();
        boolean inString = false;

        for (int i = 0; i < e.length(); i++) {
            char c = e.charAt(i);

            if (c == '"') {
                inString = !inString;
                out.append(c);
                continue;
            }

            if (!inString) {

                if (e.startsWith("and", i) &&
                        (i == 0 || !Character.isLetterOrDigit(e.charAt(i - 1))) &&
                        (i + 3 == e.length() || !Character.isLetterOrDigit(e.charAt(i + 3)))) {
                    out.append("&&");
                    i += 2;
                    continue;
                }

                if (e.startsWith("or", i) &&
                        (i == 0 || !Character.isLetterOrDigit(e.charAt(i - 1))) &&
                        (i + 2 == e.length() || !Character.isLetterOrDigit(e.charAt(i + 2)))) {
                    out.append("||");
                    i += 1;
                    continue;
                }

                if (e.startsWith("not", i) &&
                        (i == 0 || !Character.isLetterOrDigit(e.charAt(i - 1))) &&
                        (i + 3 == e.length() || !Character.isLetterOrDigit(e.charAt(i + 3)))) {
                    out.append("!");
                    i += 2;
                    continue;
                }

                if (e.startsWith("True", i)) {
                    out.append("true");
                    i += 3;
                    continue;
                }

                if (e.startsWith("False", i)) {
                    out.append("false");
                    i += 4;
                    continue;
                }
            }

            out.append(c);
        }

        return out.toString();
    }

    private String typeOf(String e, Stack<Map<String, String>> scope) {
        if (e.matches("-?\\d+")) return "int";
        if (e.matches("-?\\d+\\.\\d+")) return "double";
        if (e.startsWith("\"")) return "String";
        if (e.equals("true") || e.equals("false")) return "boolean";

        for (int i = scope.size() - 1; i >= 0; i--)
            if (scope.get(i).containsKey(e))
                return scope.get(i).get(e);

        return "int";
    }

    private String resolveReturn(List<String> types) {
        if (types.contains("String")) return "String";
        if (types.contains("double")) return "double";
        if (types.contains("boolean")) return "boolean";
        if (types.contains("int")) return "int";
        return "void";
    }

    private int indent(String s) {
        int c = 0;
        for (char ch : s.toCharArray()) {
            if (ch == ' ') c++;
            else break;
        }
        return c;
    }
}