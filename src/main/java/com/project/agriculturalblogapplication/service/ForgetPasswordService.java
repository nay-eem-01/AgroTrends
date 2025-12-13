//package com.project.agriculturalblogapplication.service;
//
//import com.project.agriculturalblogapplication.Config.AppProperties;
//import com.project.agriculturalblogapplication.model.response.SendVerificationCodeResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class ForgetPasswordService {
//
//    private final OTPService otpService;
//    private final UserService userService;
//    private final EmailTemplateService emailTemplateService;
//    private final MailService mailService;
//    private final BrevoSMSService brevoSmsService;
//    private final AppProperties appProperties;
//    private final BrevoProperties brevoProperties;
//
//    public SendVerificationCodeResponse sendOtpToInitiateForgetPasswordRequest(User user, InitiateForgetPasswordRequest request, String lang) {
//        SendOTPRequest sendOtpRequest = SendOTPRequest.builder()
//                .userId(user.getId())
//                .otpChannel(request.getOtpChannel())
//                .email(user.getEmail())
//                .mobileNumber(user.getMobileNumber())
//                .otpPurpose(OtpPurpose.FORGET_PASSWORD)
//                .build();
//        OTP otp = otpService.sendOTP(sendOtpRequest, lang);
//
//        UserDetails userDetails = user.getUserDetails();
//        String greetings = "Dear Customer";
//        if (userDetails != null) {
//            greetings = "Hello " + userDetails.getFirstName();
//        }
//
//        if (otp.getOtpChannel().equals(OTPChannel.EMAIL)) {
//            EmailTemplate emailTemplate = emailTemplateService.findByEmailKeyWithException(EmailKey.RESET_PASSWORD.name(), lang);
//            String htmlTemplate = emailTemplate.getHtmlTemplate();
//            Map<String, String> replacementMap = new HashMap<>();
//            replacementMap.put(EmailTemplatePlaceholder.LOGO.getName(), appProperties.getLogoUrl());
//            replacementMap.put(EmailTemplatePlaceholder.GREETINGS.getName(), greetings);
//            replacementMap.put(EmailTemplatePlaceholder.OTP_CODE.getName(), otp.getOtpCode());
//            htmlTemplate = CommonUtils.getEmailText(htmlTemplate, replacementMap);
//            mailService.sendMail(otp.getEmail(), "Reset Password", htmlTemplate);
//        }
//
//        if (otp.getOtpChannel().equals(OTPChannel.SMS)) {
//            BrevoSendSMSRequest brevoSendSMSRequest = BrevoSendSMSRequest.builder()
//                    .sender(brevoProperties.getSmsSender() != null ? brevoProperties.getSmsSender() : "DHRUBOK")
//                    .recipient(user.getMobileNumber())
//                    .content("We have received a request to reset your password. Your reset password verification code is " + otp.getOtpCode() + ". This code will be expired in 2 minutes.")
//                    .type("transactional")
//                    .build();
//
//            brevoSmsService.sendSms(brevoSendSMSRequest, lang);
//        }
//
//        return SendVerificationCodeResponse.builder()
//                .otpToken(otp.getOtpToken())
//                .otpChannel(otp.getOtpChannel())
//                .email(otp.getEmail())
//                .mobileNumber(CommonUtils.getMaskedMobileNumber(otp.getMobileNumber()))
//                .build();
//    }
//
//    public VerifyOTPResponse verifyForgetPasswordRequest(User user, VerifyForgetPasswordRequest request, String lang) {
//        VerifyOTPRequest verifyOtpRequest = VerifyOTPRequest.builder()
//                .userId(user.getId())
//                .otpToken(request.getOtpToken())
//                .otpCode(request.getOtpCode())
//                .build();
//        OTP otp = otpService.verifyOTP(verifyOtpRequest, lang);
//
//        return VerifyOTPResponse.builder()
//                .isVerified(true)
//                .otpToken(otp.getOtpToken())
//                .build();
//    }
//
//    public void resetPassword(User user, ResetPasswordRequest request, String lang) {
//        OTP otp = otpService.findByUserIdAndOtpToken(user.getId(), request.getOtpToken());
//        if (otp == null) {
//            throw new ApplicationException(HttpStatus.BAD_REQUEST, "ERROR_INVALID_RESET_PASSWORD_REQUEST", lang);
//        }
//
//        String newPassword = request.getNewPassword();
//        String invalidPasswordMessage = CommonUtils.getInvalidPasswordMessage(newPassword);
//        if (invalidPasswordMessage != null) {
//            throw new ApplicationException(HttpStatus.BAD_REQUEST, invalidPasswordMessage, lang);
//        }
//
//        userService.updatePassword(user.getId(), newPassword, lang);
//    }
//
//}
