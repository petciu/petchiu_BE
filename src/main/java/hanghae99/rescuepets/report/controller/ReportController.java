package hanghae99.rescuepets.report.controller;

import hanghae99.rescuepets.common.dto.ResponseDto;
import hanghae99.rescuepets.common.entity.ReportEnum;
import hanghae99.rescuepets.common.security.MemberDetails;
import hanghae99.rescuepets.report.dto.ReportMemberRequestDto;
import hanghae99.rescuepets.report.dto.ReportRequestDto;
import hanghae99.rescuepets.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.beans.PropertyEditorSupport;


@RequestMapping("/api/report")
@RequiredArgsConstructor
@RestController
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/all")
    @Operation(summary = "Report 전체 게시글을 페이징없이 불러오기", description = "Report를 페이징없이 불러옵니다")
    public ResponseEntity<ResponseDto> getReportAll(@RequestParam(required = false, defaultValue = "createdAt") String sortBy, @Parameter(hidden = true) @AuthenticationPrincipal MemberDetails memberDetails) {

        return reportService.getReportAll(sortBy, memberDetails.getMember());
    }

    @PostMapping(value = "/post")
    public ResponseEntity<ResponseDto> reportPost(@RequestBody ReportRequestDto reportRequestDto, @Parameter(hidden = true) @AuthenticationPrincipal MemberDetails memberDetails){
        return reportService.reportPost(reportRequestDto, memberDetails.getMember());
    }



    @DeleteMapping(value = "/post/{reportId}")
    public ResponseEntity<ResponseDto> reportPostDelete(@PathVariable Long reportId, @Parameter(hidden = true) @AuthenticationPrincipal MemberDetails memberDetails){
        return reportService.reportPostDelete(reportId,memberDetails.getMember());
    }


    @PostMapping(value = "/comment")
    public ResponseEntity<ResponseDto> reportComment(@RequestBody ReportRequestDto reportRequestDto, @Parameter(hidden = true) @AuthenticationPrincipal MemberDetails memberDetails){
        return  reportService.reportComment(reportRequestDto,memberDetails.getMember());
    }



    @DeleteMapping(value = "/comment/{reportId}")
    public ResponseEntity<ResponseDto> reportCommentDelete(@PathVariable Long reportId, @Parameter(hidden = true)@AuthenticationPrincipal MemberDetails memberDetails){
        return reportService.reportCommentDelete(reportId, memberDetails.getMember());
    }

    @PostMapping(value = "/member")
    public ResponseEntity<ResponseDto> reportMember(@RequestBody ReportMemberRequestDto reportMemberRequestDto,  @Parameter(hidden = true) @AuthenticationPrincipal MemberDetails memberDetails){
        return reportService.reportMember(reportMemberRequestDto,memberDetails.getMember());
    }

    @DeleteMapping(value = "/member/{reportId}")
    public ResponseEntity<ResponseDto> reportMemberDelete(@PathVariable Long reportId, @Parameter(hidden = true) @AuthenticationPrincipal MemberDetails memberDetails){
        return reportService.reportMemberDelete(reportId, memberDetails.getMember());
    }

    // value 값과 key 값 바꾸는 로직
    public class TempEnumConverter extends PropertyEditorSupport {
        public void setAsText(final String text) throws IllegalArgumentException {
            setValue(ReportEnum.fromValue(text));
        }
    }
    // value 값과 key 값 바꾸는 로직
    @InitBinder
    public void initBinder(final WebDataBinder webdataBinder) {
        webdataBinder.registerCustomEditor(ReportEnum.class, new TempEnumConverter());
    }

}




