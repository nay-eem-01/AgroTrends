package com.project.agriculturalblogapplication.service;

import com.project.agriculturalblogapplication.entities.Tag;
import com.project.agriculturalblogapplication.repositories.TagRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TagServiceTest {

    private final TagRepository tagRepository = mock(TagRepository.class);
    private final TagService tagService = new TagService(tagRepository);

    @Test
    void namesAreTrimmedLowercasedAndSingleSpaced() {
        assertEquals("rice blast", TagService.normalize("  Rice   BLAST "));
        assertEquals("ধান", TagService.normalize(" ধান "));
    }

    @Test
    void existingTagsAreReusedNewOnesCreatedAndDuplicatesDropped() {
        Tag rice = tag("rice");
        when(tagRepository.findByName("rice")).thenReturn(Optional.of(rice));
        when(tagRepository.findByName("rice blast")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(call -> call.getArgument(0));

        Set<Tag> tags = tagService.resolve(List.of("Rice", "rice blast", "RICE", "  "));

        assertEquals(List.of("rice", "rice blast"), tags.stream().map(Tag::getName).toList());
        verify(tagRepository, times(1)).save(any(Tag.class));
    }

    private static Tag tag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        return tag;
    }
}
