package com.project.agriculturalblogapplication.constatnt;

public final class AppTables {



    public AppTables() {
    }

    public static final String USER_NAME = "users";

    public static final String ROLE_NAME = "role";

    public static final String USER_ROLE_NAME = "user_role";

    public static final String PRIVILEGE_NAME = "privilege";

    public static final String ROLE_PRIVILEGE_NAME = "role_privilege";

    public static final String USER_TYPE = "user_type";

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
}
