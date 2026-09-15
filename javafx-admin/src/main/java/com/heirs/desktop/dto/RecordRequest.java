package com.heirs.desktop.dto;

import java.time.LocalDate;
import com.heirs.desktop.model.Record;

/** Exact shared shape of CreateRecordRequestDto and UpdateRecordRequestDto. */
public record RecordRequest(String title, String description, String category, String department,
        String referenceNumber, Integer publicationYear, LocalDate publishedDate,
        String status, String source, String keywords) {
    public static RecordRequest from(Record r) {
        return new RecordRequest(r.getTitle(), r.getDescription(), r.getCategory(), r.getDepartment(),
                r.getReference(), r.getYear() == null || r.getYear() == 0 ? null : r.getYear(),
                r.getPublishedDate(), r.getStatus(), r.getSource(), r.getKeywords());
    }
}
