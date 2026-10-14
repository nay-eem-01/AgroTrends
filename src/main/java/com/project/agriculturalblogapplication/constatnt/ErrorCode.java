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
    public static final String ERROR_CATEGORY_IS_REQUIRED = "Blog category is required.";
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
    public static final String ERROR_EMAIL_NOT_VERIFIED = "Confirm your e-mail address first: open the link we sent you, or ask for a new one.";
    public static final String ERROR_INVALID_OR_EXPIRED_LINK = "This link is invalid or has expired.";
    public static final String ERROR_REFRESH_TOKEN_EXPIRED = "Refresh token has expired. Please sign in again.";
    public static final String ERROR_NEW_PASSWORD_SAME_AS_CURRENT = "New password must be different from the current password.";
    public static final String ERROR_QUESTION_TOO_LONG = "Question must be at most 1000 characters.";
    public static final String ERROR_AI_DAILY_LIMIT_REACHED = "You have reached today's limit of AI questions. Please try again tomorrow.";
    public static final String ERROR_AI_UNAVAILABLE = "The AI advisor is not available right now. Please try again later.";
    public static final String ERROR_QUESTION_ALREADY_ANSWERED = "This question already has answers; AI drafts are only for unanswered questions.";
    public static final String ERROR_INVALID_SORT_FIELD = "This list cannot be sorted by that field.";
    public static final String ERROR_BLOG_ALREADY_PUBLISHED = "This blog is already published.";
    public static final String ERROR_BLOG_NOT_PUBLISHED = "This blog is not published.";
    public static final String ERROR_TOO_MANY_TAGS = "A blog can have at most 5 tags.";
    public static final String ERROR_TAG_TOO_LONG = "A tag can be at most 40 characters.";
    public static final String ERROR_SEARCH_QUERY_REQUIRED = "Please enter something to search for.";
    public static final String ERROR_IMAGE_REQUIRED = "Please choose an image to upload.";
    public static final String ERROR_IMAGE_TOO_LARGE = "Images can be at most 5 MB.";
    public static final String ERROR_IMAGE_TYPE_NOT_ALLOWED = "Only JPEG, PNG and WebP images are allowed.";
    public static final String ERROR_CANNOT_CLAP_OWN_BLOG = "You cannot clap for your own blog.";
    public static final String ERROR_INVALID_CLAP_COUNT = "Clap between 1 and 50 times.";
    public static final String ERROR_CANNOT_FOLLOW_YOURSELF = "You cannot follow yourself.";
    public static final String ERROR_TAG_NOT_FOUND = "Tag not found.";
    public static final String ERROR_BIO_TOO_LONG = "A bio can be at most 2000 characters.";
    public static final String ERROR_AGRI_FIELD_TOO_LONG = "Crop and region can be at most 60 characters.";
    public static final String ERROR_INVALID_REQUEST = "Invalid request.";
    // Formatted with the parameter name (and, for enums, the allowed values) by ExceptionHandlingController.
    public static final String ERROR_INVALID_PARAMETER_VALUE = "Invalid value for '%s'.";
    public static final String ERROR_INVALID_PARAMETER_CHOICE = "Invalid value for '%s'. Use one of: %s.";
    public static final String ERROR_MISSING_PARAMETER = "Missing required parameter '%s'.";
}
