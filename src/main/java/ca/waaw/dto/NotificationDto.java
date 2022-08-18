package ca.waaw.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private String id;

    private String title;

    private String description;

    private boolean isRead;

    private String type;

    private Instant createdTime;

}