package com.stavya.janawaaz.dto;

import com.stavya.janawaaz.entity.ComplaintCategory;
import com.stavya.janawaaz.entity.ComplaintStatus;
import com.stavya.janawaaz.entity.LocationStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public final class ComplaintDtos {

    private ComplaintDtos() {
    }

    /** One ticket is created per selected category, so each can be tracked on its own. */
    public record CreateComplaintRequest(
            @NotNull Long volunteerId,
            @NotNull Integer wardId,
            @NotEmpty List<ComplaintCategory> categories,
            String description,
            String landmark,
            String residentName,
            String residentMobile,
            String photoUrl,
            Double latitude,
            Double longitude) {
    }

    public record UpdateStatusRequest(@NotNull ComplaintStatus status, String note) {
    }

    public record UpdateResponse(ComplaintStatus status, String note, Instant createdAt) {
    }

    public record ComplaintResponse(String ticketNo, Integer wardId, String localityEn, String localityHi,
                                    ComplaintCategory category, String description, String landmark,
                                    String residentName, String residentMobileMasked, String photoUrl,
                                    Double latitude, Double longitude, LocationStatus locationStatus,
                                    ComplaintStatus status, Instant createdAt, Instant resolvedAt,
                                    List<UpdateResponse> updates) {
    }

    /** Fields a volunteer or admin may correct after filing. Null leaves a field unchanged. */
    public record EditComplaintRequest(ComplaintCategory category, String description, String landmark) {
    }
}
