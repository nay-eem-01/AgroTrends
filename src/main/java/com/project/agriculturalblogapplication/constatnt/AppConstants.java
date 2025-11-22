package com.project.agriculturalblogapplication.constatnt;

import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.HashMap;

@Configuration
public class AppConstants {
    public static final String PAGE_NO = "pageNo";
    public static final String PAGE_SIZE = "pageSize";
    public static final String SORT_BY = "sortBy";
    public static final String SORT_ORDER = "sortOrder";

    public static String INITIAL_USERNAME = "admin@gmail.com";
    public static String INITIAL_PASSWORD = "123456";

    public static String INITIAL_ROLE = "SUPER ADMIN";
    public static String USER_ROLE = "USER";

    public static String consumerPermission = "USER";
    public static String consumerPermissionDesc = "User Generalized Permission";

    public static String JWT_TOKEN_TYPE = "Bearer";

    public static BigDecimal DefaultBalance = BigDecimal.valueOf(0.0);


    public static HashMap<String, String> PERMISSIONS = new HashMap<>() {
        {
            put("GENERAL", "GENERAL CONSUMER");

            put("USER_CREATE", "USER CREATE");
            put("USER_READ", "USER READ");
            put("USER_UPDATE", "USER UPDATE");
            put("USER_DELETE", "USER DELETE");

            put("ROLE_CREATE", "ROLE CREATE");
            put("ROLE_READ", "ROLE READ");
            put("ROLE_UPDATE", "ROLE UPDATE");
            put("ROLE_DELETE", "ROLE DELETE");

            put("NOTIFICATION_SEND", "NOTIFICATION SEND");
            put("USER_UPDATE_FROM_ADMIN", "USER UPDATE FROM ADMIN");
        }
    };
}
