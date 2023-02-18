package ca.waaw.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private String id;

    private String title;

    private String description;

    private boolean isRead;

    private String type;

    private String createdTime;

}