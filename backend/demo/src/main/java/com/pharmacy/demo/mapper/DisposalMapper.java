package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.DisposalDetailResponse;
import com.pharmacy.demo.dto.response.DisposalSummaryResponse;
import com.pharmacy.demo.entity.Disposal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface DisposalMapper {

    @Mapping(target = "lineCount", expression = "java(disposal.getLines() == null ? 0 : disposal.getLines().size())")
    @Mapping(target = "totalQuantity", expression = "java(totalQuantityOf(disposal))")
    DisposalSummaryResponse toSummary(Disposal disposal);

    DisposalDetailResponse toDetail(Disposal disposal);

    default int totalQuantityOf(Disposal disposal) {
        if (disposal.getLines() == null) {
            return 0;
        }
        return disposal.getLines().stream()
                .mapToInt(line -> line.getQuantity() == null ? 0 : line.getQuantity())
                .sum();
    }
}
