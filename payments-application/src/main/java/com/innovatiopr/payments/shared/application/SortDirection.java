package com.innovatiopr.payments.shared.application;

public enum SortDirection {

    ASC, DESC;

    public static SortDirection parse(String raw, SortDirection fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return "asc".equalsIgnoreCase(raw.trim()) ? ASC : "desc".equalsIgnoreCase(raw.trim()) ? DESC : fallback;
    }

    public String sql() {
        return this == ASC ? "ASC" : "DESC";
    }
}
