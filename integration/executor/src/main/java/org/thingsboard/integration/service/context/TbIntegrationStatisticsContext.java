package org.thingsboard.integration.service.context;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.integration.api.IntegrationStatisticsService;

@Component
@Data
@RequiredArgsConstructor
public class TbIntegrationStatisticsContext  implements TbIntegrationStatisticsContextComponent{
    private final IntegrationStatisticsService integrationStatisticsService;
}
