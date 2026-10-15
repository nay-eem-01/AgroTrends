package com.project.agriculturalblogapplication.constatnt;

public final class ErrorCode {

    private ErrorCode() {}

    public static final String ERROR_UNAUTHORIZED_ACCESS = "Unauthorized access.";
    public static final String ERROR_USER_NOT_FOUND = "User not found.";
    public static final String ERROR_INVALID_EMAIL = "Invalid email.";
    public static final String ERROR_PASSWORD_IS_REQUIRED = "Password is required.";
    public static final String ERROR_INVALID_TOKEN = "Token is invalid!";
    public static final String ERROR_INCORRECT_PASSWORD = "Incorrect password.";
    public static final String ERROR_MOBILE_NUMBER_ALREADY_EXISTS = "User already exists with this mobile number. Please try with different mobile number.";

    public static final String ERROR_QUESTION_IS_REQUIRED = "Question is required.";
    public static final String ERROR_TOKEN_IS_REQUIRED = "Token is required.";

    public static final String ERROR_FIRST_NAME_IS_REQUIRED = "First name is required";

    public static final String ERROR_ROLE_NOT_FOUND = "Role not found.";
    public static final String ERROR_ROLE_ID_IS_REQUIRED = "Role ID is required.";
    public static final String ERROR_ROLE_NAME_IS_REQUIRED = "Role name is required.";
    public static final String ERROR_ROLE_TYPE_IS_REQUIRED = "Role type is required.";
    public static final String ERROR_ROLE_ALREADY_EXISTS = "Role already exists.";
    public static final String ERROR_PLEASE_SELECT_PRIVILEGES = "Please select privileges.";
    public static final String ERROR_USER_ALREADY_ASSIGNED_WITH_ROLE = "User already assigned with this role. Please delete those user to delete this role.";

    public static final String ERROR_COUNTRY_CODE_IS_REQUIRED = "Country code is required.";

    public static final String ERROR_PASSWORD_SHOULD_NOT_CONTAIN_WHITE_SPACE = "Password should not contain white spaces.";
    public static final String ERROR_PASSWORD_MUST_HAVE_MINIMUM_OF_8_CHARACTERS_AND_MAXIMUM_OF_16_CHARACTERS = "Password must have minimum of 8 characters and maximum of 16 characters long.";
    public static final String ERROR_PASSWORD_MUST_HAVE_A_DIGIT_A_SPECIAL_CHARACTER_AND_AN_ALPHABET = "Password must have a digit, a special character and a capital letter and a small letter.";

    public static final String ERROR_DESIGNATION_IS_REQUIRED = "Designation is required";
    public static final String ERROR_SPECIALITY_IS_REQUIRED = "Doctor's speciality is required";

    public static final String ERROR_OTP_TOKEN_IS_REQUIRED = "OTP token is required.";
    public static final String ERROR_MOBILE_NUMBER_IS_REQUIRED = "Mobile number is required.";
    public static final String ERROR_NAME_IS_REQUIRED = "Name is required.";
    public static final String ERROR_EMAIL_IS_REQUIRED = "Email is required.";

    public static final String ERROR_CURRENT_PASSWORD_IS_REQUIRED = "Current password is required.";
    public static final String ERROR_NEW_PASSWORD_IS_REQUIRED = "New password is required.";
    public static final String ERROR_OTP_CHANNEL_IS_REQUIRED = "OTP channel is required.";

    public static final String ERROR_USER_IS_REQUIRED = "User is required.";

    public static final String ERROR_USER_ALREADY_EXISTS_WITH_EMAIL = "User already exists with email.";

    public static final String ERROR_TITLE_IS_REQUIRED = "Blog title is required.";
    public static final String ERROR_CONTENT_IS_REQUIRED = "Blog content is required.";
    public static final String ERROR_CATEGORY_IS_REQUIRED = "Category is required.";
    public static final String ERROR_BLOG_ID_IS_REQUIRED = "Blog id is required.";

    public static final String ERROR_CATEGORY_NOT_FOUND = "Category not found";
    public static final String ERROR_CATEGORY_ALREADY_EXISTS = "Category already exists";
    public static final String ERROR_AUTHOR_NOT_FOUND = "Author not found";
    public static final String ERROR_BLOG_NOT_FOUND = "Blog not found";
    public static final String ERROR_COMMENT_BLOG_MISMATCH = "Comment Blog mismatch";
    public static final String ERROR_COMMENT_NOT_FOUND = "Comment not found";
    public static final String ERROR_QUESTION_NOT_FOUND = "Question not found";
    public static final String ERROR_ANSWER_NOT_FOUND = "Answer not found";
    public static final String ERROR_ANSWER_QUESTION_MISMATCH  = "Answer And Question mismatch";

