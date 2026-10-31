package com.pharmacy.demo.mapper;

import com.pharmacy.demo.dto.response.StocktakeDetailResponse;
import com.pharmacy.demo.dto.response.StocktakeSummaryResponse;
import com.pharmacy.demo.entity.Stocktake;
import com.pharmacy.demo.entity.embedded.StocktakeLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(imports = StocktakeLine.class)
public interface StocktakeMapper {

    @Mapping(target = "scope", expression = "java(scopeOf(stocktake))")
    @Mapping(target = "lineCount", expression = "java(stocktake.getLines() == null ? 0 : stocktake.getLines().size())")
    @Mapping(target = "differenceCount", expression = "java(differenceCountOf(stocktake))")
    StocktakeSummaryResponse toSummary(Stocktake stocktake);

    @Mapping(target = "scope", expression = "java(scopeOf(stocktake))")
    StocktakeDetailResponse toDetail(Stocktake stocktake);

    default String scopeOf(Stocktake stocktake) {
        return stocktake.getCategoryId() == null ? "ALL" : stocktake.getCategoryId();
    }

    default int differenceCountOf(Stocktake stocktake) {
        if (stocktake.getLines() == null) {
            return 0;
        }
        return (int) stocktake.getLines().stream()
                .filter(line ->
                        line.getActualQty() != null && line.getDifference() != null && line.getDifference() != 0)
                .count();
    }
}
