package edu.csuf.sis.records;

import java.math.BigDecimal;
import java.util.List;

public record TranscriptTerm(String termName, List<TranscriptLine> courses, BigDecimal termGpa) {
}