    public static final String ERROR_INVALID_CREDENTIALS = "Invalid email or password.";
    public static final String ERROR_TOO_MANY_ATTEMPTS = "Too many attempts. Please try again later.";
    public static final String ERROR_FORBIDDEN = "You do not have permission to perform this action.";
    public static final String ERROR_INVALID_GOOGLE_TOKEN = "Google sign-in failed. Please try again.";
    public static final String ERROR_GOOGLE_EMAIL_UNVERIFIED = "Your Google account's e-mail is not verified.";
    public static final String ERROR_NO_PASSWORD_SET = "This account signs in with Google and has no password yet. Use 'Forgot password' to set one.";
    public static final String ERROR_GOOGLE_SIGN_IN_DISABLED = "Google sign-in is not available.";
    public static final String ERROR_TOO_MANY_REQUESTS_FROM_NETWORK = "Too many requests from your network today. Please try again later.";
    public static final String ERROR_EMAIL_NOT_VERIFIED = "Confirm your e-mail address first: open the link we sent you, or ask for a new one.";
    public static final String ERROR_INVALID_OR_EXPIRED_LINK = "This link is invalid or has expired.";
    public static final String ERROR_REFRESH_TOKEN_EXPIRED = "Refresh token has expired. Please sign in again.";
    public static final String ERROR_NEW_PASSWORD_SAME_AS_CURRENT = "New password must be different from the current password.";
    public static final String ERROR_QUESTION_TOO_LONG = "Question must be at most 1000 characters.";
    public static final String ERROR_AI_DAILY_LIMIT_REACHED = "You have reached today's limit of AI questions. Please try again tomorrow.";
    public static final String ERROR_AI_UNAVAILABLE = "The AI advisor is not available right now. Please try again later.";
    public static final String ERROR_NOT_AN_EXPERT = "Only Verified Experts can review AI answers.";
    public static final String ERROR_NOT_EXPERT_IN_TOPIC = "Only a Verified Expert in this question's topic can review its AI answer.";
    public static final String ERROR_CANNOT_FLAG_AI_ANSWER = "Only Verified Experts and Top Contributors in this topic can flag an AI answer.";
    public static final String ERROR_AI_ANSWER_ALREADY_REVIEWED = "This AI answer has already been reviewed.";
    public static final String ERROR_AI_ANSWER_NOT_REVIEWED = "This AI answer has not been reviewed yet.";
    public static final String ERROR_AI_ASK_ALREADY_PUBLISHED = "This AI answer has already been sent to the experts as a question.";
    public static final String ERROR_CORRECTION_REQUIRED = "Write your corrected answer.";
    public static final String ERROR_CORRECTION_TOO_LONG = "An answer can be at most 5000 characters.";
    public static final String ERROR_QUESTION_TITLE_TOO_LONG = "A title can be at most 150 characters.";
    public static final String ERROR_NOTIFICATION_NOT_FOUND = "Notification not found.";
    public static final String ERROR_REPORT_TARGET_REQUIRED = "Say what you are reporting.";
    public static final String ERROR_REPORT_REASON_REQUIRED = "Choose a reason for the report.";
    public static final String ERROR_REPORT_DETAILS_TOO_LONG = "Report details can be at most 500 characters.";
    public static final String ERROR_ALREADY_REPORTED = "You have already reported this.";
    public static final String ERROR_CANNOT_REPORT_YOURSELF = "You cannot report yourself.";
    public static final String ERROR_REPORT_NOT_FOUND = "Report not found.";
    public static final String ERROR_REPORT_ALREADY_CLOSED = "This report has already been handled.";
    public static final String ERROR_BAN_INSTEAD = "A user cannot be removed; ban the account instead.";
    public static final String ERROR_ACCOUNT_BANNED = "This account has been suspended by the moderators.";
    public static final String ERROR_CANNOT_BAN_ADMIN = "Admin accounts cannot be banned.";
    public static final String ERROR_BAN_REASON_REQUIRED = "Give a reason for the ban (at most 500 characters).";
    public static final String ERROR_INVALID_BLOG_STATUS = "A new post is either a DRAFT or PUBLISHED.";
    public static final String ERROR_UNDER_REVIEW = "This post is hidden while the moderators review reports about it.";
    public static final String ERROR_AI_ANSWER_NOT_FOUND = "This question has no AI answer.";
    public static final String ERROR_INVALID_SORT_FIELD = "This list cannot be sorted by that field.";
    public static final String ERROR_BLOG_ALREADY_PUBLISHED = "This blog is already published.";
    public static final String ERROR_BLOG_NOT_PUBLISHED = "This blog is not published.";
    public static final String ERROR_TOO_MANY_TAGS = "A post can have at most 5 tags.";
    public static final String ERROR_TAG_TOO_LONG = "A tag can be at most 40 characters.";
    public static final String ERROR_SEARCH_QUERY_REQUIRED = "Please enter something to search for.";
    public static final String ERROR_IMAGE_REQUIRED = "Please choose an image to upload.";
    public static final String ERROR_IMAGE_TOO_LARGE = "Images can be at most 5 MB.";
    public static final String ERROR_IMAGE_TYPE_NOT_ALLOWED = "Only JPEG, PNG and WebP images are allowed.";
    public static final String ERROR_IMAGE_NOT_UPLOADED = "Photos must be uploaded through /api/images first.";
    public static final String ERROR_TOO_MANY_PHOTOS = "A question can have at most 4 photos.";
    public static final String ERROR_CANNOT_CLAP_OWN_BLOG = "You cannot clap for your own blog.";
    public static final String ERROR_INVALID_CLAP_COUNT = "Clap between 1 and 50 times.";
    public static final String ERROR_CANNOT_VOTE_OWN_POST = "You cannot vote on your own post.";
    public static final String ERROR_DOWNVOTE_NEEDS_REPUTATION = "You need 50 reputation points to downvote.";
    public static final String ERROR_ONLY_ASKER_CAN_ACCEPT = "Only the person who asked can accept an answer.";
    public static final String ERROR_CANNOT_ACCEPT_REPLY = "Only answers, not replies, can be accepted.";
    public static final String ERROR_CANNOT_FOLLOW_YOURSELF = "You cannot follow yourself.";
    public static final String ERROR_TAG_NOT_FOUND = "Tag not found.";
    public static final String ERROR_BIO_TOO_LONG = "A bio can be at most 2000 characters.";
    public static final String ERROR_AGRI_FIELD_TOO_LONG = "Crop and region can be at most 60 characters.";
    public static final String ERROR_EXPERT_TOPICS_REQUIRED = "Choose the topics (1 to 3) you are an expert in.";
    public static final String ERROR_EXPERT_FIELD_TITLE_REQUIRED = "Tell us your field, e.g. Veterinarian or Agronomist.";
    public static final String ERROR_EXPERT_INSTITUTION_REQUIRED = "Tell us where you studied or work.";
    public static final String ERROR_EXPERT_CREDENTIAL_TYPE_REQUIRED = "Choose what kind of proof you are sending.";
    public static final String ERROR_EXPERT_TEXT_TOO_LONG = "Field and institution can be at most 150 characters.";
    public static final String ERROR_EXPERT_TOO_MANY_PROOFS = "Send at most 4 proof images and 3 links.";
    public static final String ERROR_EXPERT_INVALID_LINK = "Links must start with http:// or https://.";
    public static final String ERROR_EXPERT_PROOF_REQUIRED = "Add at least one proof image or link.";
    public static final String ERROR_EXPERT_APPLICATION_PENDING = "You already have an application waiting for review.";
    public static final String ERROR_EXPERT_APPLICATION_NOT_FOUND = "Expert application not found.";
    public static final String ERROR_EXPERT_APPLICATION_DECIDED = "This application has already been decided.";
    public static final String ERROR_EXPERT_TOPICS_NOT_REQUESTED = "Approve one or more of the topics the applicant asked for.";
    public static final String ERROR_EXPERT_REJECT_REASON_REQUIRED = "Tell the applicant why the application is rejected.";
    public static final String ERROR_EXPERT_NOTE_TOO_LONG = "A review note can be at most 500 characters.";
    public static final String ERROR_EXPERT_BADGE_NOT_FOUND = "This user is not a Verified Expert in that topic.";
    public static final String ERROR_INVALID_REQUEST = "Invalid request.";
    // Formatted with the parameter name (and, for enums, the allowed values) by ExceptionHandlingController.
    public static final String ERROR_INVALID_PARAMETER_VALUE = "Invalid value for '%s'.";
    public static final String ERROR_INVALID_PARAMETER_CHOICE = "Invalid value for '%s'. Use one of: %s.";
    public static final String ERROR_MISSING_PARAMETER = "Missing required parameter '%s'.";
}
