package edu.au.life.shortenit.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private Long totalLinks;
    private Long activeLinks;
    private Long totalClicks;
    private Double avgClicksPerLink;
    private String topRegion;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long totalUsers;
}
