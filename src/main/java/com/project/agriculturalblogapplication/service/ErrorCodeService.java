package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.constatnt.ErrorCode;
import com.project.agriculturalblogapplication.entities.ErrorCodeEntity;
import com.project.agriculturalblogapplication.entities.Language;
import com.project.agriculturalblogapplication.entities.LocalizedText;
import com.project.agriculturalblogapplication.entities.Translation;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.ErrorCodeRequest;
import com.project.agriculturalblogapplication.model.response.ErrorCodeResponse;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.repositories.ErrorCodeRepository;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ErrorCodeService {

    private final ErrorCodeRepository errorCodeRepository;

    private final LanguageService languageService;

    private final LocalizedTextService localizedTextService;

    private TranslationService translationService;

    public Page<ErrorCodeResponse> getAll(PaginationArgs paginationArgs, String lang) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs);
        Page<ErrorCodeEntity> errorCodePage;
        errorCodePage = errorCodeRepository.findAll(pageable);

        List<ErrorCodeResponse> errorCodeResponses = errorCodePage.getContent().stream().map((errorCodeEntity)-> entityToResponse(errorCodeEntity, lang)).toList();
        return new PageImpl<>(errorCodeResponses, pageable, errorCodePage.getTotalElements());
    }

    public ErrorCodeResponse findByIdWithException(Long id, String lang) {
        ErrorCodeEntity errorCodeEntity = errorCodeRepository.findById(id).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_ERROR_CODE_NOT_FOUND, lang));
        return entityToResponse(errorCodeEntity, lang);
    }

    public ErrorCodeResponse findByInternalCode(String internalCode, String lang) {
        ErrorCodeEntity errorCodeEntity = errorCodeRepository.findTopByInternalCode(internalCode).orElse(null);
        return entityToResponse(errorCodeEntity, lang);
    }

    public ErrorCodeResponse saveErrorCode(ErrorCodeEntity errorCodeEntity, String lang) {
        ErrorCodeEntity savedErrorCode = errorCodeRepository.save(errorCodeEntity);
        return entityToResponse(savedErrorCode, lang);
    }

    public ErrorCodeResponse createNewErrorCode(ErrorCodeRequest request) {
        if (errorCodeRepository.existsByInternalCode(request.getInternalCode())) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_ERROR_CODE_ALREADY_EXISTS, request.getLang());
        }

        LocalizedText localizedMessage = localizedTextService.createLocalizedTextWithTranslation(request.getMessage(), request.getLang());

        ErrorCodeEntity errorCodeEntity = new ErrorCodeEntity();
        errorCodeEntity.setInternalCode(request.getInternalCode());
        errorCodeEntity.setInternalMessage(request.getInternalMessage());
        errorCodeEntity.setMessage(localizedMessage);
        return saveErrorCode(errorCodeEntity, request.getLang());

    }

    public ErrorCodeResponse updateErrorCode(ErrorCodeRequest request) {
        ErrorCodeEntity errorCodeEntity = errorCodeRepository.findById(request.getId()).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_ERROR_CODE_NOT_FOUND, request.getLang()));

        if (errorCodeRepository.existsByInternalCodeAndIdNot(request.getInternalCode(), errorCodeEntity.getId())) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, ErrorCode.ERROR_ERROR_CODE_ALREADY_EXISTS, request.getLang());
        }

        LocalizedText localizedMessage = localizedTextService.updateLocalizedTextWithTranslation(errorCodeEntity.getMessage(), request.getMessage(), request.getLang());

        errorCodeEntity.setInternalCode(request.getInternalCode());
        errorCodeEntity.setInternalMessage(request.getInternalMessage());
        errorCodeEntity.setMessage(localizedMessage);
        return saveErrorCode(errorCodeEntity, request.getLang());

    }

    public void deleteErrorCode(Long id, String lang) {
        ErrorCodeEntity errorCodeEntity = errorCodeRepository.findById(id).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND, ErrorCode.ERROR_ERROR_CODE_NOT_FOUND, lang));
        errorCodeRepository.delete(errorCodeEntity);
    }

    public ErrorCodeResponse entityToResponse(ErrorCodeEntity errorCodeEntity, String lang) {
        if (errorCodeEntity == null) {
            return null;
        }

        String message = errorCodeEntity.getMessage().getOriginalText();

        if (lang != null) {
            Language language = languageService.findTopByCode(lang);
            if (language != null) {
                Translation translatedMessage = translationService.findByLocalizedTextAndLanguageCode(
                        errorCodeEntity.getMessage(), lang);

                if (translatedMessage != null) {
                    message = translatedMessage.getTranslatedText();
                }
            }
        }

        ErrorCodeResponse response = new ErrorCodeResponse();
        response.setId(errorCodeEntity.getId());
        response.setInternalCode(errorCodeEntity.getInternalCode());
        response.setInternalMessage(errorCodeEntity.getInternalMessage());
        response.setMessage(message);
        response.setCreatedBy(errorCodeEntity.getCreatedBy());
        response.setCreationDate(errorCodeEntity.getCreationDate());
        response.setLastModifiedBy(errorCodeEntity.getLastModifiedBy());
        response.setLastModifiedDate(errorCodeEntity.getLastModifiedDate());
        response.setCreationDateTimeStamp(errorCodeEntity.getCreationDateTimeStamp());
        response.setLastModifiedDateTimeStamp(errorCodeEntity.getLastModifiedDateTimeStamp());
        return response;
    }

}
