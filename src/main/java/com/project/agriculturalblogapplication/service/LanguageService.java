package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Language;
import com.project.agriculturalblogapplication.exceptionHandler.ApplicationException;
import com.project.agriculturalblogapplication.model.request.CreateUpdateLanguageRequest;
import com.project.agriculturalblogapplication.payloads.PaginationArgs;
import com.project.agriculturalblogapplication.repositories.LanguageRepository;
import com.project.agriculturalblogapplication.util.CommonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LanguageService {

    private final LanguageRepository languageRepository;

    public Language save(Language language) {
        return languageRepository.save(language);
    }

    public Language createUpdate(CreateUpdateLanguageRequest request, Language language) {
        language.setCode(request.getCode());
        language.setName(request.getName());
        language.setActive(request.isActive());
        return languageRepository.save(language);
    }

    public List<Language> getAllLanguages() {
        return languageRepository.findAll();
    }

    public Language findById(long id) {
        return languageRepository.findById(id).orElse(null);
    }

   public Boolean existsByCode(String code){
        return languageRepository.existsByCode(code);
   }

    public boolean existsByCodeAndActive(String code, boolean active) {
        return languageRepository.existsByCodeAndActive(code, active);
    }

    public Language findByIdAndActive(long id) {
        return languageRepository.findTopByIdAndActive(id, true);
    }

    public Language findTopByCode(String code) {
        return languageRepository.findTopByCode(code).orElse(null);
    }

    public Language findTopByCodeWithException(String code) {
        return languageRepository.findTopByCode(code).orElseThrow(()->
                new ApplicationException(HttpStatus.NOT_FOUND, "Language not found with code: " + code + "!"));
    }

    public Language findByCodeAndName(String code, String name) {
        return languageRepository.findTopByCodeAndName(code, name);
    }

    public Language findByCodeAndNameAndIdNot(String code, String name, long id) {
        return languageRepository.findTopByCodeAndNameAndIdNot(code, name, id);
    }

    public Language findTopByCodeAndIdNot(String code,long id){

        return languageRepository.findTopByCodeAndIdNot(code,id);

    }
    public Page<Language> findAllPaginated(PaginationArgs paginationArgs) {
        Pageable pageable = CommonUtils.getPageable(paginationArgs);
        return languageRepository.findAll(pageable);
    }

    public void delete(Language language) {
        //TODO - delete all other things which is dependent into language
        languageRepository.delete(language);
    }
}
