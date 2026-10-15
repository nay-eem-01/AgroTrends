package com.project.agriculturalblogapplication.model;

import jakarta.persistence.AttributeConverter;

/**
 * Stores an enum by name like {@code @Enumerated(STRING)} but without Hibernate's CHECK constraint, which ddl-auto
 * never widens: use it for enums that will gain values. Subclass it with one line per enum.
 */
public abstract class EnumNameConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected EnumNameConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E value) {
        return value == null ? null : value.name();
    }

    @Override
    public E convertToEntityAttribute(String name) {
        return name == null ? null : Enum.valueOf(type, name);
    }
}
