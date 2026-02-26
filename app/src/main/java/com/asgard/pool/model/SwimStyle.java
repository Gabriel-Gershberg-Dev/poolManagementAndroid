package com.asgard.pool.model;

import com.google.gson.annotations.SerializedName;

public enum SwimStyle {
    @SerializedName("FREESTYLE") CRAWL,  // backward compat with saved history; חתירה (front crawl)
    BREASTSTROKE,// חזה (breaststroke)
    BUTTERFLY,   // פרפר (butterfly)
    BACKSTROKE   // גב (backstroke)
}
