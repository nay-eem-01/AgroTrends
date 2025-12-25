package com.project.agriculturalblogapplication.constatnt;

public final class ErrorCode {

    private ErrorCode() {}

    public static final String ERROR_ERROR_CODE_NOT_FOUND = "ERROR_ERROR_CODE_NOT_FOUND";
    public static final String ERROR_ERROR_CODE_ALREADY_EXISTS = "ERROR_ERROR_CODE_ALREADY_EXISTS";

    public static final String ERROR_AUTHENTICATION_FAILED = "Authentication failed.";
    public static final String ERROR_UNAUTHORIZED_ACCESS = "Unauthorized access.";
    public static final String ERROR_USER_NOT_FOUND = "User not found.";
    public static final String ERROR_USER_IS_BANNED_BY_THE_SYSTEM = "User banned by the system.";
    public static final String ERROR_USER_IS_NOT_VERIFIED = "User is not verified yet.";
    public static final String ERROR_INVALID_EMAIL = "Invalid email.";
    public static final String ERROR_PASSWORD_IS_REQUIRED = "Password is required.";
    public static final String ERROR_INVALID_TOKEN = "Token is invalid!";
    public static final String ERROR_INCORRECT_PASSWORD = "Incorrect password.";
    public static final String ERROR_INCORRECT_VERIFICATION_CODE = "Incorrect verification code.";
    public static final String ERROR_USER_STATUS_IS_REQUIRED = "User status is required.";
    public static final String ERROR_MOBILE_NUMBER_ALREADY_EXISTS = "User already exists with this mobile number. Please try with different mobile number.";

    public static final String ERROR_SECRET_IS_REQUIRED = "Secret is required.";
    public static final String ERROR_CODE_IS_REQUIRED = "Code is required.";
    public static final String ERROR_SECRET_NOT_FOUND = "Secret not found.";
    public static final String ERROR_SECURITY_QUESTION_NOT_FOUND = "Security question not found.";
    public static final String ERROR_SECURITY_QUESTION_ALREADY_EXISTS = "Security question already exists.";
    public static final String ERROR_SECURITY_CODE_NOT_FOUND = "Security code not found.";
    public static final String ERROR_SECURITY_CODE_ALREADY_EXISTS = "Security code already exists.";
    public static final String ERROR_RECOVERY_METHOD_IS_REQUIRED = "Recovery method is required.";
    public static final String ERROR_QUESTION_IS_REQUIRED = "Question is required.";
    public static final String ERROR_ANSWER_IS_REQUIRED = "Answer is required.";
    public static final String ERROR_SECURITY_CODE_IS_REQUIRED = "Security code is required.";
    public static final String ERROR_TOKEN_IS_REQUIRED = "Token is required.";

    public static final String ERROR_SIGN_IN_ATTEMPT_NOT_FOUND = "Sign-in attempt not found.";

    public static final String ERROR_USER_DEVICE_NOT_FOUND = "User device not found.";
    public static final String ERROR_THERE_IS_NO_USER_DEVICES_TO_EXPORT = "There is no user devices to export.";

    public static final String ERROR_NOTIFICATION_NOT_FOUND = "Notification not found.";

    public static final String ERROR_EMAIL_TEMPLATE_NOT_FOUND = "Email template not found.";
    public static final String ERROR_EMAIL_TEMPLATE_ALREADY_EXISTS = "Email template already exists.";

    public static final String ERROR_ACCOUNT_RECOVERY_REQUEST_ID_IS_REQUIRED = "Account recovery request ID is required.";
    public static final String ERROR_ACCOUNT_RECOVERY_REQUEST_STATUS_IS_REQUIRED = "Account recovery request status is required.";
    public static final String ERROR_NO_USER_FOUND = "Sorry! No user found with the given email.";
    public static final String ERROR_ACCOUNT_RECOVERY_REQUEST_NOT_FOUND = "Account recovery request not found.";
    public static final String ERROR_ACCOUNT_RECOVERY_REQUEST_ALREADY_EXISTS = "Your account recovery request is already in progress. Please wait for the response.";
    public static final String ERROR_FIRST_NAME_IS_REQUIRED = "First name is required";
    public static final String ERROR_LAST_NAME_IS_REQUIRED = "Last name is required.";
    public static final String ERROR_DATE_OF_BIRTH_IS_REQUIRED = "Date of birth is required.";
    public static final String ERROR_PLACE_OF_BIRTH_IS_REQUIRED = "Place of birth is required.";
    public static final String ERROR_OCCUPATION_IS_REQUIRED = "Occupation is required.";
    public static final String ERROR_SELF_PEP_DECLARATION_IS_REQUIRED = "Self pep declaration is required.";
    public static final String ERROR_EMAIL_ALREADY_REGISTERED = "Email already registered. Please try with another email.";

