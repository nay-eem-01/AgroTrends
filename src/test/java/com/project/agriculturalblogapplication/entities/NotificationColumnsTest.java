package com.project.agriculturalblogapplication.entities;

import com.project.agriculturalblogapplication.enums.NotificationTarget;
import com.project.agriculturalblogapplication.enums.NotificationType;
import jakarta.persistence.Convert;
import jakarta.persistence.Enumerated;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NotificationColumnsTest {

    @Test
    void typeColumnsAreConvertedSoHibernateAddsNoEnumCheckConstraint() throws Exception {
        // Regression: @Enumerated made Hibernate add a CHECK listing today's types, which ddl-auto never widens.
        for (String field : new String[]{"type", "targetType"}) {
            assertNull(Notification.class.getDeclaredField(field).getAnnotation(Enumerated.class), field);
            Convert convert = Notification.class.getDeclaredField(field).getAnnotation(Convert.class);
            assertEquals(field.equals("type") ? Notification.TypeConverter.class : Notification.TargetConverter.class,
                    convert.converter());
        }
    }

    @Test
    void convertersRoundTripTheNames() {
        Notification.TypeConverter types = new Notification.TypeConverter();
        Notification.TargetConverter targets = new Notification.TargetConverter();

        assertEquals(NotificationType.COMMENT_REPLY, types.convertToEntityAttribute(types.convertToDatabaseColumn(NotificationType.COMMENT_REPLY)));
        assertEquals("BLOG", targets.convertToDatabaseColumn(NotificationTarget.BLOG));
        assertNull(types.convertToEntityAttribute(null));
    }
}
