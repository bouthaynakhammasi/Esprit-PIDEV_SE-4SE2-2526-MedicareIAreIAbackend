package com.aziz.demosec.service;

import com.aziz.demosec.dto.PostRequest;
import com.aziz.demosec.dto.PostResponse;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import java.util.List;

public interface PostService {
    PostResponse create(@Valid PostRequest request, MultipartFile image);
    PostResponse getById(Long id);
    List<PostResponse> getAll();
    List<PostResponse> getByCategory(String category);
    List<PostResponse> search(String keyword);
    List<PostResponse> getByAuthorId(Long authorId);
    PostResponse update(Long id, @Valid PostRequest request, MultipartFile image);
    void delete(Long id);
}