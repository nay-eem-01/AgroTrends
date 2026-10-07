package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Tag;
import com.project.agriculturalblogapplication.repositories.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private static final int SUGGESTION_LIMIT = 20;

    private final TagRepository tagRepository;

    /** "  Rice   Blast " and "rice blast" are the same tag. */
    public static String normalize(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /** Existing tags are reused and new names are created; duplicates and blanks are dropped. */
    public Set<Tag> resolve(Collection<String> names) {
        Set<String> normalized = names.stream()
                .map(TagService::normalize)
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Tag> tags = new LinkedHashSet<>();
        for (String name : normalized) {
            tags.add(tagRepository.findByName(name).orElseGet(() -> tagRepository.save(newTag(name))));
        }
        return tags;
    }

    /** Tag names starting with {@code prefix}, alphabetically, for autocomplete. */
    public List<String> suggest(String prefix) {
        return tagRepository.findByNameStartingWith(normalize(prefix),
                        PageRequest.of(0, SUGGESTION_LIMIT, Sort.by("name"))).stream()
                .map(Tag::getName)
                .toList();
    }

    private static Tag newTag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        return tag;
    }
}
