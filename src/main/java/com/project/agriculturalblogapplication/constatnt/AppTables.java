package com.project.agriculturalblogapplication.constatnt;

public final class AppTables {



    public AppTables() {
    }

    public static final String USER_NAME = "users";

    public static final String ROLE_NAME = "role";

    public static final String USER_ROLE_NAME = "user_role";

    public static final String PRIVILEGE_NAME = "privilege";

    public static final String ROLE_PRIVILEGE_NAME = "role_privilege";

    public static final String USER_TYPES = "user_types";

    public static final class AuditModelTable {

        public static final String ID = "id";

        public static final String CREATED_BY = "created_by";

        public static final String CREATION_DATE = "creation_date";

        public static final String LAST_MODIFIED_BY = "last_modified_by";

        public static final String LAST_MODIFIED_DATE = "last_modified_date";
    }

    public static final class BlogTable {

        public static final String TITLE = "title";

        public static final String CONTENT = "content";

        public static final String IMAGE_URL = "image_url";

        public static final String NAME = "blogs";
    }

    public static final class UserTable {

        public static final String USER_ID = "user_id";
        
        public static final String NAME = "user_name";

        public static final String EMAIL = "user_email";

        public static final String PASSWORD = "user_password";

        public static final String MOBILE_NUMBER ="mobile_number";

    }

    public static final class CategoryTable {
        
        public static final String CATEGORY_ID = "category_id";
        
        public static final String CATEGORY_NAME = "category_name";

        public static final String NAME = "categories";
    }

    public static final class AuthorTable {

        public static final String AUTHOR_ID = "author_id";
        
        public static final String NAME = "name";
        
        public static final String AUTHOR_NAME = "author_name";
        
        public static final String OCCUPATION = "occupation";
        
        public static final String DESIGNATION = "designation";
        
        public static final String WORK_PLACE_OR_INSTITUTION = "work_place_or_institution";
    }

    public static final class RoleTable {

        public static final String ROLE_ID = "role_id";

        public static final String ROLE_TYPE = "role_type";

        public static final String ROLE_NAME = "role_name";
    }

    public static final class PrivilegeTable {

        public static final String PRIVILEGE_ID = "privilege_id";

        public static final String NAME = "name";

        public static final String DESC_NAME = "desc_name";
    }

    public static final class AnswerTable {

        public static final String ANSWER_ID = "answer_id";

        public static final String NAME = "answers";

        public static final String CONTENT = "content";

    }

    public static final class QuestionTable {

        public static final String QUESTION_ID = "question_id";

        public static final String NAME = "questions";

        public static final String CONTENT = "content";

        public static final String TITLE = "title";
    }

    public static final class RefreshTokenTable {

        private RefreshTokenTable() {}

        public static final String TABLE_NAME =  "refresh_tokens";

        public static final String USER_ID = "user_id";

        public static final String TOKEN = "token";

        public static final String EXPIRY_TIME = "image_url";
    }

    public static final class UserSessionTable {

        private UserSessionTable() {}

        public static final String TABLE_NAME = "user_sessions";

        public static final String TOKEN = "token";

        public static final String TOKEN_TYPE = "token_type";

        public static final String SESSION_TOKEN = "session_token";

        public static final String IS_SESSION_TOKEN_VALID = "is_session_token_valid";

        public static final String ISSUES_AT = "issues_at";

        public static final String EXPIRES_AT = "expires_at";

        public static final String DEACTIVATED_AT = "deactivated_at";

        public static final String PLATFORM_TYPE = "platform_type";

        public static final String IS_ACTIVE = "is_active";

        public static final String USER_DEVICE_ID = "user_device_id";

        public static final String USER_TYPE = "user_type";

        public static final String USER_ID = "user_id";
    }

    public static final class LocalizedTextTable {
        private LocalizedTextTable() {}

        public static final String TABLE_NAME = "localized_texts";

        public static final String ORIGINAL_TEXT = "original_text";

        public static final String ORIGINAL_LANGUAGE_CODE = "original_language_code";
    }

    public static final class TranslationTable {
        private TranslationTable() {}

        public static final String TABLE_NAME = "translations";

        public static final String LANGUAGE_ID = "language_id";

        public static final String LANGUAGE_CODE = "language_code";

        public static final String TRANSLATED_TEXT = "translated_text";

        public static final String LOCALIZED_TEXT_ID = "localized_text_id";
    }


}
