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
    public static final String ASC_OR_DESC = "ascOrDesc";

    public static final String DEFAULT_PAGE_NO = "0";
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final String ASC_OR_DESC_VALUE = "asc";
    public static final String SORT_BY_VALUE = "createdDate";
    public static final String PARAMETERS = "parameters";
    public static final String LANG = "lang";

    public static String INITIAL_USERNAME = "admin@gmail.com";
    public static String INITIAL_MOBILE_NUMBER = "+01833849973";
    public static String INITIAL_PASSWORD = "123456";

    public static String INITIAL_ROLE = "SUPER ADMIN";
    public static String USER_ROLE = "USER";

    public static String CONSUMER_PERMISSIONS = "USER";
    public static String CONSUMER_PERMISSIONS_DESC = "User Generalized Permission";

    public static String JWT_TOKEN_TYPE = "Bearer";

    public static final String DEFAULT_LANGUAGE_NAME = "English";
    public static final String DEFAULT_LANGUAGE_CODE = "en";
    public static final String BANGLA_LANGUAGE_NAME = "Bangla";
    public static final String BANGLA_LANGUAGE_CODE = "bn";
    public static final String LOAD_ORIGINAL_TEXT = "loadOriginalText";
    public static final String DEFAULT_LARGE_TEXT_DATA_TYPE = "TEXT";


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

            put("CATEGORY_CREATE", "CATEGORY CREATE");
            put("CATEGORY_READ", "CATEGORY READ");
            put("CATEGORY_UPDATE", "CATEGORY UPDATE");
            put("CATEGORY_DELETE", "CATEGORY DELETE");

            put("NOTIFICATION_SEND", "NOTIFICATION SEND");
            put("USER_UPDATE_FROM_ADMIN", "USER UPDATE FROM ADMIN");
        }
    };
}
