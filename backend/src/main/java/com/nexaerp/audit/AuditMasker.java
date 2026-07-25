package com.nexaerp.audit;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Masks sensitive fields inside JSON strings before audit data is saved.
 * Compatible with Spring Boot 4 / Jackson 3 (tools.jackson).
 */
public final class AuditMasker {

    private static final String MASK = "***MASKED***";

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password",
            "passwordhash",
            "confirmpassword",
            "currentpassword",
            "oldpassword",
            "newpassword",
            "token",
            "accesstoken",
            "refreshtoken",
            "invitetoken",
            "verificationtoken",
            "emailverificationtoken",
            "resettoken",
            "passwordresettoken",
            "otp",
            "pin",
            "secret",
            "secretkey",
            "clientsecret",
            "apikey",
            "privatekey",
            "authorization",
            "bearer",
            "nid",
            "nationalid",
            "ssn"
    );

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private AuditMasker() {
    }

    public static String mask(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }

        try {
            JsonNode root = MAPPER.readTree(json);
            if (root == null) {
                return json;
            }

            maskNode(root);
            return MAPPER.writeValueAsString(root);
        } catch (Exception ignored) {
            // Plain audit text is allowed; only valid JSON is traversed.
            return json;
        }
    }

    private static void maskNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;

            for (Map.Entry<String, JsonNode> property : objectNode.properties()) {
                String fieldName = property.getKey();

                if (isSensitive(fieldName)) {
                    objectNode.put(fieldName, MASK);
                } else {
                    maskNode(property.getValue());
                }
            }
            return;
        }

        if (node.isArray()) {
            node.forEach(AuditMasker::maskNode);
        }
    }

    private static boolean isSensitive(String fieldName) {
        return fieldName != null
                && SENSITIVE_KEYS.contains(normalize(fieldName));
    }

    private static String normalize(String fieldName) {
        return fieldName
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
