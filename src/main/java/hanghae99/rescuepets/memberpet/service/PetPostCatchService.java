package hanghae99.rescuepets.memberpet.service;


import hanghae99.rescuepets.common.dto.CustomException;
import hanghae99.rescuepets.common.dto.ResponseDto;
import hanghae99.rescuepets.common.entity.Member;
import hanghae99.rescuepets.common.entity.PetPostCatch;
import hanghae99.rescuepets.common.entity.PostImage;
import hanghae99.rescuepets.common.s3.S3Uploader;
import hanghae99.rescuepets.memberpet.dto.PetPostCatchRequestDto;
import hanghae99.rescuepets.memberpet.dto.PetPostCatchResponseDto;
import hanghae99.rescuepets.memberpet.repository.PetPostCatchRepository;
import hanghae99.rescuepets.wish.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static hanghae99.rescuepets.common.dto.ExceptionMessage.*;
import static hanghae99.rescuepets.common.dto.SuccessMessage.*;

@Service
@RequiredArgsConstructor
public class PetPostCatchService {
    private final PetPostCatchRepository petPostCatchRepository;
    private final WishRepository wishRepository;
    private final S3Uploader s3Uploader;

    @Transactional
    public ResponseEntity<ResponseDto> getPetPostCatchList(int page, int size, String sortBy, Member member) {
        Sort sort = Sort.by(Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PetPostCatch> PetPostCatchPage = petPostCatchRepository.findAll(pageable);

        List<PetPostCatch> PetPostCatches = PetPostCatchPage.getContent();
        List<PetPostCatchResponseDto> dtoList = new ArrayList<>();

        for (PetPostCatch petPostCatch : PetPostCatches) {
            PetPostCatchResponseDto dto = PetPostCatchResponseDto.of(petPostCatch);
            dto.setWished(wishRepository.findWishByPetPostCatchIdAndMemberId(petPostCatch.getId(), member.getId()).isPresent());
            dtoList.add(dto);
        }
        return ResponseDto.toResponseEntity(POST_LIST_READING_SUCCESS, dtoList);
    }
    @Transactional
    public ResponseEntity<ResponseDto> getPetPostCatchListByMember(int page, int size, String sortBy, Member member) {

        Sort sort = Sort.by(Sort.Direction.DESC, sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<PetPostCatch> PetPostCatchPage = petPostCatchRepository.findByMemberId(member.getId(), pageable);
        List<PetPostCatch> PetPostCatches = PetPostCatchPage.getContent();
        List<PetPostCatchResponseDto> dtoList = new ArrayList<>();

        for (PetPostCatch petPostCatch : PetPostCatches) {
            PetPostCatchResponseDto dto = PetPostCatchResponseDto.of(petPostCatch);
            dto.setWished(wishRepository.findWishByPetPostCatchIdAndMemberId(petPostCatch.getId(), member.getId()).isPresent());
            dtoList.add(dto);
        }
        return ResponseDto.toResponseEntity(MY_POST_READING_SUCCESS, dtoList);
    }

    @Transactional
    public ResponseEntity<ResponseDto> getPetPostCatch(Long petPostCatchId, Member member) {
        PetPostCatch petPostCatch = petPostCatchRepository.findById(petPostCatchId).orElseThrow(() -> new CustomException(POST_NOT_FOUND));
        PetPostCatchResponseDto responseDto = PetPostCatchResponseDto.of(petPostCatch);
        responseDto.setWished(wishRepository.findWishByPetPostCatchIdAndMemberId(petPostCatchId, member.getId()).isPresent());
        return ResponseDto.toResponseEntity(POST_READING_SUCCESS, responseDto);
    }

    @Transactional
    public ResponseEntity<ResponseDto> create(PetPostCatchRequestDto requestDto, Member member) {
        List<String> postImageURLs = new ArrayList<>();
        if (requestDto.getPostImages() != null && !requestDto.getPostImages().isEmpty()) {
            postImageURLs = s3Uploader.uploadMulti(requestDto.getPostImages());
        }
        PetPostCatch petPostCatch = new PetPostCatch(requestDto, member);
        for (String postImageURL : postImageURLs) {
            petPostCatch.addPostImage(new PostImage(petPostCatch, postImageURL));
        }
        petPostCatchRepository.save(petPostCatch);
        return ResponseDto.toResponseEntity(POST_WRITING_SUCCESS);
    }


    @Transactional
    public ResponseEntity<ResponseDto> update(Long petPostCatchId, PetPostCatchRequestDto requestDto, Member member) {
        PetPostCatch petPostCatch = petPostCatchRepository.findById(petPostCatchId).orElseThrow(() -> new CustomException(POST_NOT_FOUND));
        if (member.getNickname().equals(petPostCatch.getMember().getNickname())) {
            List<String> postImageURLs = s3Uploader.uploadMulti(requestDto.getPostImages());
            petPostCatch.getPostImages().clear();
            for (String postImageURL : postImageURLs) {
                petPostCatch.addPostImage(new PostImage(petPostCatch, postImageURL));
            }
            petPostCatch.update(requestDto);
            return ResponseDto.toResponseEntity(POST_MODIFYING_SUCCESS);
        } else {
            throw new CustomException(UNAUTHORIZED_UPDATE_OR_DELETE);
        }
    }

    @Transactional
    public ResponseEntity<ResponseDto> delete(Long petPostCatchId, Member member) {
        PetPostCatch petPostCatch = petPostCatchRepository.findById(petPostCatchId).orElseThrow(() -> new CustomException(POST_NOT_FOUND)
        );
        if (member.getNickname().equals(petPostCatch.getMember().getNickname())) {
            petPostCatchRepository.deleteById(petPostCatchId);
            return ResponseDto.toResponseEntity(POST_DELETE_SUCCESS);
        } else {
            throw new CustomException(UNAUTHORIZED_UPDATE_OR_DELETE);
        }
    }

}