    public static final String ERROR_ROLE_NOT_FOUND = "Role not found.";
    public static final String ERROR_ROLE_ID_IS_REQUIRED = "Role ID is required.";
    public static final String ERROR_INVALID_ROLE_TYPE = "Invalid role type.";
    public static final String ERROR_ROLE_NAME_IS_REQUIRED = "Role name is required.";
    public static final String ERROR_ROLE_TYPE_IS_REQUIRED = "Role type is required.";
    public static final String ERROR_ROLE_ALREADY_EXISTS = "Role already exists.";
    public static final String ERROR_PLEASE_SELECT_PRIVILEGES = "Please select privileges.";
    public static final String ERROR_USER_ALREADY_ASSIGNED_WITH_ROLE = "User already assigned with this role. Please delete those user to delete this role.";

    public static final String ERROR_SOURCE_OF_FUNDS_IS_REQUIRED = "Source of funds is required.";
    public static final String ERROR_PURPOSE_OF_ACCOUNT_IS_REQUIRED = "Purpose of account is required.";
    public static final String ERROR_YEARLY_EXPECTED_OUTGOING_TRANSACTION_VOLUME_IS_REQUIRED = "Yearly expected outgoing transaction volume is required.";
    public static final String ERROR_YEARLY_EXPECTED_INCOMING_TRANSACTION_VOLUME_IS_REQUIRED = "Yearly expected incoming transaction volume is required.";

    public static final String ERROR_ADDRESS_LINE_1_IS_REQUIRED = "Address-line 1 is required.";
    public static final String ERROR_ADDRESS_LINE_1_CAN_CONTAIN_AT_MOST_45_CHARACTERS = "Address-line 1 can contain at most 45 characters.";
    public static final String ERROR_ADDRESS_LINE_2_IS_REQUIRED = "Address-line 2 is required.";
    public static final String ERROR_ADDRESS_LINE_2_CAN_CONTAIN_AT_MOST_45_CHARACTERS = "Address-line 2 can contain at most 45 characters.";
    public static final String ERROR_CITY_IS_REQUIRED = "City is required.";
    public static final String ERROR_STATE_IS_REQUIRED = "State is required.";
    public static final String ERROR_CITY_CAN_CONTAIN_AT_MOST_40_CHARACTERS = "City can contain at most 40 characters.";
    public static final String ERROR_POSTAL_CODE_IS_REQUIRED = "Postal code is required.";
    public static final String ERROR_POSTAL_CODE_CAN_CONTAIN_AT_MOST_20_CHARACTERS = "Postal code can contain at most 20 characters.";

    public static final String ERROR_COUNTRY_IS_REQUIRED = "Country is required.";
    public static final String ERROR_COUNTRY_CODE_IS_REQUIRED = "Country code is required.";
    public static final String ERROR_COUNTRY_CODE_MUST_BE_IN_ISO2_FORMAT = "Country code must be in ISO-2 format. For example BE for Belgium.";

    public static final String ERROR_PASSWORD_SHOULD_NOT_CONTAIN_WHITE_SPACE = "Password should not contain white spaces.";
    public static final String ERROR_PASSWORD_MUST_HAVE_MINIMUM_OF_8_CHARACTERS_AND_MAXIMUM_OF_16_CHARACTERS = "Password must have minimum of 8 characters and maximum of 16 characters long.";
    public static final String ERROR_PASSWORD_MUST_HAVE_A_DIGIT_A_SPECIAL_CHARACTER_AND_AN_ALPHABET = "Password must have a digit, a special character and a capital letter and a small letter.";

    public static final String ERROR_GENDER_IS_REQUIRED = "Gender is required";

    public static final String ERROR_DESIGNATION_IS_REQUIRED = "Designation is required";
    public static final String ERROR_SPECIALITY_IS_REQUIRED = "Doctor's speciality is required";
    public static final String ERROR_ONMS_REGISTRATION_NUMBER_IS_REQUIRED = "ONMS registration number is required";

    public static final String ERROR_THERE_IS_NO_USERS_TO_EXPORT = "There is no users to export.";

    public static final String ERROR_OTP_IS_REQUIRED = "OTP is required.";
    public static final String ERROR_OTP_TOKEN_IS_REQUIRED = "OTP token is required.";
    public static final String ERROR_OTP_NOT_FOUND = "OTP not found.";
    public static final String ERROR_INVALID_OTP_CHANNEL = "Invalid OTP channel.";
    public static final String ERROR_MOBILE_NUMBER_IS_REQUIRED = "Mobile number is required.";
    public static final String ERROR_CONTACT_EMAIL_IS_REQUIRED = "Contact email is required.";
    public static final String ERROR_MESSAGE_IS_REQUIRED = "Message is required.";
    public static final String ERROR_NAME_IS_REQUIRED = "Name is required.";
    public static final String ERROR_ACCOUNT_CATEGORY_IS_REQUIRED = "Account category is required.";
    public static final String ERROR_EMAIL_IS_REQUIRED = "Email is required.";
    public static final String ERROR_RECIPIENT_EMAIL_IS_REQUIRED = "Recipient email is required..";
    public static final String ERROR_OTP_IS_ALREADY_USED = "OTP is already used.";
    public static final String ERROR_OTP_IS_EXPIRED = "OTP is expired.";
    public static final String ERROR_INCORRECT_OTP = "Incorrect OTP.";
    public static final String ERROR_MAXIMUM_OTP_ATTEMPTS_EXCEED = "Maximum OTP attempts exceed.";
    public static final String ERROR_MOBILE_NUMBER_DOES_NOT_EXIST = "Mobile number does not exist.";
    public static final String ERROR_EMAIL_DOES_NOT_EXIST = "Email does not exist.";
    public static final String ERROR_FAILED_TO_SEND_MOBILE_NUMBER_VERIFICATION_CODE = "Failed to send mobile number verification code.";

