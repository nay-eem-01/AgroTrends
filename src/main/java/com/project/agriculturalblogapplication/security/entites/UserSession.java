package com.project.agriculturalblogapplication.security.entites;

import com.project.agriculturalblogapplication.constatnt.AppConstants;
import com.project.agriculturalblogapplication.constatnt.AppTables.UserSessionTable;
import com.project.agriculturalblogapplication.model.AuditModel;
import com.project.agriculturalblogapplication.security.enums.PlatformType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = UserSessionTable.TABLE_NAME)
public class UserSession extends AuditModel<String> {

    @Column(name = UserSessionTable.TOKEN, columnDefinition = AppConstants.DEFAULT_LARGE_TEXT_DATA_TYPE)
    private String token;

    @Column(name = UserSessionTable.TOKEN_TYPE)
    private String tokenType;

    @Column(name = UserSessionTable.ISSUES_AT)
    private OffsetDateTime issuesAt;

    @Column(name = UserSessionTable.EXPIRES_AT)
    private OffsetDateTime expiresAt;

    @Column(name = UserSessionTable.DEACTIVATED_AT)
    private OffsetDateTime deactivatedAt;

    @Column(name = UserSessionTable.SESSION_TOKEN)
    private String sessionToken;

    @Column(name = UserSessionTable.IS_SESSION_TOKEN_VALID)
    private Boolean isSessionTokenValid;

    @Enumerated(EnumType.STRING)
    @Column(name = UserSessionTable.PLATFORM_TYPE)
    private PlatformType platformType;

    @Column(name = UserSessionTable.IS_ACTIVE)
    private Boolean isActive;

    @Column(name = UserSessionTable.USER_DEVICE_ID)
    private Long userDeviceId;

    @Column(name = UserSessionTable.USER_TYPE)
    private String userType;

    @Column(name = UserSessionTable.USER_ID)
    private Long userId;
}
