package com.project.agriculturalblogapplication.constatnt;

public final class AppTables {



    public AppTables() {
    }

    public static final String USER_NAME = "users";

    public static final String ROLE_NAME = "role";

    public static final String USER_ROLE_NAME = "user_role";

    public static final String PRIVILEGE_NAME = "privilege";

    public static final String ROLE_PRIVILEGE_NAME = "role_privilege";


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

        public static final String STATUS = "status";

        public static final String PUBLISHED_AT = "published_at";

        public static final String SLUG = "slug";

        public static final String CLAP_COUNT = "clap_count";

        public static final String NAME = "blogs";
    }

    public static final class UserTable {

        public static final String USER_ID = "user_id";
        
        public static final String NAME = "user_name";

        public static final String EMAIL = "user_email";

        public static final String PASSWORD = "user_password";

        public static final String MOBILE_NUMBER = "mobile_number";

        public static final String GOOGLE_SUBJECT = "google_subject";

        public static final String EMAIL_VERIFIED_AT = "email_verified_at";

        public static final String BANNED_AT = "banned_at";

        public static final String BAN_REASON = "ban_reason";
    }

    public static final class FollowTable {

        public static final String AUTHOR_FOLLOWS = "author_follows";

        public static final String TAG_FOLLOWS = "tag_follows";

        public static final String USER_ID = "user_id";

        public static final String AUTHOR_ID = "author_id";

        public static final String TAG_ID = "tag_id";
    }

    public static final class BookmarkTable {

        public static final String NAME = "bookmarks";

        public static final String USER_ID = "user_id";

        public static final String BLOG_ID = "blog_id";
    }

    public static final class ClapTable {

        public static final String NAME = "claps";

        public static final String BLOG_ID = "blog_id";

        public static final String USER_ID = "user_id";

        public static final String CLAP_COUNT = "clap_count";
    }

    public static final class VoteTable {

        public static final String NAME = "votes";

        public static final String DIRECTION = "direction";

        public static final String SCORE = "score";
    }

    public static final class ReputationEventTable {

        public static final String NAME = "reputation_events";

        public static final String REASON = "reason";

        public static final String POINTS = "points";
    }

    public static final class ExpertApplicationTable {

        public static final String NAME = "expert_applications";

        public static final String EXPERT_APPLICATION_ID = "expert_application_id";

        public static final String TOPICS = "expert_application_topics";

        public static final String FIELD_TITLE = "field_title";

        public static final String CREDENTIAL_TYPE = "credential_type";

        public static final String INSTITUTION = "institution";

        public static final String PROOF_IMAGE_URLS = "proof_image_urls";

        public static final String LINKS = "links";

        public static final String STATUS = "status";

        public static final String REVIEW_NOTE = "review_note";
    }

    public static final class ExpertBadgeTable {

        public static final String NAME = "expert_badges";

        public static final String REVOKED_AT = "revoked_at";
    }

    public static final class NotificationTable {

        public static final String NAME = "notifications";

        public static final String RECIPIENT_ID = "recipient_id";

        public static final String ACTOR_ID = "actor_id";

        public static final String TYPE = "type";

        public static final String TARGET_TYPE = "target_type";

        public static final String TARGET_ID = "target_id";

        public static final String READ_AT = "read_at";
    }

    public static final class ReportTable {

        public static final String NAME = "reports";

        public static final String REPORTER_ID = "reporter_id";

        public static final String TARGET_TYPE = "target_type";

        public static final String TARGET_ID = "target_id";

        public static final String REASON = "reason";

        public static final String DETAILS = "details";

        public static final String SNAPSHOT = "snapshot";

        public static final String STATUS = "status";
    }

    public static final class TagTable {

        public static final String TAG_NAME = "tag_name";

        public static final String NAME = "tags";

        public static final String BLOG_TAGS = "blog_tags";

        public static final String BLOG_ID = "blog_id";

        public static final String TAG_ID = "tag_id";
    }

    public static final class CategoryTable {
        
        public static final String CATEGORY_ID = "category_id";
        
        public static final String CATEGORY_NAME = "category_name";

        public static final String NAME = "categories";
    }

    public static final class AuthorTable {

        public static final String AUTHOR_ID = "author_id";
        
        public static final String NAME = "authors";
        
        public static final String AUTHOR_NAME = "author_name";
        
        public static final String OCCUPATION = "occupation";
        
        public static final String DESIGNATION = "designation";
        
        public static final String WORK_PLACE_OR_INSTITUTION = "work_place_or_institution";

        public static final String BIO = "bio";

        public static final String PROFILE_IMAGE_URL = "profile_image_url";
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

        public static final String QUESTION_TAGS = "question_tags";

        public static final String IMAGE_URLS = "image_urls";

        public static final String ACCEPTED_ANSWER_ID = "accepted_answer_id";
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

    public static final class QuestionAiAnswerTable {

        public static final String NAME = "question_ai_answers";

        public static final String ANSWER = "answer";

        public static final String SOURCES = "sources";

        public static final String STATUS = "status";

        public static final String PROMPT_TOKENS = "prompt_tokens";

        public static final String COMPLETION_TOKENS = "completion_tokens";

        public static final String REVIEWED_BY = "reviewed_by";

        public static final String REVIEWED_AT = "reviewed_at";

        public static final String CORRECTION_ANSWER_ID = "correction_answer_id";
    }

    public static final class AiAnswerTable {

        private AiAnswerTable() {}

        public static final String TABLE_NAME =  "ai_answers";

        public static final String USER_ID = "user_id";

        public static final String QUESTION = "question";

        public static final String PROMPT_TOKENS = "prompt_tokens";

        public static final String COMPLETION_TOKENS = "completion_tokens";

        public static final String AI_ANSWER = "ai_answer";

        public static final String SOURCES = "sources";

        public static final String PUBLISHED_QUESTION_ID = "published_question_id";
    }
}