    public static final String ERROR_CURRENT_PASSWORD_IS_REQUIRED = "Current password is required.";
    public static final String ERROR_NEW_PASSWORD_IS_REQUIRED = "New password is required.";
    public static final String ERROR_OTP_CHANNEL_IS_REQUIRED = "OTP channel is required.";
    public static final String ERROR_INCORRECT_CURRENT_PASSWORD = "Incorrect current password.";
    public static final String ERROR_NEW_PASSWORD_AND_CURRENT_PASSWORD_CAN_NOT_BE_SAME = "New password and current password can not be same.";

    public static final String ERROR_INVALID_API_KEY_OR_HMAC_KEY = "Invalid API key or HMAC key.";

    public static final String ERROR_PAGE_NUMBER_IS_REQUIRED = "Page number is required.";
    public static final String ERROR_PAGE_LIMIT_IS_REQUIRED = "Page limit is required.";
    public static final String ERROR_OFFSET_IS_REQUIRED = "Offset is required.";
    public static final String ERROR_VERIFICATION_CODE_IS_REQUIRED = "Verification code is required.";

    public static final String ERROR_SESSION_TOKEN_IS_REQUIRED = "Session token is required.";
    public static final String ERROR_INVALID_SESSION_TOKEN = "Session token is invalid.";
    public static final String ERROR_USER_ID_IS_REQUIRED = "User ID is required.";

    public static final String ERROR_PROFILE_CREATION_FAILED = "Profile creation failed.";
    public static final String ERROR_USER_ALREADY_EXISTS_WITH_EMAIL = "User already exists with email.";
    public static final String ERROR_DOCTOR_ALREADY_EXIST = "Doctor already exists with ONMS Registration number.";
    public static final String ERROR_DOCTOR_NOT_FOUND = "Doctor not found";
    public static final String ERROR_PATIENT_NOT_FOUND = "Patient not found";
    public static final String ERROR_PROFILE_NOT_FOUND = "Profile not found.";
    public static final String ERROR_EMAIL_VERIFICATION_FAILED = "Email verification failed.";
    public static final String ERROR_MOBILE_NUMBER_VERIFICATION_FAILED = "Mobile number verification failed.";
    public static final String ERROR_MOBILE_NUMBER_ALREADY_VERIFIED = "Mobile number is already verified.";
    public static final String ERROR_EXCEEDED_VERIFICATION_ATTEMPTS = "Maximum verification attempts exceeded.";
    public static final String ERROR_COULD_NOT_RESEND_VERIFICATION_CODE = "Could not resend verification code.";
    public static final String ERROR_EMAIL_IS_ALREADY_VERIFIED = "Email is already verified.";
    public static final String ERROR_MOBILE_NUMBER_IS_ALREADY_VERIFIED = "Mobile number is already verified.";


    public static final String ERROR_PAGE_ALREADY_EXISTS_WITH_TAG = "Page already exists with tag.";
    public static final String ERROR_PAGE_NOT_FOUND = "Page not found.";
    public static final String ERROR_PAGE_ALREADY_HAS_SOME_SECTIONS = "Page already has some sections.";
    public static final String ERROR_SECTION_NOT_FOUND = "Section not found.";

    public static final String ERROR_FILE_UPLOAD_FAILED = "File upload failed.";
    public static final String ERROR_FILE_OBJECT_NOT_FOUND = "File object not found.";

    public static final String ERROR_FAILED_TO_SEND_EMAIL = "Failed to send email.";

    public static final String ERROR_RECAPTCHA_VALIDATION_FAILED = "Recaptcha validation failed.";


    public static final String ERROR_INVALID_DATE_RANGE = "Invalid date range.";

    public static final String ERROR_TITLE_IS_REQUIRED = "Blog title is required.";
    public static final String ERROR_CONTENT_IS_REQUIRED = "Blog content is required.";
    public static final String ERROR_CATEGORY_IS_REQUIRED = "Blog category is required.";
    public static final String ERROR_AUTHOR_USER_ID_IS_REQUIRED = "Author user id is required.";
    public static final String ERROR_BLOG_ID_IS_REQUIRED = "Blog id is required.";

    public static final String ERROR_CATEGORY_NOT_FOUND = "Category not found";
    public static final String ERROR_CATEGORY_ALREADY_EXISTS = "Category already exists";
    public static final String ERROR_AUTHOR_NOT_FOUND = "Author not found";
    public static final String ERROR_BLOG_NOT_FOUND = "Blog not found";
}
