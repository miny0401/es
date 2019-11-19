package com.miny.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
import java.util.Objects;

@Getter
@Setter
@ToString(callSuper = true)
public class SearchWordRequest extends BaseRequest {

    /**
     * 搜索词来源；1-用户搜索；2-热词推荐
     */
    @NotNull
    @Range(min = 1, max = 2)
    private Integer wordSource;

    /**
     * 搜索词，用户手动输入或点击搜索的推荐热词
     */
    @NotNull
    private String searchWord;

    @NotNull
    private Integer pageNum;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        SearchWordRequest that = (SearchWordRequest) o;
        return Objects.equals(wordSource, that.wordSource) &&
                Objects.equals(searchWord, that.searchWord);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), wordSource, searchWord);
    }
}
