package hanghae99.rescuepets.memberpet.dto;

import hanghae99.rescuepets.common.entity.NeuterEnum;
import hanghae99.rescuepets.common.entity.Post;
import hanghae99.rescuepets.common.entity.SexEnum;
import hanghae99.rescuepets.common.entity.UpkindEnum;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PostResponseDto {
    private Long id;
    private UpkindEnum upkind;
    private SexEnum sexCd;
    private NeuterEnum neuterYn;
    private String kindCd;
    private String age;
    private String weight;
    private String colorCd;
    private String happenPlace;
    private String happenLongitude;
    private String happenLatitude;
    private String happenDt;
    private String happenHour;
    private String specialMark;
    private String content;
    private String gratuity;
    private String contact;
    private String nickname;
    private String createdAt;
    private String modifiedAt;
    private Boolean openNickname;
    private Boolean isWished = false;
    private Integer wishedCount = 0;
    private List<PostImageResponseDto> postImages;
    private Boolean isLinked = false;

    public static PostResponseDto of(Post post) {
        return PostResponseDto.builder()
                .id(post.getId())
                .upkind(post.getUpkind())
                .sexCd(post.getSexCd())
                .neuterYn(post.getNeuterYn())
                .kindCd(post.getKindCd())
                .age(post.getAge())
                .weight(post.getWeight())
                .colorCd(post.getColorCd())
                .happenPlace(post.getHappenPlace())
                .happenLongitude(post.getHappenLongitude())
                .happenLatitude(post.getHappenLatitude())
                .happenDt(post.getHappenDt())
                .happenHour(post.getHappenHour())
                .specialMark(post.getSpecialMark())
                .content(post.getContent())
                .gratuity(post.getGratuity())
                .contact(post.getContact())
                .nickname(post.getMember().getNickname())
                .createdAt(post.getCreatedAt().toString())
                .modifiedAt(post.getModifiedAt().toString())
                .postImages(post.getPostImages().stream().map(PostImageResponseDto::of).toList())
                .build();
    }
    public void setWished(Boolean isWished) {
        this.isWished = isWished;
    }
    public void setWishedCount(Integer wishedCount) {this.wishedCount = wishedCount;}
    public void setLinked(Boolean isLinked) {
        this.isLinked = isLinked;
    }
}