package hanghae99.rescuepets.comment.controller;

import hanghae99.rescuepets.comment.dto.CommentResponseDto;
import hanghae99.rescuepets.comment.dto.CommentRequestDto;
import hanghae99.rescuepets.comment.service.CommentService;
import hanghae99.rescuepets.common.dto.ResponseDto;
import hanghae99.rescuepets.common.security.MemberDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api")
@RestController
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;

//    @GetMapping("/pets/missing/members/{memberId}")
//    public ResponseDto<List<CommentResponseDto>> getCommentByUser(@AuthenticationPrincipal MemberDetails userDetails){
//        return commentService.getCommentList(userDetails.getMember());
//    }

    @GetMapping("/pets/missing/{petPostMissingId}/comments")
    public ResponseDto<List<CommentResponseDto>> getCommentByPost(@PathVariable Long petPostMissingId) {
        return commentService.getCommentCurrentList(petPostMissingId);
    }

    @PostMapping("/pets/missing/{petPostMissingId}/comments")
    public ResponseDto<String> createComment(@PathVariable Long petPostMissingId, @RequestBody CommentRequestDto requestDto, @AuthenticationPrincipal MemberDetails userDetails) {
        return commentService.create(petPostMissingId, requestDto, userDetails.getMember());
    }
//
//    @PutMapping("/pets/missing/{petPostMissingId}/comments/{commentId}")
//    public ResponseDto<String> updateComment(@PathVariable Long petPostMissingId, @RequestBody CommentRequestDto requestDto, @AuthenticationPrincipal MemberDetails userDetails) {
//        return commentService.update(petPostMissingId, requestDto, userDetails.getMember());
//    }
//
//    @DeleteMapping("/pets/missing/{petPostMissingId}/comments/{commentId}")
//    public ResponseDto<String> deleteComment(@PathVariable Long petPostMissingId, @AuthenticationPrincipal MemberDetails userDetails) {
//        return commentService.delete(petPostMissingId, userDetails.getMember());
//    }
}
