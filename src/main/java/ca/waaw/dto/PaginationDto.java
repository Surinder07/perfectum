package ca.waaw.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationDto {

    private int totalPages = 0;

    private int totalEntries = 0;

    private List<?> data = new ArrayList<>();

}
