package base;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@RequiredArgsConstructor
public class WebhookRequest {
    private final String body;
    private final Map<String, List<String>> headers;
}
