package hanghae99.rescuepets.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessMessage {

    SIGN_UP_SUCCESS(HttpStatus.CREATED, "회원가입이 완료 되었습니다."),
    LOGIN_SUCCESS(HttpStatus.OK,"로그인이 완료 되었습니다."),
    ACOUNT_CHECK_SUCCESS(HttpStatus.OK,"사용 가능한 아이디입니다."),
    EMAIL_CHECK_SUCCESS(HttpStatus.OK,"사용 가능한 이메일입니다."),
    USER_INFO_SUCCESS(HttpStatus.OK, "유저정보 불러오기 성공"),
    PET_INFO_WISH_DELETE_SUCCESS(HttpStatus.OK, "관심 유기동물 등록이 완료되었습니다."),
    PET_INFO_GET_DETAILS_SUCCESS(HttpStatus.OK, "유기동물 상세 페이지 불러오기 성공"),
    PET_INFO_GET_LIST_SUCCESS(HttpStatus.OK, "유기동물 전체 페이지 불러오기 성공"),
    GOODS_DETAIL_SUCCESS(HttpStatus.OK, "상품 정보 불러오기 성공"),
    PET_INFO_WISH_SUCCESS(HttpStatus.CREATED, "관심 유기동물 등록이 완료되었습니다.");

    private final HttpStatus httpStatus;
    private final String detail;
}
