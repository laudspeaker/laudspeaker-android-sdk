package com.laudspeaker.android;

import java.util.Map;

public interface LaudspeakerPropertiesSanitizer {
    Map<String, Object> sanitize(Map<String, Object> properties);
}
